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

package com.wowza.wms.plugin.captions;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.timedtext.model.ITimedTextConstants;

public class ModuleCaptionsBase extends ModuleBase
{
    public static Class CLASS = ModuleCaptionsBase.class;
    public static String MODULE_NAME = CLASS.getSimpleName();
    public static final String MODULE_VERSION = ReleaseInfo.getVersion();
    public static final String DELAYED_STREAM_SUFFIX = "_delayed";
    public static final String RESAMPLED_STREAM_SUFFIX = "_resampled";
    public static final boolean DEFAULT_CAPTIONS_ENABLED = false;
    public static final String PROP_CAPTIONS_STREAM_DELAY = "captionHandlerStreamDelay";
    public static final String PROP_CAPTIONS_DEBUG_LOG = "captionHandlerDebug";
    public static final String PROP_DELAYED_STREAM_DEBUG_LOG = "captionHandlerDelayedStreamDebugLog";
    public static final String PROP_MAX_CAPTION_LINE_LENGTH = "captionHandlerMaxLineLength";
    public static final String PROP_MAX_CAPTION_LINE_COUNT = "captionHandlerMaxLines";
    public static final String PROP_LINE_TERMINATORS = "captionHandlerFirstPassTerminators";
    public static final String DEFAULT_FIRST_PASS_TERMINATORS = ". |?|!|,|;";
    public static final String PROP_FIRST_PASS_PERCENTAGE = "captionHandlerFirstPassPercentage";
    public static final int DEFAULT_FIRST_PASS_PERCENTAGE = 60;
    public static final String PROP_SPEAKER_CHANGE_INDICATOR = "captionHandlerSpeakerChangeIndicator";
    public static final String DEFAULT_SPEAKER_CHANGE_INDICATOR = ">>";
    public static final String PROP_NEW_LINE_THRESHOLD = "captionHandlerNewLineThreshold";
    public static final int DEFAULT_NEW_LINE_THRESHOLD = 250;

    protected WMSLogger logger;

    public void onAppCreate(IApplicationInstance appInstance)
    {
        logger = WMSLoggerFactory.getLoggerObj(CLASS, appInstance);
        String suffixes = appInstance.getProperties().getPropertyStr("dvrRecorderControlSuffixes");
        if (suffixes != null)
            appInstance.getProperties()
                    .setProperty("dvrRecorderControlSuffixes", suffixes + "," + DELAYED_STREAM_SUFFIX + "," + RESAMPLED_STREAM_SUFFIX);
    }
}
