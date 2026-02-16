package com.ua.estore.cgsWeb.models.dto.roadie;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Options {
    @JsonProperty("signature_required")
    private String signatureRequired;
    @JsonProperty("notifications_enabled")
    private String notificationsEnabled;
    @JsonProperty("over_21_required")
    private String over21Required;
    @JsonProperty("extra_compensation")
    private double extraCompensation;
}
