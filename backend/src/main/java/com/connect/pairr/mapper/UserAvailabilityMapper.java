package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.AddUserAvailabilityRequest;
import com.connect.pairr.model.dto.UserAvailabilityResponse;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.entity.UserAvailability;

public class UserAvailabilityMapper {

    public static UserAvailabilityResponse toResponse(UserAvailability ua) {
        return UserAvailabilityResponse.builder()
                .dayType(ua.getDayType())
                .startTime(ua.getStartTime())
                .endTime(ua.getEndTime())
                .build();
    }

    public static UserAvailability toEntity(AddUserAvailabilityRequest request, User user) {
        UserAvailability ua = new UserAvailability();
        ua.setUser(user);
        ua.setDayType(request.dayType());
        ua.setStartTime(request.startTime());
        ua.setEndTime(request.endTime());
        return ua;
    }
}
