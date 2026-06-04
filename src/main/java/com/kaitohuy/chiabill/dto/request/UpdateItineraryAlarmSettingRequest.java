package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateItineraryAlarmSettingRequest {
    @NotNull(message = "Alarm enabled field is required")
    private Boolean alarmEnabled;

    @NotNull(message = "Alarm value is required")
    private Integer alarmValue;

    @NotNull(message = "Alarm unit is required")
    private String alarmUnit;
}
