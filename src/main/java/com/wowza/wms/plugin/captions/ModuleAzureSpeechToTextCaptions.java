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
