/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2025 Wowza Media Systems, LLC. All rights reserved.
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

    public static final String PROP_CAPTIONS_ENABLED = "verbitCaptionsEnabled";

    private final Map<String, SpeechHandler> speechHandlers = new ConcurrentHashMap<>();
    private final Map<String, DelayedStream> delayedStreams = new ConcurrentHashMap<>();

    private DelayedStreamListener delayedStreamListener;

    private String wssUrl = DEFAULT_WS_URL;
    private boolean enabled = DEFAULT_CAPTIONS_ENABLED;

    public void onAppStart(IApplicationInstance appInstance)
    {
        wssUrl = URLDecoder.decode(appInstance.getProperties().getPropertyStr("whisperWebsocketUrl", wssUrl), Charset.defaultCharset());
        if (wssUrl.isBlank())
        {
            logger.error(MODULE_NAME + ".onAppStart[" + appInstance.getContextStr() + "] whisperWebsocketUrl is required");
            enabled = false;
        }
        else
            enabled = appInstance.getProperties().getPropertyBoolean(PROP_CAPTIONS_ENABLED, enabled);
        if (!enabled)
        {
            logger.info(MODULE_NAME + ".onAppStart[" + appInstance.getContextStr() + "] Verbit captions module disabled");
            return;
        }

        try
        {
            appInstance.addLiveStreamPacketizerListener(new LiveStreamPacketizerListener(appInstance));
            appInstance.addLiveStreamTranscoderListener(new CaptionsTranscoderCreateListener(new WhisperCaptionsTranscoderActionListener(appInstance, speechHandlers, delayedStreams, wssUrl)));
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
