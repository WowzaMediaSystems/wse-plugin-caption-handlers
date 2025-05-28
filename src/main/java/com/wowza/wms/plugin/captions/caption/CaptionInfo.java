/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.caption;

import java.util.List;

public class CaptionInfo
{
    public long captionStart;
    public long streamBegin;
    public long streamEnd;
    public List<Caption> captions;
}
