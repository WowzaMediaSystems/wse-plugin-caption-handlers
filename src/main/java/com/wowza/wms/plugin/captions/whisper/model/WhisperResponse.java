/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.whisper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WhisperResponse
{
    @JsonProperty
    private String language = Locale.ENGLISH.getLanguage();
    @JsonProperty
    private String text;
    @JsonProperty
    private float start;
    @JsonProperty
    private float end;

    public String getLanguage()
    {
        return language;
    }

    public String getText()
    {
        return text;
    }

    public float getStart()
    {
        return start;
    }

    public float getEnd()
    {
        return end;
    }

    @Override
    public String toString()
    {
        return "WhisperResponse{" +
               "language='" + language + '\'' +
               ", text='" + text + '\'' +
               ", start=" + start +
               ", end=" + end +
               '}';
    }
}
