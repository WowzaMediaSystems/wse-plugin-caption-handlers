/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.audio;

import java.util.Locale;
import java.util.MissingResourceException;

public interface SpeechHandler extends Runnable, AutoCloseable
{
    void addAudioFrame(byte[] frame);

    void close();

    default Locale toLocale(String input)
    {
        if (input == null || input.isBlank()) return null;
        input = input.trim();

        // Case 1: 2-letter ISO language code
        if (input.length() == 2)
            return new Locale(input.toLowerCase());

        // Case 2: 3-letter ISO 639 code
        if (input.length() == 3)
        {
            for (Locale l : Locale.getAvailableLocales())
            {
                try
                {
                    if (l.getISO3Language().equalsIgnoreCase(input))
                        return new Locale(l.getLanguage());
                } catch (MissingResourceException ignored) {}
            }
        }

        // Case 3: Valid BCP 47 tag like "en-GB" or "zh-Hant-TW"
        Locale tagLocale = Locale.forLanguageTag(input);
        if (tagLocale.getLanguage().length() == 2)
            return tagLocale;

        // Otherwise, invalid input
        return null;
    }

    default boolean isBCP47WithRegion(String tag)
    {
        if (tag == null || tag.isBlank()) return false;

        Locale locale = Locale.forLanguageTag(tag);
        String lang = locale.getLanguage();
        String region = locale.getCountry();

        return lang.length() == 2 && region.length() == 2;
    }
}