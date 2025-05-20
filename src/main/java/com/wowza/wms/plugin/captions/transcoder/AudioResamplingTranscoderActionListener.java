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

package com.wowza.wms.plugin.captions.transcoder;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.azure.AzureSpeechToTextHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.caption.DelayedStreamCaptionHandler;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.transcoder.model.LiveStreamTranscoder;
import com.wowza.wms.transcoder.model.TranscoderSessionAudio;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.DELAYED_STREAM_SUFFIX;

public abstract class AudioResamplingTranscoderActionListener extends CaptionsTranscoderActionListener
{
    protected final IApplicationInstance appInstance;
    private final Map<String, SpeechHandler> handlers;
    private final Map<String, DelayedStream> delayedStreams;

    public AudioResamplingTranscoderActionListener(IApplicationInstance appInstance, Map<String, SpeechHandler> handlers, Map<String, DelayedStream> delayedStreams)
    {
        this.appInstance = appInstance;
        this.handlers = handlers;
        this.delayedStreams = delayedStreams;
    }

    @Override
    public void onInitBeforeLoadTemplate(LiveStreamTranscoder transcoder)
    {
        System.out.println("AudioResamplingTranscoderActionListener.onInitBeforeLoadTemplate");
        super.onInitBeforeLoadTemplate(transcoder);
        if (!transcoder.getStreamName().endsWith(DELAYED_STREAM_SUFFIX))
        {
            String resampleTemplate = "audioResample.xml";
            String templateDir = transcoder.getTemplateDir();
            if (!(new File(templateDir, resampleTemplate).exists()))
            {
                String appName = appInstance.getApplication().getName();
                String appInstanceName = appInstance.getName();
                String regex = String.format("(/%s|/%s)", appName, appInstanceName);
                templateDir = templateDir.replaceAll(regex, "");
            }
            if (new File(templateDir, resampleTemplate).exists())
            {
                transcoder.setTemplateName(resampleTemplate);
                transcoder.setTemplateDir(templateDir);
            }
        }
    }

    @Override
    public void onInitStop(LiveStreamTranscoder transcoder)
    {
        String streamName = transcoder.getStreamName();
        if (streamName.endsWith(DELAYED_STREAM_SUFFIX))
            return;
        String mappedName  = streamName.replace(".stream", "");
        TranscoderSessionAudio sessionAudio = transcoder.getTranscodingSession().getSessionAudio();
        SpeechHandler speechHandler = handlers.computeIfAbsent(mappedName, k -> {
            DelayedStream delayedStream = delayedStreams.computeIfAbsent(mappedName,
                    name -> new DelayedStream(appInstance, streamName, Executors.newSingleThreadScheduledExecutor()));
            CaptionHandler captionHandler = new DelayedStreamCaptionHandler(appInstance, delayedStream);
            SpeechHandler handler = getSpeechHandler(captionHandler);
            new Thread(handler, AzureSpeechToTextHandler.class.getSimpleName() + "[" + appInstance.getContextStr() + "/" + streamName + "]")
                    .start();
            return handler;
        });
        TranscoderAudioFrameListener frameListener = new TranscoderAudioFrameListener(speechHandler);
        sessionAudio.addFrameListener(frameListener);
    }

    public abstract SpeechHandler getSpeechHandler(CaptionHandler captionHandler);

    @Override
    public void onShutdownStart(LiveStreamTranscoder transcoder)
    {
        String mappedName  = transcoder.getStreamName().replace(".stream", "");
        handlers.computeIfPresent(mappedName, (k, handler) -> {
            handler.close();
            return null;
        });
    }
}
