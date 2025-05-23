package com.wowza.wms.plugin.captions.whisper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wowza.wms.timedtext.model.ITimedTextConstants;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WhisperResponse
{
    @JsonProperty
    private String language = ITimedTextConstants.LANGUAGE_ID_ENGLISH;
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
