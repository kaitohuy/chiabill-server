package com.kaitohuy.chiabill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryAlarmSettingResponse {
    private Boolean alarmEnabled;
    private Integer alarmValue;
    private String alarmUnit;
}
