/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2022 Wowza Media Systems, LLC. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
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
}
