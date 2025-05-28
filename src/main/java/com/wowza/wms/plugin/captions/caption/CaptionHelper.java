/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.caption;

import com.wowza.util.StringUtils;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CaptionHelper
{
    final public static int defaultMaxLineLengthSBCS = 37;
    final public static int defaultMaxLineLengthMBCS = 30;
    static final long ticksPerMinute = 60 * 1000 * 10000;
    final private String language;
    final private String[] firstPassTerminators;
    final private String[] secondPassTerminators;

    final private int maxWidth;
    final private int maxHeight;
    private final String speakerChangeIndicator;
    private final CaptionTiming captionTiming;
    private final String text;
    private Optional<List<Caption>> captions = Optional.empty();

    private final int firstPassPercentage;
    public static final Instant dotNetEpoch = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0,
            ZoneOffset.UTC).toInstant();
    private int trackId = 99;

    public static Instant epochInstantFromTicks(BigInteger ticks)
    {
        // 10,000 ticks per millisecond.
        long ms = Math.floorDiv(ticks.longValue(), 10000);
        long restTicks = Math.floorMod(ticks.longValue(), 10000);
        long restNanos = restTicks * 100;
        return dotNetEpoch.plusMillis(ms).plusNanos(restNanos);
    }

    public static Instant epochInstantFromMillis(long milliseconds)
    {
        return dotNetEpoch.plusMillis(milliseconds);
    }

    public static List<Caption> getCaptions(String language, int maxLineLength, int maxLines, String lineTerminators,
                                            int firstPassPercentage, CaptionTiming captionTiming, String text)
    {
        return getCaptions(language, maxLineLength, maxLines, lineTerminators, firstPassPercentage, null, captionTiming,
                text);
    }

    public static List<Caption> getCaptions(String language, int maxLineLength, int maxLines, String lineTerminators,
                                            int firstPassPercentage, String speakerChangeIndicator, CaptionTiming captionTiming, String text)
    {
        CaptionHelper helper = new CaptionHelper(language, maxLineLength, maxLines, lineTerminators, firstPassPercentage, speakerChangeIndicator,
                captionTiming, text);
        return helper.getCaptions();
    }

    public CaptionHelper(String language, int maxWidth, int maxHeight, String lineTerminators, int firstPassPercentage,
                         String speakerChangeIndicator, CaptionTiming captionTiming, String text)
    {
        this.language = language;
        this.maxHeight = maxHeight;
        this.speakerChangeIndicator = speakerChangeIndicator;
        this.captionTiming = captionTiming;
        this.text = text;
		this.firstPassPercentage = firstPassPercentage;

		// consider adapting to use http://unicode.org/reports/tr29/#Sentence_Boundaries


        switch (language) {
            case "zh":
            case "ja":
                this.firstPassTerminators = new String[]{"，", "、", "；", "？", "！", "?", "!", ",", ";"};
                this.secondPassTerminators = new String[]{"。", " "};
                break;
            default:
                this.firstPassTerminators = Arrays.stream(lineTerminators.split("\\|"))
                        .filter(Predicate.not(String::isBlank)).toArray(String[]::new);
                this.secondPassTerminators = new String[]{" "};
                break;
        }

        if (maxWidth == defaultMaxLineLengthSBCS && (language.equals("zh") || language.equals("ja")))
        {
            this.maxWidth = defaultMaxLineLengthMBCS;
        }
        else
        {
            this.maxWidth = maxWidth;
        }
    }

    public static BigInteger getCalculatedDuration(String text, int wordsPerMinute)
    {
        int wordCount = text.split("\\s").length;
        return BigInteger.valueOf((wordCount * ticksPerMinute) / wordsPerMinute);
    }

    public List<Caption> getCaptions()
    {
        ensureCaptions();
        return captions.get();
    }

    private void ensureCaptions()
    {
        if (!captions.isPresent())
        {
            captions = Optional.of(new ArrayList<>());
            addCaptionsForText();
        }
    }
    
    private void addCaptionsForText()
    {
        int captionStartsAt = 0;
        List<String> captionLines = new ArrayList<>();

        int index = 0;
        while (index < text.length())
        {
            index = skipSkippable(index);

            int lineLength = getBestWidth(index);
            captionLines.add(text.substring(index, index + lineLength).trim());
            index = index + lineLength;
        }
        if (captionLines.size() <= maxHeight)
            addSingleCaption(captionLines, this.captionTiming.begin, this.captionTiming.end);
        else
            addMultipleCaptions(captionLines);
    }

    private void addMultipleCaptions(List<String> captionLines)
    {
        Duration perLineDuration = Duration.between(this.captionTiming.begin, this.captionTiming.end)
                .dividedBy(captionLines.size());
        AtomicReference<Instant> beginRef = new AtomicReference<>(this.captionTiming.begin);
        AtomicInteger counter = new AtomicInteger(0);
        captionLines.stream()
                .collect(Collectors.groupingBy(s -> counter.getAndIncrement() / maxHeight))
                .values()
                .forEach(lines -> {
                    Instant begin = beginRef.get();
                    Duration duration = perLineDuration.multipliedBy(lines.size());
                    Instant end = begin.plus(duration);
                    addSingleCaption(lines, begin, end);
                    beginRef.set(end);
                });
    }

    private void addSingleCaption(List<String> captionLines, Instant begin, Instant end)
    {
        String captionText = String.join("\n", captionLines);
        Caption caption = new Caption(language, begin, end, captionText, trackId);
        captions.get().add(caption);
    }

    private int getBestWidth(int startIndex)
    {
        if (!StringUtils.isEmpty(speakerChangeIndicator))
        {
            int speakerIndex = text.indexOf(speakerChangeIndicator, startIndex);
            if (speakerIndex != -1)
            {
                if (speakerIndex == startIndex)
                    speakerIndex = text.indexOf(speakerChangeIndicator, speakerIndex + speakerChangeIndicator.length());
                if (speakerIndex > startIndex)
                {
                    int width = speakerIndex - startIndex;
                    if (width <= maxWidth)
                        return width;
                }
            }
        }

        int remaining = text.length() - startIndex;
        if (remaining <= maxWidth)
            return remaining;
        int firstPass = findBestWidth(firstPassTerminators, startIndex);
        if (firstPass == maxWidth)
            return firstPass;
        int firstPassWordCount = getWordCount(startIndex, firstPass);
        int secondPass = findBestWidth(secondPassTerminators, startIndex);
        int secondPassWordCount = getWordCount(startIndex, secondPass);
        if (firstPassWordCount == 0 && secondPassWordCount == 0)
            return maxWidth;
        return (secondPassWordCount == 0 || (firstPassWordCount * 100.0f) / secondPassWordCount > firstPassPercentage) ? firstPass : secondPass;

    }

    private int getWordCount(int startIndex, int length)
    {
        if (length < 0)
            return 0;
        return text.substring(startIndex, startIndex + length).trim().split("\\s").length;
    }

    private int findBestWidth(String[] terminators, int startAt)
    {
        var remaining = text.length() - startAt;

        var bestWidth = -1;
        for (var terminator : terminators)
        {
            var checkChars = Math.min(remaining, maxWidth);
            if (terminator.endsWith(" "))
                checkChars += 1;
            // We need to get the last index of the terminator,
            // but only searching from startAt to startAt + checkChars.
            // So we take a substring of text from startAt to
            // startAt + checkChars.
            // Afterward, we need to re-add startAt to the resulting
            // index (which is from the substring) to align it with
            // the text from which the substring was taken.
            var index = text.substring(startAt, startAt + checkChars).lastIndexOf(terminator) + startAt;
            var width = index - startAt;
            if (width > bestWidth)
            {
                bestWidth = width + terminator.length();
            }
        }

        return bestWidth;
    }

    private int skipSkippable(int startIndex)
    {
        var index = startIndex;
        while (text.length() > index && text.charAt(index) == ' ')
        {
            index++;
        }

        return index;
    }
    
    private CaptionTiming getPartialResultCaptionTiming(int captionStartsAt, int captionLength)
    {
        var resultDuration = Duration.between(captionTiming.begin, captionTiming.end);
        var textLength = text.length();
        // TODO2 Consider something more precise than ms.
        var partialBegin = captionTiming.begin.plusMillis(resultDuration.toMillis() * captionStartsAt / textLength);
        var partialEnd = captionTiming.begin.plusMillis(resultDuration.toMillis() * (captionStartsAt + captionLength) / textLength);
        return new CaptionTiming(partialBegin, partialEnd);
    }
}
