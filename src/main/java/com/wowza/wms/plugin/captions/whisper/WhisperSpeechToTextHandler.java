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

package com.wowza.wms.plugin.captions.whisper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.caption.Caption;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHelper;
import com.wowza.util.StringUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.plugin.captions.whisper.model.Alternative;
import com.wowza.wms.plugin.captions.whisper.model.CaptionLine;
import com.wowza.wms.plugin.captions.whisper.model.Item;
import com.wowza.wms.plugin.captions.whisper.model.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.*;
import static com.wowza.wms.plugin.captions.stream.DelayedStream.DEFAULT_START_DELAY;

public class WhisperSpeechToTextHandler implements SpeechHandler
{
    private static final Class<WhisperSpeechToTextHandler> CLASS = WhisperSpeechToTextHandler.class;
    private static final String CLASS_NAME = CLASS.getSimpleName();

    private final LinkedBlockingQueue<ByteBuffer> audioBuffer = new LinkedBlockingQueue<>();
    private final Map<String, LinkedList<CaptionLine>> captionLines = new ConcurrentHashMap<>();
    private final Map<String, String> speakers = new ConcurrentHashMap<>();

    private final WMSLogger logger;
    private final CaptionHandler captionHandler;
    private final boolean debugLog;
    private final int maxLineLength;
    private final int maxLineCount;
    private final String wssUrl;
    private final int maxRetries = 5;
    private final String speakerChangeIndicator;
    private final IApplicationInstance appInstance;
    private final int newLineThreshold;
    private final long delay;

    private String responseType = "captions";
    private volatile long messageOffset = -1;

    private volatile boolean doQuit = false;
    private volatile boolean outputRunning = false;

    private Thread runningThread;

    private WebSocket websocket;
    private int retryCount = 0;

    public WhisperSpeechToTextHandler(IApplicationInstance appInstance, CaptionHandler captionHandler, String wssUrl)
    {
        this.appInstance = appInstance;
        this.logger = WMSLoggerFactory.getLoggerObj(appInstance);
        this.captionHandler = captionHandler;
        WMSProperties props = appInstance.getProperties();
        this.debugLog = props.getPropertyBoolean(PROP_CAPTIONS_DEBUG_LOG, false);
        this.maxLineLength = props.getPropertyInt(PROP_MAX_CAPTION_LINE_LENGTH, CaptionHelper.defaultMaxLineLengthSBCS);
        this.maxLineCount = props.getPropertyInt(PROP_MAX_CAPTION_LINE_COUNT, 2);
        this.speakerChangeIndicator = props.getPropertyStr(PROP_SPEAKER_CHANGE_INDICATOR, DEFAULT_SPEAKER_CHANGE_INDICATOR);
        this.newLineThreshold = props.getPropertyInt(PROP_NEW_LINE_THRESHOLD, DEFAULT_NEW_LINE_THRESHOLD);
        this.delay = props.getPropertyLong(PROP_CAPTIONS_STREAM_DELAY, DEFAULT_START_DELAY);
        this.wssUrl = wssUrl;
        this.websocket = connectWebsocket();
    }

