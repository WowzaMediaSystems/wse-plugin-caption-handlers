/*
 * WOWZA MEDIA SYSTEMS, LLC ("Wowza") CONFIDENTIAL
 *  © 2025 Wowza Media Systems, LLC. All rights reserved.
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

package com.wowza.wms.plugin.captions.whisper.model;

import com.wowza.wms.plugin.captions.caption.CaptionHelper;

import java.time.Instant;

public class CaptionLine
{
    private final String language;
    private String text;
    private Instant start = CaptionHelper.epochInstantFromMillis(0);
    private Instant end = CaptionHelper.epochInstantFromMillis(0);
    private final long timeAdded = System.currentTimeMillis();

    public CaptionLine(String language)
    {
        this.language = language;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setStart(Instant start)
    {
        this.start = start;
    }

    public void setEnd(Instant end)
    {
        this.end = end;
    }

    public String getText()
    {
        return text;
    }

    public Instant getStart()
    {
        return start;
    }

    public Instant getEnd()
    {
        return end;
    }

    public long getTimeAdded()
    {
        return timeAdded;
    }

    @Override
    public String toString()
    {
        return "CaptionLine{" +
               "language='" + language + '\'' +
               ", start=" + start +
               ", end=" + end +
               ", text='" + text + '\'' +
               '}';
    }
}
