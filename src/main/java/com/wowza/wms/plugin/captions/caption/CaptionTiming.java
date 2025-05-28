/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.caption;//

import java.time.Instant;

public class CaptionTiming
{
    public Instant begin;
    public Instant end;
    
    public CaptionTiming(Instant begin, Instant end)
    {
        this.begin = begin;
        this.end = end;
    }
}
