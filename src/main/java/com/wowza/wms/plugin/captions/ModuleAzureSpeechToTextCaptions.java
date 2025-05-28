/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.azure.AzureCaptionsTranscoderActionListener;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.plugin.captions.stream.DelayedStreamListener;
import com.wowza.wms.plugin.captions.stream.LiveStreamPacketizerListener;
import com.wowza.wms.plugin.captions.transcoder.CaptionsTranscoderCreateListener;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.timedtext.model.ITimedTextConstants;

import java.util.*;
import java.util.concurrent.*;

public class ModuleAzureSpeechToTextCaptions extends ModuleCaptionsBase
{
    static
    {
        CLASS = ModuleAzureSpeechToTextCaptions.class;
        MODULE_NAME = CLASS.getSimpleName();
        try
        {
            Class.forName(SpeechConfig.class.getName());
        }
        catch (ClassNotFoundException e)
        {
            WMSLoggerFactory.getLogger(CLASS).error(String.format("%s exception: %s", MODULE_NAME, e), e);
        }
    }

    public static final String PROP_CAPTIONS_ENABLED = "speechToTextCaptionsEnabled";
    public static final String PROP_DEFAULT_CAPTION_LANGUAGES = ITimedTextConstants.PROP_LIVE_CAPTION_DEFAULT_LANGUAGES;
    public static final String PROP_RECOGNITION_LANGUAGE = "speechToTextRecognitionLanguage";
    public static final String PROP_PHRASE_LIST = "speechToTextPhraseList";
    public static final String PROP_PROFANITY_MASK_OPTION = "speechToTextProfanityMaskOption";
    public static final String PROP_SUBSCRIPTION_KEY = "speechToTextSubscriptionKey";
    public static final String PROP_SERVICE_REGION = "speechToTextServiceRegion";
    private final Map<String, SpeechHandler> speechHandlers = new ConcurrentHashMap<>();
    private final Map<String, DelayedStream> delayedStreams = new ConcurrentHashMap<>();
    private DelayedStreamListener delayedStreamListener;
    private String subscriptionKey;
    private String serviceRegion;
    private boolean enabled = false;

    public void onAppCreate(IApplicationInstance appInstance)
    {
        super.onAppCreate(appInstance);
        enabled = appInstance.getProperties().getPropertyBoolean(PROP_CAPTIONS_ENABLED, enabled);
        try
        {
            subscriptionKey = Objects.requireNonNull(appInstance.getProperties().getPropertyStr(PROP_SUBSCRIPTION_KEY), "Azure Speech Subscription Key not set");
            serviceRegion = Objects.requireNonNull(appInstance.getProperties().getPropertyStr(PROP_SERVICE_REGION), "Azure Speech Service Region not set");
        }
        catch (NullPointerException npe)
        {
            logger.error(String.format("%s.onAppCreate [%s] error: %s", MODULE_NAME, appInstance.getContextStr(), npe.getMessage()));
            enabled = false;
        }
        logger.info(String.format("%s.onAppCreate: [%s] version: %s enabled: %b", MODULE_NAME, appInstance.getContextStr(), MODULE_VERSION, enabled));
    }

    public void onAppStart(IApplicationInstance appInstance)
    {
        if (!enabled)
            return;
        try
        {
            appInstance.addLiveStreamPacketizerListener(new LiveStreamPacketizerListener(appInstance));
            appInstance.addLiveStreamTranscoderListener(new CaptionsTranscoderCreateListener(new AzureCaptionsTranscoderActionListener(appInstance, speechHandlers, delayedStreams,
                    subscriptionKey, serviceRegion)));
            delayedStreamListener = new DelayedStreamListener(appInstance, delayedStreams);
            appInstance.addMediaCasterListener(delayedStreamListener);
        }
        catch (Exception e)
        {
            logger.error(MODULE_NAME + ".onAppStart exception", e);
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
