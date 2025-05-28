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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import com.wowza.wms.plugin.captions.whisper.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
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

    private final WMSLogger logger;
    private final CaptionHandler captionHandler;
    private Socket socket;
    private SocketListener socketListener;

    private final boolean debugLog;
    private final int maxLineLength;
    private final int maxLineCount;
    private final String socketHost;
    private final int socketPort;

    private final IApplicationInstance appInstance;
    private final int newLineThreshold;
    private final long delay;

    private volatile boolean doQuit = false;
    private volatile boolean outputRunning = false;
    private final String[] lineTerminators;

    private Thread runningThread;

//    private WebSocket websocket;
    private int retryCount = 0;

    public WhisperSpeechToTextHandler(IApplicationInstance appInstance, CaptionHandler captionHandler)
    {
        this.appInstance = appInstance;
        this.logger = WMSLoggerFactory.getLoggerObj(appInstance);
        this.captionHandler = captionHandler;
        WMSProperties props = appInstance.getProperties();
        this.debugLog = props.getPropertyBoolean(PROP_CAPTIONS_DEBUG_LOG, false);
        this.maxLineLength = props.getPropertyInt(PROP_MAX_CAPTION_LINE_LENGTH, CaptionHelper.defaultMaxLineLengthSBCS);
        this.maxLineCount = props.getPropertyInt(PROP_MAX_CAPTION_LINE_COUNT, 2);
        this.lineTerminators = props.getPropertyStr(PROP_LINE_TERMINATORS, DEFAULT_FIRST_PASS_TERMINATORS).split("\\|");
        this.newLineThreshold = props.getPropertyInt(PROP_NEW_LINE_THRESHOLD, DEFAULT_NEW_LINE_THRESHOLD);
        this.delay = props.getPropertyLong(PROP_CAPTIONS_STREAM_DELAY, DEFAULT_START_DELAY);
        this.socketHost = props.getPropertyStr("whisperSocketHost", "localhost");
        this.socketPort = props.getPropertyInt("whisperSocketPort", 3000);
        try
        {
            this.socket = new Socket(socketHost, socketPort);
            this.socketListener = new SocketListener();
            new Thread(socketListener, CLASS_NAME + ".SocketListener").start();
        }
        catch (IOException e)
        {
            socket = null;
            logger.error(CLASS_NAME + " error creating Whisper socket: " + e, e);
        }
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
        int maxRetries = 5;
        while (retryCount < maxRetries)
        {
            try
            {
                logger.info(CLASS_NAME + ".Socket.reconnect: Attempting to reconnect...");
                if (socket != null && !socket.isClosed())
                {
                    socket.close();
                }
                socket = new Socket(socketHost, socketPort);
                socketListener = new SocketListener();
                new Thread(socketListener, CLASS_NAME + ".SocketListener").start();
                break; // Exit the method if reconnection is successful
            }
            catch (Exception e)
            {
                socket = null;
                retryCount++;
                if (retryCount >= maxRetries) {
                    logger.error(CLASS_NAME + ".Socket.reconnect: Failed to reconnect after " + maxRetries + " attempts", e);
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
                ByteBuffer frame = audioBuffer.poll(100, TimeUnit.MILLISECONDS);
                if (frame == null)
                    continue;
                if (socket != null && socket.isConnected())
                {
                    socket.getOutputStream().write(frame.array());
                    socket.getOutputStream().flush();
                }
                else
                {
                    logger.error(CLASS_NAME + ".run(): Socket is not connected");
                    reconnect();
                }
            }
            catch (Exception e)
            {
                if (doQuit)
                    break;
                reconnect();
                if (socket == null)
                    break;
            }
        }
    }

    private void processPendingCaptions()
    {
        try
        {
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
                    if (doQuit || lines.size() > maxLineCount || (!lines.isEmpty() && lines.peekLast().getTimeAdded() < System.currentTimeMillis() - delay / 2))
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
        if (socket != null)
        {
            try
            {
                socket.shutdownOutput();
            }
            catch (IOException e)
            {
                logger.error(CLASS_NAME + ".close: Error closing socket: " + e, e);
            }
        }
    }

    private void handleWhisperResponse(WhisperResponse response)
    {
        if (debugLog)
            logger.info(CLASS_NAME + ".handleWhisperResponse: response: " + response);
        String language = response.getLanguage();
        LinkedList<CaptionLine> lines = captionLines.computeIfAbsent(language, k -> new LinkedList<>());
        synchronized (lines)
        {
            String text = response.getText();
            if (!StringUtils.isEmpty(text))
            {
                Instant start = CaptionHelper.epochInstantFromMillis((long) (response.getStart() * 1000));
                Instant end = CaptionHelper.epochInstantFromMillis((long) (response.getEnd() * 1000));

                StringBuilder sb = new StringBuilder();
                CaptionLine line = lines.peekLast();
                if (line != null && Duration.between(line.getEnd(), start).toMillis() < newLineThreshold)
                {
                    sb.append(line.getText());
                }
                else
                {
                    line = new CaptionLine(language);
                    line.setStart(start);
                    lines.add(line);
                }

                List<String> items = Arrays.stream(text.split("\\s+")).toList();
                if (debugLog)
                    logger.info(CLASS_NAME + ".handleCaptionMessage: items: " + items);
                float duration = response.getEnd() - response.getStart();
                float perWordDuration = duration / items.size();
                for (int i = 0; i < items.size(); i++)
                {
                    String item = items.get(i);
                    float itemStart = response.getStart() + (i * perWordDuration);
                    float itemEnd = itemStart + perWordDuration;
                    int length = sb.length();
                    if (length > 0)
                    {
                        // check if the text length + preceding space will exceed the max line length. If so, create a new line
                        if (length + 1 + item.length() > maxLineLength)
                        {
                            line.setEnd(end);
                            line.setText(sb.toString());
                            sb.setLength(0);
                            if (debugLog)
                                logger.info(CLASS_NAME + ".handleCaptionMessage(maxLineLength): start: " + line.getStart() + ", end: " + line.getEnd() + ", text: " + line.getText());
                            line = new CaptionLine(language);
                            line.setStart(CaptionHelper.epochInstantFromMillis((long) (itemStart * 1000)));
                            lines.add(line);
                        }
                        else
                            sb.append(" ");
                    }
                    sb.append(item);
                    end = CaptionHelper.epochInstantFromMillis((long) (itemEnd * 1000));
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

    private class SocketListener implements Runnable
    {
        @Override
        public void run()
        {
            try (InputStream inputStream = socket.getInputStream())
            {
                parseJsonStream(inputStream);
                doQuit = true;
                processPendingCaptions();
            }
            catch (SocketException s)
            {
                logger.info(CLASS_NAME + ".SocketListener.run: SocketException: " + s);
            }
            catch (IOException e)
            {
                logger.error(CLASS_NAME + ".SocketListener.run exception: e", e);
            }
        }

        private void parseJsonStream(InputStream inputStream) throws IOException
        {
            JsonFactory factory = new JsonFactory();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonParser parser = factory.createParser(inputStream);

            while (!parser.isClosed())
            {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.START_OBJECT)
                {
                    // Deserialize the JSON object into a POJO
                    WhisperResponse response = objectMapper.readValue(parser, WhisperResponse.class);
                    handleWhisperResponse(response);
                }
            }
            logger.info(CLASS_NAME + ".parseJsonStream: end");
        }
    }
}
