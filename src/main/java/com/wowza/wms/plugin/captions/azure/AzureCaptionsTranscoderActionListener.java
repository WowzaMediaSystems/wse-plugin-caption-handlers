/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.azure;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.plugin.captions.transcoder.AudioResamplingTranscoderActionListener;

import java.util.Map;

public class AzureCaptionsTranscoderActionListener extends AudioResamplingTranscoderActionListener
{
    private final String subscriptionKey;
    private final String serviceRegion;

    public AzureCaptionsTranscoderActionListener(IApplicationInstance appInstance, Map<String, SpeechHandler> handlers, Map<String, DelayedStream> delayedStreams,
                                                 String subscriptionKey, String serviceRegion)
    {
        super(appInstance, handlers, delayedStreams);
        this.subscriptionKey = subscriptionKey;
        this.serviceRegion = serviceRegion;
    }

    @Override
    public SpeechHandler getSpeechHandler(CaptionHandler captionHandler)
    {
        return new AzureSpeechToTextHandler(appInstance, captionHandler, subscriptionKey, serviceRegion);
    }
}