    private WebSocket connectWebsocket()
    {
        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Builder builder = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5));

        while (retryCount < maxRetries)
        {
            try
            {
                CompletableFuture<WebSocket> future = builder.buildAsync(URI.create(wssUrl), new WebSocketListener());
                return future.join();
            }
            catch (CompletionException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof IOException || cause instanceof InterruptedException)
                {
                    retryCount++;
                    if (retryCount >= maxRetries)
                        throw new RuntimeException("Failed to connect WebSocket after " + maxRetries + " retries", e);
                    addExponentialDelayWithJitter();
                }
                else
                    throw e;
            }
        }
        throw new RuntimeException("Failed to connect WebSocket after " + maxRetries + " retries");
    }

    private void addExponentialDelayWithJitter()
    {
        long jitter = (long) (Math.random() * 1000); // Add up to 1 second of random jitter
        long baseDelay = 500;
        long delay = baseDelay * (1L << retryCount) + jitter;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }

    private void reconnect()
    {
        while (retryCount < maxRetries)
        {
            try
            {
                logger.info(CLASS_NAME + ".WebSocketListener.reconnect: Attempting to reconnect...");
                if (websocket != null && !websocket.isOutputClosed())
                {
                    websocket.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting").join();
                }
                websocket = connectWebsocket();
                break; // Exit the method if reconnection is successful
            }
            catch (Exception e)
            {
                retryCount++;
                if (retryCount >= maxRetries) {
                    logger.error(CLASS_NAME + ".WebSocketListener.reconnect: Failed to reconnect after " + maxRetries + " attempts", e);
                    break;
                }
                addExponentialDelayWithJitter();
            }
        }
    }

    @Override
    public void run()
    {
        runningThread = Thread.currentThread();
        while (!doQuit)
        {
            try
            {
                if (!outputRunning)
                {
                    outputRunning = true;
                    appInstance.getVHost().getThreadPool().execute(this::processPendingCaptions);
                }

                ByteBuffer frame = audioBuffer.take();
                websocket.sendBinary(frame, true).join();
            }
            catch (Exception e)
            {
                if (doQuit)
                    break;
                reconnect();
                if (websocket == null)
                    break;
            }
        }
    }

    private void processPendingCaptions()
    {
        try
        {
//            if (debugLog)
//                logger.info(CLASS_NAME + ".processPendingCaptions: processing pending captions: " + captionLines);
            List<Caption> captions = new ArrayList<>();
            for (Map.Entry<String, LinkedList<CaptionLine>> entry : captionLines.entrySet())
            {
                String language = entry.getKey();
                LinkedList<CaptionLine> lines = entry.getValue();

                synchronized (lines)
                {
                    Instant start = null;
                    Instant end = null;
                    List<String> textList = new ArrayList<>();
                    if (lines.size() > maxLineCount || (!lines.isEmpty() && lines.peekLast().getTimeAdded() < System.currentTimeMillis() - delay / 2))
                    {
                        while (textList.size() < maxLineCount && !lines.isEmpty())
                        {
                            CaptionLine line = lines.removeFirst();
                            if (start == null)
                                start = line.getStart();
                            end = line.getEnd();
                            textList.add(line.getText());
                        }
                    }

                    if (!textList.isEmpty())
                    {
                        // todo: make trackid dynamic
                        Caption caption = new Caption(language, start, end, String.join("\n", textList), 99);
                        captions.add(caption);
                    }
                }
            }
            captions.forEach(captionHandler::handleCaption);
        }
        catch (Exception e)
        {
            logger.error(CLASS_NAME + ".processPendingCaptions: Error processing pending captions: " + captionLines, e);
        }
        finally
        {
            outputRunning = false;
        }
    }

    @Override
    public void addAudioFrame(byte[] frame)
    {
        audioBuffer.add(ByteBuffer.wrap(frame));
    }

    @Override
    public void close()
    {
        logger.info(CLASS_NAME + ".close()");
        try
        {
            doQuit = true;
            Map<String, Object> event = Map.of("event", "EOS", "payload", Collections.emptyMap());
            String json = new ObjectMapper().writeValueAsString(event);
            websocket.sendText(json, true).join();
            if (runningThread != null)
                runningThread.interrupt();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            websocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing").join();
        }
    }

    private class WebSocketListener implements WebSocket.Listener
    {
        private final List<CharSequence> buffer = new ArrayList<>();

        private CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
        {
            buffer.add(data);
            webSocket.request(1);
            if (last)
            {
                String message = String.join("", buffer);
                try
                {
                    if (debugLog)
                        logger.info(CLASS_NAME + ".WebSocketListener.onText(): " + message);
                    handleCaptionMessage(message);
                }
                catch (Exception e)
                {
                    logger.error(CLASS_NAME + ".WebSocketListener.onText(): " + message, e);
                }

                buffer.clear();
                accumulatedMessage.complete(null);
                CompletionStage<?> cf = accumulatedMessage;
                accumulatedMessage = new CompletableFuture<>();
                return cf;
            }
            return accumulatedMessage;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error)
        {
            logger.error(CLASS_NAME + ".WebSocketListener.onError: " + error.getMessage(), error);
            if (!doQuit)
                reconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
        {
            logger.info(CLASS_NAME + ".WebSocketListener.onClose: " + statusCode + " " + reason);
            if (!doQuit)
                reconnect();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }

    private void handleCaptionMessage(String message) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<?,?> responseMap = mapper.readValue(message, Map.class);
        Response response = mapper.convertValue(responseMap.get("response"), Response.class);
        String type = response.getType();
        if (type.equalsIgnoreCase("transcript") && messageOffset == -1)
        {
            messageOffset = (long) (response.getStart() * 1000);
            logger.info(CLASS_NAME + ".handleCaptionMessage: messageOffset: " + messageOffset);
        }
        String language = response.getLanguageCode() != null ? Locale.forLanguageTag(response.getLanguageCode()).getLanguage() : "en";
        List<Alternative> alternatives = response.getAlternatives();
        if (!alternatives.isEmpty())
        {
            LinkedList<CaptionLine> lines = captionLines.computeIfAbsent(language, k -> new LinkedList<>());
            synchronized (lines)
            {
                Alternative alternative = alternatives.get(0);
                String text = alternative.getTranscript();
                boolean isFinal = response.isFinal();
                if (!StringUtils.isEmpty(text) && isFinal && type.equalsIgnoreCase(responseType))
                {
                    Instant start = CaptionHelper.epochInstantFromMillis((long) (response.getStart() * 1000) - messageOffset);
                    Instant end = CaptionHelper.epochInstantFromMillis((long) (response.getEnd() * 1000) - messageOffset);

                    StringBuilder sb = new StringBuilder();
                    CaptionLine line = lines.peekLast();
                    if (line != null && Duration.between(line.getEnd(), start).toMillis() < newLineThreshold)
                    {
                        start = line.getStart();
                        sb.append(line.getText());
                    }
                    else
                    {
                        line = new CaptionLine(language);
                        line.setStart(start);
                        lines.add(line);
                    }

                    List<Item> items = alternative.getItems();
                    for (int i = 0; i < items.size(); i++)
                    {
                        Item item = items.get(i);
                        // check if the speaker has changed.  If so, create a new line and add the speaker change indicator
                        String speaker = item.getSpeakerId();
                        String lastSpeaker = speakers.computeIfAbsent(language, k -> speaker);
                        if (!speaker.equals(lastSpeaker))
                        {
                            if (debugLog)
                                logger.info(CLASS_NAME + ".handleCaptionMessage(speaker change): speaker: " + speaker + ", lastSpeaker = " + lastSpeaker);
                            if (sb.length() > 0)
                            {
                                line.setEnd(end);
                                line.setText(sb.toString());
                                sb.setLength(0);
                                if (debugLog)
                                    logger.info(CLASS_NAME + ".handleCaptionMessage(newSpeaker): speaker: " + speaker + ", start: " + line.getStart() + ", end: " + line.getEnd() + ", text: " + line.getText());
                            }
                            else
                            {
                                lines.remove(line);
                                if (debugLog)
                                    logger.info(CLASS_NAME + ".handleCaptionMessage(newSpeaker - empty line): speaker: " + speaker + ", start: " + line.getStart() + ", end: " + line.getEnd());
                            }
                            line = new CaptionLine(language);
                            line.setStart(CaptionHelper.epochInstantFromMillis((long) (item.getStart() * 1000) - messageOffset));
                            lines.add(line);
                            sb.append(speakerChangeIndicator).append(" ").append(item.getValue());
                        }
                        else
                        {
                            int length = sb.length();
                            if (length > 0)
                            {
                                // check if the text length will exceed the max line length. If so, create a new line
                                int itemLength = item.getValue().length();
                                if (isText(item))
                                    itemLength += 1; // add space for the space
                                int j = i + 1;
                                while (j < items.size()) // add punctuation length
                                {
                                    if (isText(items.get(j)))
                                        break;
                                    itemLength += items.get(j).getValue().length();
                                    j++;
                                }
                                if (length + itemLength > maxLineLength)
                                {
                                    line.setEnd(end);
                                    line.setText(sb.toString());
                                    sb.setLength(0);
                                    if (debugLog)
                                        logger.info(CLASS_NAME + ".handleCaptionMessage(maxLineLength): speaker: " + speaker + ", start: " + line.getStart() + ", end: " + line.getEnd() + ", text: " + line.getText());
                                    line = new CaptionLine(language);
                                    line.setStart(CaptionHelper.epochInstantFromMillis((long) (item.getStart() * 1000) - messageOffset));
                                    lines.add(line);
                                }
                                else if (isText(item))
                                    sb.append(" ");
                            }
                            sb.append(item.getValue());
                        }
                        end = CaptionHelper.epochInstantFromMillis((long) (item.getEnd() * 1000) - messageOffset);
                        speakers.put(language, speaker);
                    }
                    text = sb.toString();
                    if (!text.isEmpty())
                    {
                        line.setEnd(end);
                        line.setText(text);
                        if (debugLog)
                            logger.info(CLASS_NAME + ".handleCaptionMessage(end): start: " + line.getStart() + ", end: " + line.getEnd() + ", text: " + line.getText());
                    }
                }
            }
        }
    }

    private boolean isText(Item item)
    {
        return item.getKind().equals("text");
    }
}
