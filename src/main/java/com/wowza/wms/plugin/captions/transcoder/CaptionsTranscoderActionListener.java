/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions.transcoder;

import com.wowza.wms.transcoder.model.*;
import com.wowza.wms.vhost.IVHost;

import java.util.regex.*;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.DELAYED_STREAM_SUFFIX;


public class CaptionsTranscoderActionListener extends LiveStreamTranscoderActionNotifyBase
{
    @Override
    public void onInitBeforeLoadTemplate(LiveStreamTranscoder transcoder)
    {
        if (transcoder.getStreamName().endsWith(DELAYED_STREAM_SUFFIX))
        {
            String regex = "\\$\\{SourceStreamName\\}";
            String templateName = transcoder.getTemplateName();
            String streamName = transcoder.getStreamName().replace(DELAYED_STREAM_SUFFIX, "");
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(templateName);
            String newTemplateName = matcher.replaceAll(streamName);
            transcoder.setTemplateName(newTemplateName);
        }
    }
}
