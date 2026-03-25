package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.DayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AddUserAvailabilityRequest(
        @NotNull @Schema(description = "WEEKDAY or WEEKEND") DayType dayType,
        @NotNull @Schema(description = "Start time (must be before endTime)", example = "09:00") LocalTime startTime,
        @NotNull @Schema(description = "End time", example = "17:00") LocalTime endTime
) {
    @AssertTrue(message = "startTime must be before endTime")
    private boolean isValidTimeRange() {
        // null-guard needed because @AssertTrue may run before @NotNull
        if (startTime == null || endTime == null) return true;
        return startTime.isBefore(endTime);
    }
}