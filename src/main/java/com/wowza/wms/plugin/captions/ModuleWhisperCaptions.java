/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.plugin.captions.stream.DelayedStreamListener;
import com.wowza.wms.plugin.captions.stream.LiveStreamPacketizerListener;
import com.wowza.wms.plugin.captions.transcoder.CaptionsTranscoderCreateListener;
import com.wowza.wms.plugin.captions.whisper.WhisperCaptionsTranscoderActionListener;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.stream.IMediaStream;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleWhisperCaptions extends ModuleCaptionsBase
{

    private static final String DEFAULT_WS_URL = "ws://localhost/ws:3000";

    static
    {
        CLASS = ModuleWhisperCaptions.class;
        MODULE_NAME = CLASS.getSimpleName();
    }

    public static final String PROP_CAPTIONS_ENABLED = "whisperCaptionsEnabled";

    private final Map<String, SpeechHandler> speechHandlers = new ConcurrentHashMap<>();
    private final Map<String, DelayedStream> delayedStreams = new ConcurrentHashMap<>();

    private DelayedStreamListener delayedStreamListener;

    private boolean enabled = DEFAULT_CAPTIONS_ENABLED;

    public void onAppStart(IApplicationInstance appInstance)
    {
        enabled = appInstance.getProperties().getPropertyBoolean(PROP_CAPTIONS_ENABLED, enabled);
        if (!enabled)
        {
            logger.info(MODULE_NAME + ".onAppStart[" + appInstance.getContextStr() + "] Whisper captions module disabled");
            return;
        }
        logger.info(MODULE_NAME + ".onAppStart[" + appInstance.getContextStr() + "]");
        try
        {
            appInstance.addLiveStreamPacketizerListener(new LiveStreamPacketizerListener(appInstance));
            appInstance.addLiveStreamTranscoderListener(new CaptionsTranscoderCreateListener(new WhisperCaptionsTranscoderActionListener(appInstance, speechHandlers, delayedStreams)));
            delayedStreamListener = new DelayedStreamListener(appInstance, delayedStreams);
            appInstance.addMediaCasterListener(delayedStreamListener);
        }
        catch (Exception e)
        {
            logger.error(MODULE_NAME + ".onAppStart[" + appInstance.getContextStr() + "] exception", e);
        }
    }

    public void onStreamCreate(IMediaStream stream)
    {
        if (!enabled)
            return;
        stream.addClientListener(delayedStreamListener);
        stream.addLivePacketListener(delayedStreamListener);
    }
}
