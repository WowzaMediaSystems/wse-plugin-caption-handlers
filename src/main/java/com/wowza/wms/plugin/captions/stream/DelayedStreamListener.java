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

package com.wowza.wms.plugin.captions.stream;

import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.mediacaster.*;
import com.wowza.wms.stream.*;

import java.util.Map;
import java.util.concurrent.Executors;

import static com.wowza.wms.plugin.captions.ModuleCaptionsBase.DELAYED_STREAM_SUFFIX;

public class DelayedStreamListener implements IMediaStreamLivePacketNotify, MediaCasterNotify, StreamActionNotify
{
    protected final IApplicationInstance appInstance;
    protected final Map<String, DelayedStream> delayedStreams;

    public DelayedStreamListener(IApplicationInstance appInstance, Map<String, DelayedStream> delayedStreams)
    {
        this.appInstance = appInstance;
        this.delayedStreams = delayedStreams;
    }

    @Override
    public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
    {
        String mappedName  = streamName.replace(".stream", "");
        delayedStreams.computeIfPresent(mappedName, (k, delayedStream) -> {
            delayedStream.shutdown();
            return null;
        });
    }

    @Override
    public void onLivePacket(IMediaStream stream, AMFPacket packet)
    {
        String streamName = stream.getName();
        if (stream.isTranscodeResult() || streamName.endsWith(DELAYED_STREAM_SUFFIX))
            return;
        String mappedName = streamName.replace(".stream", "");
        DelayedStream delayedStream = delayedStreams.computeIfAbsent(mappedName,
                name -> new DelayedStream(appInstance, streamName, Executors.newSingleThreadScheduledExecutor()));
        delayedStream.writePacket(packet);
    }

    @Override
    public void onStreamStop(IMediaCaster mediaCaster)
    {
        String mappedName = mediaCaster.getMediaCasterId().replace(".stream", "");
        delayedStreams.computeIfPresent(mappedName, (k, delayedStream) -> {
            delayedStream.shutdown();
            return null;
        });
    }
}
