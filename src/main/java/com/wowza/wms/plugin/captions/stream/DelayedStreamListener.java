/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.mediacaster.*;
import com.wowza.wms.stream.*;

import java.util.Map;
import java.util.concurrent.Executors;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.DELAYED_STREAM_SUFFIX;

public class DelayedStreamListener implements IMediaStreamLivePacketNotify, MediaCasterNotify, StreamActionNotify
{
    protected final IApplicationInstance appInstance;
    protected final Map<String, DelayedStream> delayedStreams;

    public DelayedStreamListener(IApplicationInstance appInstance, Map<String, DelayedStream> delayedStreams)
    {
        this.appInstance = appInstance;
        this.delayedStreams = delayedStreams;
    }

    @Override
    public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
    {
        String mappedName  = streamName.replace(".stream", "");
        delayedStreams.computeIfPresent(mappedName, (k, delayedStream) -> {
            delayedStream.shutdown();
            return null;
        });
    }

    @Override
    public void onLivePacket(IMediaStream stream, AMFPacket packet)
    {
        String streamName = stream.getName();
        if (stream.isTranscodeResult() || streamName.endsWith(DELAYED_STREAM_SUFFIX))
            return;
        String mappedName = streamName.replace(".stream", "");
        DelayedStream delayedStream = delayedStreams.computeIfAbsent(mappedName,
                name -> new DelayedStream(appInstance, streamName, Executors.newSingleThreadScheduledExecutor()));
        delayedStream.writePacket(packet);
    }

    @Override
    public void onStreamStop(IMediaCaster mediaCaster)
    {
        String mappedName = mediaCaster.getMediaCasterId().replace(".stream", "");
        delayedStreams.computeIfPresent(mappedName, (k, delayedStream) -> {
            delayedStream.shutdown();
            return null;
        });
    }
}
