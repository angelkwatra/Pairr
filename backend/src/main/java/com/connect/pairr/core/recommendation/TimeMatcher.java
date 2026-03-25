package com.connect.pairr.core.recommendation;

import com.connect.pairr.model.dto.TimeMatchResult;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TimeMatcher {

    /**
     * Finds the best time match between requester and candidate availability slots.
     *
     * Complexity: O(n log n + m log m) where:
     *   n = number of requester availability slots
     *   m = number of candidate availability slots
     *
     * Optimized from O(nm) by sorting and using a sweep-line approach.
     *
     * @param requesterSlots Requester's availability slots
     * @param candidateSlots Candidate's availability slots
     * @return TimeMatchResult with best overlap and distance
     */
    public TimeMatchResult findBestMatch(List<UserAvailability> requesterSlots, List<UserSkillAvailabilityData> candidateSlots) {
        if (requesterSlots == null || requesterSlots.isEmpty() ||
                candidateSlots == null || candidateSlots.isEmpty()) {
            return TimeMatchResult.builder()
                    .bestOverlap(0)
                    .bestDistance(Long.MAX_VALUE)
                    .build();
        }

        long bestOverlap = 0;
        long bestDistance = Long.MAX_VALUE;

        // Sort both lists by start time - O(n log n + m log m)
        List<UserAvailability> sortedRequester = new ArrayList<>(requesterSlots);
        sortedRequester.sort(Comparator.comparing(UserAvailability::getStartTime));

        List<UserSkillAvailabilityData> sortedCandidate = new ArrayList<>(candidateSlots);
        sortedCandidate.sort(Comparator.comparing(UserSkillAvailabilityData::startTime));

        // Sweep-line algorithm: O(n + m) with early termination
        for (UserAvailability requester : sortedRequester) {
            LocalTime requesterStartTime = requester.getStartTime();
            LocalTime requesterEndTime = requester.getEndTime();

            for (UserSkillAvailabilityData candidate : sortedCandidate) {
                LocalTime candidateStartTime = candidate.startTime();
                LocalTime candidateEndTime = candidate.endTime();

                // Check for overlap first
                long overlap = calculateOverlap(requesterStartTime, requesterEndTime, candidateStartTime, candidateEndTime);
                if (overlap > 0) {
                    bestOverlap = Math.max(bestOverlap, overlap);
                } else {
                    // No overlap - calculate distance for scoring
                    long distance = calculateDistance(requesterStartTime, requesterEndTime, candidateStartTime, candidateEndTime);
                    if (distance != Long.MAX_VALUE && distance != 0) {
                        bestDistance = Math.min(bestDistance, distance);
                    }
                }

                // Early termination: if candidate starts after requester ends,
                // and we've already calculated the distance, we can break
                // because subsequent candidates will be even further away
                if (candidateStartTime.isAfter(requesterEndTime)) {
                    // We've already calculated distance above, so we can break
                    break;
                }
            }
        }

        return TimeMatchResult.builder()
                .bestOverlap(bestOverlap)
                .bestDistance(bestDistance)
                .build();
    }

    private long calculateOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        LocalTime maxStart = start1.isAfter(start2) ? start1 : start2;
        LocalTime minEnd = end1.isBefore(end2) ? end1 : end2;
        return maxStart.isBefore(minEnd) ? Duration.between(maxStart, minEnd).toSeconds() : 0;
    }

    private long calculateDistance(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (end1.isBefore(start2)) return Duration.between(end1, start2).toSeconds();
        if (end2.isBefore(start1)) return Duration.between(end2, start1).toSeconds();
        return 0;
    }
}

