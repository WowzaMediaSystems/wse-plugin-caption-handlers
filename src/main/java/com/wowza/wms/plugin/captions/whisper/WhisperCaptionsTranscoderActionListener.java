/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.whisper;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.plugin.captions.transcoder.AudioResamplingTranscoderActionListener;
import com.wowza.wms.application.IApplicationInstance;

import java.util.Map;

public class WhisperCaptionsTranscoderActionListener extends AudioResamplingTranscoderActionListener
{
    public WhisperCaptionsTranscoderActionListener(IApplicationInstance appInstance, Map<String, SpeechHandler> speechHandlers, Map<String, DelayedStream> delayedStreams)
    {
        super(appInstance, speechHandlers, delayedStreams);
    }

    @Override
    public SpeechHandler getSpeechHandler(CaptionHandler captionHandler)
    {
        return new WhisperSpeechToTextHandler(appInstance, captionHandler);
    }
}
