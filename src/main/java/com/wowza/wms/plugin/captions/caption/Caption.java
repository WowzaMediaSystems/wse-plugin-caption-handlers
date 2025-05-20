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

package com.wowza.wms.plugin.captions.caption;//

import java.time.*;

import static com.wowza.wms.plugin.captions.caption.CaptionHelper.dotNetEpoch;

public class Caption
{
    private final String language;
    private final long begin;
    private final long end;
    private final String text;
    private final int trackId;

    public Caption(String language, Instant begin, Instant end, String text, int trackId)
    {
        this.language = language;
        this.begin = Duration.between(dotNetEpoch, begin).toMillis();
        this.end = Duration.between(dotNetEpoch, end).toMillis();
        this.text = text;
        this.trackId = trackId;
    }

    public String getLanguage()
    {
        return language;
    }

    public long getBegin()
    {
        return begin;
    }

    public long getEnd()
    {
        return end;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        return "Caption{" +
                "language='" + language + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                ", text='" + text.replace('\n', '|') + '\'' +
                '}';
    }

    public int getTrackId()
    {
        return trackId;
    }
}
