/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.azure;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.translation.*;
import com.wowza.wms.application.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.caption.Caption;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHelper;
import com.wowza.wms.plugin.captions.caption.CaptionTiming;
import com.wowza.wms.timedtext.model.ITimedTextConstants;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.wowza.wms.plugin.captions.ModuleAzureSpeechToTextCaptions.*;

public class AzureSpeechToTextHandler implements SpeechHandler
{
    private static final Class<AzureSpeechToTextHandler> CLASS = AzureSpeechToTextHandler.class;
    private static final String CLASS_NAME = CLASS.getSimpleName();
    public static final String DEFAULT_RECOGNITION_LANGUAGE = "en-US";
    private final CaptionHandler captionHandler;
    private final WMSLogger logger;
    private final PushAudioInputStream audioStream = PushAudioInputStream.createPushStream();
    private final Semaphore semaphore = new Semaphore(0);
    private final SpeechConfig speechConfig;
    private final String recognitionLanguage;
    private final Map<String, String> languageMap;
    private final List<String> translationLanguages;
    private final List<String> phrases;
    private final boolean debugLog;
    private final int maxLineLength;
    private final int maxLines;
    private final String firstPassTerminators;
    private final int firstPassPercentage;

    public AzureSpeechToTextHandler(IApplicationInstance appInstance, CaptionHandler captionHandler, String subscriptionKey,
            String serviceRegion)
    {
        WMSProperties props = appInstance.getProperties();
        this.logger = WMSLoggerFactory.getLoggerObj(appInstance);
        debugLog = props.getPropertyBoolean(PROP_CAPTIONS_DEBUG_LOG, false);
        firstPassTerminators = props.getPropertyStr(PROP_LINE_TERMINATORS, DEFAULT_FIRST_PASS_TERMINATORS);
        firstPassPercentage = props.getPropertyInt(PROP_FIRST_PASS_PERCENTAGE, DEFAULT_FIRST_PASS_PERCENTAGE);
        maxLineLength = props.getPropertyInt(PROP_MAX_CAPTION_LINE_LENGTH, CaptionHelper.defaultMaxLineLengthSBCS);
        maxLines = props.getPropertyInt(PROP_MAX_CAPTION_LINE_COUNT, 2);
        this.captionHandler = captionHandler;
        recognitionLanguage = toLocale(props.getPropertyStr(PROP_RECOGNITION_LANGUAGE, DEFAULT_RECOGNITION_LANGUAGE))
                .toLanguageTag();
        if (!isBCP47WithRegion(recognitionLanguage))
            throw new RuntimeException("Invalid recognition language: " + recognitionLanguage);

        String languagesStr = appInstance.getTimedTextProperties().getPropertyStr(PROP_DEFAULT_CAPTION_LANGUAGES, ITimedTextConstants.LANGUAGE_ID_ENGLISH);
        languageMap = Arrays.stream(languagesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toMap(
                        s -> toLocale(s).getLanguage(),
                        s -> s,
                        (existing, replacement) -> existing));

        translationLanguages = languageMap.keySet().stream()
                .filter(lang -> !lang.equals(Locale.forLanguageTag(recognitionLanguage).getLanguage()))
                .collect(Collectors.toList());
        String phraseStr = props.getPropertyStr(PROP_PHRASE_LIST, "");
        phrases = Arrays.stream(phraseStr.split(";"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        speechConfig = translationLanguages.isEmpty() ? SpeechConfig.fromSubscription(subscriptionKey, serviceRegion) :
                       SpeechTranslationConfig.fromSubscription(subscriptionKey, serviceRegion);
        speechConfig.setSpeechRecognitionLanguage(recognitionLanguage);

        ProfanityOption profanityOption = ProfanityOption.Masked;
        try
        {
            profanityOption = ProfanityOption.valueOf(props.getPropertyStr(PROP_PROFANITY_MASK_OPTION, "Masked"));
        }
        catch (IllegalArgumentException e)
        {
            // no-op
        }
        speechConfig.setProfanity(profanityOption);
    }

    @Override
    public void run()
    {
        try(speechConfig; audioStream; AudioConfig audioConfig = AudioConfig.fromStreamInput(audioStream))
        {
            Recognizer recognizer;
            if (speechConfig instanceof SpeechTranslationConfig) {
                translationLanguages.forEach(lang -> ((SpeechTranslationConfig) speechConfig).addTargetLanguage(lang));
                recognizer = new TranslationRecognizer((SpeechTranslationConfig) speechConfig, audioConfig);
            }
            else
                recognizer = new SpeechRecognizer(speechConfig, audioConfig);
            try (recognizer)
            {
                String recognizerName = recognizer.getClass().getSimpleName();

                if (!phrases.isEmpty())
                {
                    PhraseListGrammar grammar = PhraseListGrammar.fromRecognizer(recognizer);
                    phrases.forEach(grammar::addPhrase);
                }

                recognizer.sessionStarted.addEventListener((s, e) -> logger.info(MODULE_NAME + "::" + CLASS_NAME + "::" + recognizerName + " session started. session: " + e.getSessionId()));
                recognizer.sessionStopped.addEventListener(((s, e) -> logger.info(MODULE_NAME + "::" + CLASS_NAME + "::" + recognizerName + " session stopped. session: " + e.getSessionId())));
                recognizer.speechStartDetected.addEventListener((s, e) -> logger.info(MODULE_NAME + "::" + CLASS_NAME + "::" + recognizerName + " speech start detected. session: " + e.getSessionId()));
                recognizer.speechEndDetected.addEventListener((s, e) -> logger.info(MODULE_NAME + "::" + CLASS_NAME + "::" + recognizerName + " speech End detected. session: " + e.getSessionId()));
                if (recognizer instanceof TranslationRecognizer)
                {
                    ((TranslationRecognizer)recognizer).recognizing.addEventListener((s, e) -> handleRecognizingEvent(e.getSessionId(), e.getResult()));
                    ((TranslationRecognizer)recognizer).recognized.addEventListener((s, e) -> handleRecognizedEvent(e.getSessionId(), e.getResult()));
                    ((TranslationRecognizer)recognizer).canceled.addEventListener((s, e) -> handleCancelledEvent(e.getSessionId(), e.getReason(), e.getErrorCode(), e.getErrorDetails()));
                    ((TranslationRecognizer)recognizer).startContinuousRecognitionAsync().get();
                    semaphore.acquire();
                    ((TranslationRecognizer)recognizer).stopContinuousRecognitionAsync().get();
                }
                else
                {
                    ((SpeechRecognizer)recognizer).recognizing.addEventListener((s, e) -> handleRecognizingEvent(e.getSessionId(), e.getResult()));
                    ((SpeechRecognizer)recognizer).recognized.addEventListener((s, e) -> handleRecognizedEvent(e.getSessionId(), e.getResult()));
                    ((SpeechRecognizer)recognizer).canceled.addEventListener((s, e) -> handleCancelledEvent(e.getSessionId(), e.getReason(), e.getErrorCode(), e.getErrorDetails()));

                    ((SpeechRecognizer)recognizer).startContinuousRecognitionAsync().get();
                    semaphore.acquire();
                    ((SpeechRecognizer)recognizer).stopContinuousRecognitionAsync().get();
                }
            }
            catch (InterruptedException ignored)
            {
            }
        }
        catch (Exception e)
        {
            logger.error(MODULE_NAME + "::" + CLASS_NAME + ".run exception",  e);
        }
    }

    private void handleRecognizingEvent(String sessionId, RecognitionResult result)
    {
        if (debugLog)
        {
            Instant start = CaptionHelper.epochInstantFromTicks(result.getOffset());
            Instant end = CaptionHelper.epochInstantFromTicks(result.getOffset().add(result.getDuration()));
            long latency = Long.parseLong(result.getProperties().getProperty(PropertyId.SpeechServiceResponse_RecognitionLatencyMs));
            String json = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
            logger.info(MODULE_NAME + "::" + CLASS_NAME + "handleRecognizingEvent: session: " + sessionId + " RECOGNIZING: Timing: " + getTimestamp(start, end) + " Latency=" + latency + " Result=" + json);
        }
    }

    private void handleRecognizedEvent(String sessionId, RecognitionResult result)
    {
        if (result.getReason() == ResultReason.NoMatch && debugLog)
            logger.info(MODULE_NAME + "::" + CLASS_NAME + "handleRecognizedEvent: session: " + sessionId + " NOMATCH: Speech could not be recognized.");
        else
        {
            Instant start = CaptionHelper.epochInstantFromTicks(result.getOffset());
            Instant end = CaptionHelper.epochInstantFromTicks(result.getOffset().add(result.getDuration()));
            long latency = Long.parseLong(result.getProperties().getProperty(PropertyId.SpeechServiceResponse_RecognitionLatencyMs));
            String json = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
            if (debugLog)
                logger.info(MODULE_NAME + "::" + CLASS_NAME + "handleRecognizedEvent: session: " + sessionId + " RECOGNIZED: Timing: " + getTimestamp(start, end) + " Latency=" + latency + " Result=" + json);
            handleResult(result, start, end);
        }
    }

    private void handleResult(RecognitionResult result, Instant start, Instant end)
    {
        CaptionTiming captionTiming = new CaptionTiming(start, end);
        List<Caption> sourceCaptions = CaptionHelper.getCaptions(languageMap.get(Locale.forLanguageTag(recognitionLanguage).getLanguage()), maxLineLength, maxLines,
				firstPassTerminators, firstPassPercentage, captionTiming, result.getText());
        sourceCaptions.forEach(captionHandler::handleCaption);

        if (result instanceof TranslationRecognitionResult)
        {
            ((TranslationRecognitionResult)result).getTranslations().forEach((language, translation) -> {
                List<Caption> translatedCaptions = CaptionHelper.getCaptions(languageMap.get(language), maxLineLength, maxLines,
                        firstPassTerminators, firstPassPercentage, captionTiming, translation);
                translatedCaptions.forEach(captionHandler::handleCaption);
            });
        }
    }

    private void handleCancelledEvent(String sessionId, CancellationReason reason, CancellationErrorCode errorCode, String errorDetails)
    {
        logger.warn(MODULE_NAME + "::" + CLASS_NAME + "handleCancelledEvent: session: " + sessionId + " Translation Session Cancelled: Reason=" + reason);
        if (reason == CancellationReason.Error) {
            logger.error(MODULE_NAME + "::" + CLASS_NAME + "handleCancelledEvent: session: " + sessionId + " Translation Session Cancelled: ErrorCode=" + errorCode);
            logger.error(MODULE_NAME + "::" + CLASS_NAME + "handleCancelledEvent: session: " + sessionId + " Translation Session Cancelled: ErrorDetails=" + errorDetails);
        }
        semaphore.release();
    }

    @Override
    public void addAudioFrame(byte[] frame)
    {
        audioStream.write(frame);
    }

    @Override
    public void close()
    {
        audioStream.close();
        semaphore.release();
    }

    private String getTimestamp(Instant startTime, Instant endTime)
    {
        var format = "HH:mm:ss.SSS";
        // Set the timezone to UTC so the time is not adjusted for our local time zone.
        var formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.from(ZoneOffset.UTC));
        return String.format("%s --> %s", formatter.format(startTime), formatter.format(endTime));
    }
}
