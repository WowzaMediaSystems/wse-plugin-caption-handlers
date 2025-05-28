/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.transcoder;

import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.livetranscoder.ILiveStreamTranscoder;
import com.wowza.wms.stream.livetranscoder.LiveStreamTranscoderNotifyBase;
import com.wowza.wms.transcoder.model.LiveStreamTranscoder;

public class CaptionsTranscoderCreateListener extends LiveStreamTranscoderNotifyBase
{
    private final CaptionsTranscoderActionListener listener;

    public CaptionsTranscoderCreateListener(CaptionsTranscoderActionListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void onLiveStreamTranscoderCreate(ILiveStreamTranscoder transcoder, IMediaStream stream)
    {
        ((LiveStreamTranscoder)transcoder).addActionListener(listener);
    }
}
