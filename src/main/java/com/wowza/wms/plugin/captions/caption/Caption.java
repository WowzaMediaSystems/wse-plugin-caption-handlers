/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.caption;//

import java.time.*;

import static com.wowza.wms.plugin.captions.caption.CaptionHelper.dotNetEpoch;

public class Caption
{
    private final String language;
    private final long begin;
    private final long end;
    private final String text;
    private final int trackId;

    public Caption(String language, Instant begin, Instant end, String text, int trackId)
    {
        this.language = language;
        this.begin = Duration.between(dotNetEpoch, begin).toMillis();
        this.end = Duration.between(dotNetEpoch, end).toMillis();
        this.text = text;
        this.trackId = trackId;
    }

    public String getLanguage()
    {
        return language;
    }

    public long getBegin()
    {
        return begin;
    }

    public long getEnd()
    {
        return end;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        return "Caption{" +
                "language='" + language + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                ", text='" + text.replace('\n', '|') + '\'' +
                '}';
    }

    public int getTrackId()
    {
        return trackId;
    }
}
