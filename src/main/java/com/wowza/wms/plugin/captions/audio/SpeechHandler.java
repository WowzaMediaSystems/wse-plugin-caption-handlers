/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.audio;

import com.wowza.wms.transcoder.model.TranscoderNativeAudioFrame;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

public interface SpeechHandler extends Runnable, AutoCloseable
{
    Map<String, String> DEFAULT_REGIONS = Map.ofEntries(
            Map.entry("en", "US"), Map.entry("eng", "US"),
            Map.entry("es", "ES"), Map.entry("spa", "ES"),
            Map.entry("fr", "FR"), Map.entry("fra", "FR"),
            Map.entry("de", "DE"), Map.entry("deu", "DE"),
            Map.entry("it", "IT"), Map.entry("ita", "IT"),
            Map.entry("pt", "BR"), Map.entry("por", "BR"),
            Map.entry("ru", "RU"), Map.entry("rus", "RU"),
            Map.entry("ja", "JP"), Map.entry("jpn", "JP"),
            Map.entry("ko", "KR"), Map.entry("kor", "KR"),
            Map.entry("zh", "CN"), Map.entry("zho", "CN"),
            Map.entry("ar", "SA"), Map.entry("ara", "SA"),
            Map.entry("hi", "IN"), Map.entry("hin", "IN"),
            Map.entry("nl", "NL"), Map.entry("nld", "NL"),
            Map.entry("pl", "PL"), Map.entry("pol", "PL"),
            Map.entry("tr", "TR"), Map.entry("tur", "TR"),
            Map.entry("sv", "SE"), Map.entry("swe", "SE"),
            Map.entry("da", "DK"), Map.entry("dan", "DK"),
            Map.entry("no", "NO"), Map.entry("nor", "NO"),
            Map.entry("fi", "FI"), Map.entry("fin", "FI"),
            Map.entry("cs", "CZ"), Map.entry("ces", "CZ"),
            Map.entry("el", "GR"), Map.entry("ell", "GR"),
            Map.entry("he", "IL"), Map.entry("heb", "IL"),
            Map.entry("th", "TH"), Map.entry("tha", "TH"),
            Map.entry("vi", "VN"), Map.entry("vie", "VN"),
            Map.entry("id", "ID"), Map.entry("ind", "ID"),
            Map.entry("uk", "UA"), Map.entry("ukr", "UA"),
            Map.entry("ro", "RO"), Map.entry("ron", "RO"),
            Map.entry("hu", "HU"), Map.entry("hun", "HU"),
            Map.entry("ca", "ES"), Map.entry("cat", "ES")
    );


   void addAudioFrame(TranscoderNativeAudioFrame frame);

    void close();

    default Locale toLocale(String input)
    {
        if (input == null || input.isBlank()) return null;
        input = input.trim();

        // Case 1: 2-letter ISO language code
        if (input.length() == 2)
            return new Locale(input.toLowerCase(), DEFAULT_REGIONS.getOrDefault(input.toLowerCase(), ""));

        // Case 2: 3-letter ISO 639 code
        if (input.length() == 3)
        {
            for (Locale l : Locale.getAvailableLocales())
            {
                try
                {
                    if (l.getISO3Language().equalsIgnoreCase(input))
                        return new Locale(l.getLanguage(), DEFAULT_REGIONS.getOrDefault(input.toLowerCase(), ""));
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

    /**
     * Converts a 2 or 3 letter language code to a BCP-47 tag with region.
     *
     * @param languageCode ISO 639-1 (2-letter) or ISO 639-2/3 (3-letter) code or BCP-47 tag
     * @return BCP-47 tag (e.g., "en-US")
     * @throws IllegalArgumentException if language code is unknown
     */
    default String toBCP47(String languageCode)
    {
        if (languageCode == null || languageCode.isEmpty())
        {
            throw new IllegalArgumentException("Language code cannot be null or empty");
        }

        // Check if it's already a BCP-47 tag (contains hyphen or underscore)
        if (languageCode.contains("-") || languageCode.contains("_"))
        {
            try
            {
                // Validate by parsing with Locale and return normalized form
                Locale locale = Locale.forLanguageTag(languageCode.replace("_", "-"));
                // Check if it has both language and country
                if (!locale.getLanguage().isEmpty() && !locale.getCountry().isEmpty())
                {
                    return locale.toLanguageTag();
                }
            } catch (Exception e)
            {
                // Fall through to treat as language code
            }
        }

        // Treat as a simple language code
        String region = DEFAULT_REGIONS.get(languageCode.toLowerCase());
        if (region == null)
        {
            throw new IllegalArgumentException("Unknown language code: " + languageCode);
        }

        return new Locale.Builder()
                .setLanguage(languageCode.toLowerCase())
                .setRegion(region)
                .build()
                .toLanguageTag();
    }
}