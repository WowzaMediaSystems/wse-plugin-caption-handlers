/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.util.FLVUtils;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.publish.Publisher;
import com.wowza.wms.vhost.IVHost;

import java.util.*;
import java.util.concurrent.*;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.*;

public class DelayedStream
{
    private static final Class<DelayedStream> CLASS = DelayedStream.class;
    private static final String CLASS_NAME = CLASS.getSimpleName();
    public static final long DEFAULT_START_DELAY = 30000;
    private final IApplicationInstance appInstance;
    private final WMSLogger logger;
    private final String streamName;
    private final ScheduledExecutorService executor;
    private final long startTime;
    private final long startDelay;
    private boolean debugLog = false;
    private long startOffset = -1;

    private Publisher publisher;
    private boolean doSendOnMetaData = true;
    private boolean isFirstAudio = true;
    private boolean isFirstVideo = true;
    private final Queue<AMFPacket> packets = new PriorityBlockingQueue<>(2400, new AMFPacketComparator());
    private volatile boolean doShutdown = false;

    public DelayedStream(IApplicationInstance appInstance, String streamName, ScheduledExecutorService executor)
    {
        this.appInstance = appInstance;
        this.logger = WMSLoggerFactory.getLoggerObj(appInstance);
        this.debugLog = appInstance.getProperties().getPropertyBoolean(PROP_DELAYED_STREAM_DEBUG_LOG, debugLog);
        this.streamName = streamName;
        this.executor = executor;
        startTime = System.currentTimeMillis();
        startDelay = appInstance.getProperties().getPropertyLong(PROP_CAPTIONS_STREAM_DELAY, DEFAULT_START_DELAY);
        executor.scheduleAtFixedRate(() -> processPackets(), 0, 75, TimeUnit.MILLISECONDS);
    }

    public long getStartOffset()
    {
        return startOffset;
    }

    public void writePacket(AMFPacket packet)
    {
        if(doShutdown)
            return;
        packets.add(packet);
        if (debugLog)
            logger.info(MODULE_NAME + "::" + CLASS_NAME + ".writePacket() [" + appInstance.getContextStr() + "/" + streamName + "] packet: " + packet);
        if (startOffset == -1)
            startOffset = packet.getAbsTimecode();
    }

    private void processPackets()
    {
        try
        {
            if(doShutdown && packets.isEmpty())
            {
                executor.shutdown();
                shutdownPublisher();
            }

            long now = System.currentTimeMillis();
            if(now - startDelay < startTime)
                return;
            if(packets.isEmpty())
            {
                return;
            }
            if (publisher == null)
            {
                publisher = Publisher.createInstance(appInstance);
                publisher.setStreamType(appInstance.getStreamType());
                publisher.publish(streamName + DELAYED_STREAM_SUFFIX);
            }
            while (true)
            {
                AMFPacket packet = packets.peek();
                if (packet == null)
                    return;
                long timecode = packet.getAbsTimecode();
                if (startTime - startOffset + timecode > now - startDelay)
                    break;

                if (debugLog)
                    logger.info(MODULE_NAME + "::" + CLASS_NAME + ".processPackets() [" + appInstance.getContextStr() + "/" + streamName + "] packet: " + packet);

                if (doSendOnMetaData)
                {
                    while (true)
                    {
                        IMediaStream videoSourceStream = appInstance.getStreams().getStream(streamName);
                        if (videoSourceStream == null)
                            break;
                        IMediaStreamMetaDataProvider metaDataProvider = videoSourceStream.getMetaDataProvider();
                        if (metaDataProvider == null)
                            break;

                        List<AMFPacket> metaData = new ArrayList<>();

                        metaDataProvider.onStreamStart(metaData, timecode);

                        Iterator<AMFPacket> miter = metaData.iterator();
                        while (miter.hasNext())
                        {
                            AMFPacket metaPacket = miter.next();
                            if (metaPacket == null)
                                continue;

                            if (metaPacket.getSize() <= 0)
                                continue;

                            byte[] metaDataData = metaPacket.getData();
                            if (metaDataData == null)
                                continue;

                            if (debugLog)
                                logger.info(MODULE_NAME + "::" + CLASS_NAME + ".writePacket live[onMetadata]: dat:" + timecode);
                            publisher.addDataData(metaDataData, metaDataData.length, timecode);
                        }
                        break;
                    }
                    doSendOnMetaData = false;
                }

                switch (packet.getType())
                {
                    case IVHost.CONTENTTYPE_AUDIO:
                        if (debugLog)
                            logger.info(MODULE_NAME + "::" + CLASS_NAME + ".writePacket live: aud:" + timecode + ":" + packet.getSeq());
                        if (isFirstAudio)
                        {
                            IMediaStream audioSourceStream = appInstance.getStreams().getStream(streamName);
                            if (audioSourceStream == null)
                                break;
                            AMFPacket configPacket = audioSourceStream.getAudioCodecConfigPacket(packet.getAbsTimecode());
                            if (configPacket != null)
                                publisher.addAudioData(configPacket.getData(), configPacket.getSize(), timecode);
                            isFirstAudio = false;
                        }
                        publisher.addAudioData(packet.getData(), packet.getSize(), timecode);
                        break;
                    case IVHost.CONTENTTYPE_VIDEO:
                        if (debugLog)
                            logger.info(MODULE_NAME + "::" + CLASS_NAME + ".writePacket live: vi" + (FLVUtils.isVideoKeyFrame(packet) ? "k" : "p") + ":" + timecode + ":" + packet.getSeq());
                        if (isFirstVideo)
                        {
                            IMediaStream videoSourceStream = appInstance.getStreams().getStream(streamName);
                            if (videoSourceStream == null)
                                break;
                            AMFPacket configPacket = videoSourceStream.getVideoCodecConfigPacket(packet.getAbsTimecode());
                            if (configPacket != null)
                                publisher.addVideoData(configPacket.getData(), configPacket.getSize(), timecode);
                            isFirstVideo = false;
                        }
                        publisher.addVideoData(packet.getData(), packet.getSize(), timecode);
                        break;
                    case IVHost.CONTENTTYPE_DATA0:
                    case IVHost.CONTENTTYPE_DATA3:
                        if (debugLog)
                            logger.info(MODULE_NAME + "::" + CLASS_NAME + ".writePacket live: dat:" + timecode + ":" + packet.getSeq());
                        publisher.addDataData(packet.getData(), packet.getSize(), timecode);
                        break;
                }
                packets.remove(packet);
            }
        }
        catch (Exception e)
        {
            logger.error(MODULE_NAME + "::" + CLASS_NAME + ".writePacket[metadata] ", e);
        }
    }

    public void shutdown()
    {
        doShutdown = true;
    }

    private void shutdownPublisher()
    {
        if(publisher != null)
        {
            publisher.unpublish();
            publisher.close();
        }
        publisher = null;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getFirstPacketTimecode()
    {
        return packets.stream()
                .reduce((prev, next) -> prev)
                .map(AMFPacket::getAbsTimecode).orElse(-1L);
    }

    public long getLastPacketTimecode()
    {
        return packets.stream()
                .reduce((prev, next) -> next)
                .map(AMFPacket::getAbsTimecode).orElse(-1L);
    }

    public String getStreamName()
    {
        return streamName;
    }
}
