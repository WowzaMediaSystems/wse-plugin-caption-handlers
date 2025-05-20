/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2022 Wowza Media Systems, LLC. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of Wowza Media Systems, LLC.
 * The intellectual and technical concepts contained herein are proprietary to Wowza Media Systems, LLC
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by trade secret
 * or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Wowza Media Systems, LLC. Access to the source code
 * contained herein is hereby forbidden to anyone except current Wowza Media Systems, LLC employees, managers
 * or contractors who have executed Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this
 * source code, which includes information that is confidential and/or proprietary, and is a trade secret, of
 * Wowza Media Systems, LLC. ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF WOWZA MEDIA SYSTEMS, LLC IS
 * STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION
 * OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR
 * DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.wowza.wms.plugin.captions.caption;

import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.*;
import com.wowza.wms.vhost.IVHost;

import java.time.*;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.PROP_CAPTIONS_DEBUG_LOG;
import static com.wowza.wms.plugin.captions.caption.CaptionHelper.dotNetEpoch;

public class DelayedStreamCaptionHandler implements CaptionHandler
{
    private static final Class<DelayedStreamCaptionHandler> CLASS = DelayedStreamCaptionHandler.class;
    private static final String CLASS_NAME = CLASS.getSimpleName();
    private static final int DEFAULT_WORDS_PER_MINUTE = 150;
    private final DelayedStream delayedStream;
    private final WMSLogger logger;
    private final boolean debugLog;

    private int wordsPerMinute = DEFAULT_WORDS_PER_MINUTE;

    public DelayedStreamCaptionHandler(IApplicationInstance appInstance, DelayedStream delayedStream)
    {
        this.delayedStream = delayedStream;
        logger = WMSLoggerFactory.getLoggerObj(DelayedStreamCaptionHandler.class, appInstance);
        debugLog = appInstance.getProperties().getPropertyBoolean(PROP_CAPTIONS_DEBUG_LOG, false);
    }

    @Override
    public void handleCaption(Caption caption)
    {
        if (debugLog)
            logger.info(CLASS_NAME + ".handleCaption: caption = " + caption);
        if (delayedStream == null)
            return;
        AMFDataObj amfData = new AMFDataObj();
        amfData.put("text", new AMFDataItem(caption.getText()));
        amfData.put("language", new AMFDataItem(caption.getLanguage()));
        amfData.put("trackid", new AMFDataItem(caption.getTrackId()));

        AMFDataList dataList = new AMFDataList();
        dataList.add(new AMFDataItem("onTextData"));
        dataList.add(amfData);
        byte[] data = dataList.serialize();

        long startOffset = delayedStream.getStartOffset();
        long captionOffset = caption.getBegin();
        AMFPacket packet = new AMFPacket(IVHost.CONTENTTYPE_DATA, 0, data);
        packet.setAbsTimecode(startOffset + captionOffset);
        if (debugLog)
            logger.info(CLASS_NAME + ".handleCaption: packet = " + packet);
        delayedStream.writePacket(packet);
    }

    @Override
    public int getWordsPerMinute()
    {
        return wordsPerMinute;
    }

    @Override
    public void setWordsPerMinute(int wordsPerMinute)
    {
        this.wordsPerMinute = wordsPerMinute;
    }

    public CaptionTiming getCaptionTiming()
    {
        long startOffset = delayedStream.getStartOffset();
        long firstTC = delayedStream.getFirstPacketTimecode();
        long lastTC = delayedStream.getLastPacketTimecode();
        Instant start = dotNetEpoch.plusMillis(firstTC - startOffset);
        Instant end = dotNetEpoch.plusMillis(lastTC - startOffset);
        return new CaptionTiming(start, end);
    }
}
