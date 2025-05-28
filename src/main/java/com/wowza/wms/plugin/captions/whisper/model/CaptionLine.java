/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.whisper.model;

import com.wowza.wms.plugin.captions.caption.CaptionHelper;

import java.time.Instant;

public class CaptionLine
{
    private final String language;
    private String text;
    private Instant start = CaptionHelper.epochInstantFromMillis(0);
    private Instant end = CaptionHelper.epochInstantFromMillis(0);
    private final long timeAdded = System.currentTimeMillis();

    public CaptionLine(String language)
    {
        this.language = language;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setStart(Instant start)
    {
        this.start = start;
    }

    public void setEnd(Instant end)
    {
        this.end = end;
    }

    public String getText()
    {
        return text;
    }

    public Instant getStart()
    {
        return start;
    }

    public Instant getEnd()
    {
        return end;
    }

    public long getTimeAdded()
    {
        return timeAdded;
    }

    @Override
    public String toString()
    {
        return "CaptionLine{" +
               "language='" + language + '\'' +
               ", start=" + start +
               ", end=" + end +
               ", text='" + text + '\'' +
               '}';
    }
}
