/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.transcoder;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.transcoder.model.*;

public class TranscoderAudioFrameListener implements ITranscoderAudioFrameNotify2
{
    private final SpeechHandler speechHandler;

    public TranscoderAudioFrameListener(SpeechHandler speechHandler)
    {
        this.speechHandler = speechHandler;
    }

    @Override
    public void onAudioFrame(TranscoderSessionAudio transcoderSessionAudio, TranscoderNativeAudioFrame frame)
    {
    }

    @Override
    public void onAudioFrameAfterResample(TranscoderSessionAudio transcoderSessionAudio, TranscoderNativeAudioFrame frame)
    {
        speechHandler.addAudioFrame(frame.buffer);
    }
}
