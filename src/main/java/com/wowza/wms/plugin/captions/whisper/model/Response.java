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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response
{
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("service_type")
    private String serviceType;

    @JsonProperty("language_code")
    private String languageCode;

    @JsonProperty("start")
    private float start;

    @JsonProperty("end")
    private float end;

    @JsonProperty("start_pts")
    private float startPts;

    @JsonProperty("start_epoch")
    private float startEpoch;

    @JsonProperty("is_final")
    private boolean isFinal;

    @JsonProperty("is_end_of_stream")
    private boolean isEndOfStream;

    @JsonProperty("speakers")
    private List<Speaker> speakers;

    @JsonProperty("alternatives")
    private List<Alternative> alternatives;

    public String getId()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public String getServiceType()
    {
        return serviceType;
    }

    public String getLanguageCode()
    {
        return languageCode;
    }

    public float getStart()
    {
        return start;
    }

    public float getEnd()
    {
        return end;
    }

    public float getStartPts()
    {
        return startPts;
    }

    public float getStartEpoch()
    {
        return startEpoch;
    }

    public boolean isFinal()
    {
        return isFinal;
    }

    public boolean isEndOfStream()
    {
        return isEndOfStream;
    }

    public List<Speaker> getSpeakers()
    {
        return speakers;
    }

    public List<Alternative> getAlternatives()
    {
        return alternatives;
    }

}