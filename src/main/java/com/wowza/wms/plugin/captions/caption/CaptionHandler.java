/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.caption;

public interface CaptionHandler
{
    void handleCaption(Caption caption);

    int getWordsPerMinute();

    void setWordsPerMinute(int wordsPerMinute);

    CaptionTiming getCaptionTiming();

    default long getStartOffset()
    {
        return 0;
    }

    default String getStreamName()
    {
        return "unknown";
    }
}
