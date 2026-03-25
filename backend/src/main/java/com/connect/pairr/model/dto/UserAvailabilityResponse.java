package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.DayType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class UserAvailabilityResponse {
    private DayType dayType;
    private LocalTime startTime;
    private LocalTime endTime;
}
