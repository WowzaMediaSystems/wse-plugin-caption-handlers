/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2023 Wowza Media Systems, LLC. All rights reserved.
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

package com.wowza.wms.plugin.captions.azure;

import com.wowza.wms.plugin.captions.audio.SpeechHandler;
import com.wowza.wms.plugin.captions.caption.CaptionHandler;
import com.wowza.wms.plugin.captions.stream.DelayedStream;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.plugin.captions.transcoder.AudioResamplingTranscoderActionListener;

import java.util.Map;

public class AzureCaptionsTranscoderActionListener extends AudioResamplingTranscoderActionListener
{
    private final String subscriptionKey;
    private final String serviceRegion;

    public AzureCaptionsTranscoderActionListener(IApplicationInstance appInstance, Map<String, SpeechHandler> handlers, Map<String, DelayedStream> delayedStreams,
                                                 String subscriptionKey, String serviceRegion)
    {
        super(appInstance, handlers, delayedStreams);
        this.subscriptionKey = subscriptionKey;
        this.serviceRegion = serviceRegion;
    }

    @Override
    public SpeechHandler getSpeechHandler(CaptionHandler captionHandler)
    {
        return new AzureSpeechToTextHandler(appInstance, captionHandler, subscriptionKey, serviceRegion);
    }
}
