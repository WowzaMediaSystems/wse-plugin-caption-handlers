/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.stream;

import com.wowza.util.FLVUtils;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.vhost.IVHost;

import java.util.Comparator;

public class AMFPacketComparator implements Comparator<AMFPacket>
{
    @Override
    public int compare(AMFPacket thisPacket, AMFPacket otherPacket)
    {
        if (thisPacket == otherPacket)
            return 0;
        int ret = 0;
        if (thisPacket.getAbsTimecode() == otherPacket.getAbsTimecode())
        {
            if (thisPacket.getType() != otherPacket.getType())
                ret = thisPacket.getType() == IVHost.CONTENTTYPE_VIDEO ? -1 : 1;
            else
                ret = (FLVUtils.isVideoCodecConfig(thisPacket) || FLVUtils.isAudioCodecConfig(thisPacket)) ? -1 : 1;
        }
        else
            ret = thisPacket.getAbsTimecode() < otherPacket.getAbsTimecode() ? -1 : 1;

        return ret;
    }
}
