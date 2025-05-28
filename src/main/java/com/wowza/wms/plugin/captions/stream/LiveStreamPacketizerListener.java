/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.cupertinostreaming.livestreampacketizer.LiveStreamPacketizerCupertino;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.livepacketizer.*;
import com.wowza.wms.timedtext.model.ITimedTextConstants;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.*;


public class LiveStreamPacketizerListener extends LiveStreamPacketizerActionNotifyBase
{
    private final IApplicationInstance appInstance;

    public LiveStreamPacketizerListener(IApplicationInstance appInstance)
    {
        this.appInstance = appInstance;
    }

    @Override
    public void onLiveStreamPacketizerCreate(ILiveStreamPacketizer packetizer, String streamName)
    {
        IMediaStream stream = appInstance.getStreams().getStream(streamName);
        if (packetizer instanceof LiveStreamPacketizerCupertino && (streamName.endsWith(DELAYED_STREAM_SUFFIX) || (stream.isTranscodeResult() && !streamName.endsWith(RESAMPLED_STREAM_SUFFIX))))
        {
            packetizer.getProperties().setProperty(ITimedTextConstants.PROP_CUPERTINO_LIVE_USE_WEBVTT, true);
        }
    }
}
