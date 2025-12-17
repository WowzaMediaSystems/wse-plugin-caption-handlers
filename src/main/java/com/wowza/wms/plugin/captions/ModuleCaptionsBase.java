/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.captions;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.ModuleBase;

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
