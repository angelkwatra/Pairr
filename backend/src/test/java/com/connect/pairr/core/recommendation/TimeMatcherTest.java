package com.connect.pairr.core.recommendation;

import com.connect.pairr.model.dto.TimeMatchResult;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.enums.ProficiencyLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeMatcherTest {

    private final TimeMatcher timeMatcher = new TimeMatcher();

    // --- Helper factories ---

    private UserAvailability slot(LocalTime start, LocalTime end) {
        UserAvailability ua = new UserAvailability();
        ua.setStartTime(start);
        ua.setEndTime(end);
        return ua;
    }

    private UserSkillAvailabilityData candidateSlot(LocalTime start, LocalTime end) {
        return new UserSkillAvailabilityData(
                null, null, ProficiencyLevel.INTERMEDIATE, null, null, 0L, start, end);
    }

    // --- Full overlap ---

    @Test
    void identicalWindows_returnsFullOverlap() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                List.of(slot(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                List.of(candidateSlot(LocalTime.of(9, 0), LocalTime.of(12, 0))));

        assertEquals(3 * 3600, result.bestOverlap());
        assertEquals(Long.MAX_VALUE, result.bestDistance());
    }

    // --- Partial overlap ---

    @Test
    void partialOverlap_returnsCorrectSeconds() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                List.of(slot(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                List.of(candidateSlot(LocalTime.of(11, 0), LocalTime.of(14, 0))));

        assertEquals(3600, result.bestOverlap()); // 11:00-12:00 = 1 hour
    }

    // --- No overlap, close windows ---

    @Test
    void noOverlap_returnsDistance() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                List.of(slot(LocalTime.of(9, 0), LocalTime.of(10, 0))),
                List.of(candidateSlot(LocalTime.of(11, 0), LocalTime.of(12, 0))));

        assertEquals(0, result.bestOverlap());
        assertEquals(3600, result.bestDistance()); // 10:00 to 11:00 = 1 hour gap
    }

    // --- Multiple slots, picks best overlap ---

    @Test
    void multipleSlots_picksBestOverlap() {
        List<UserAvailability> requester = List.of(
                slot(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                slot(LocalTime.of(14, 0), LocalTime.of(18, 0)));

        List<UserSkillAvailabilityData> candidate = List.of(
                candidateSlot(LocalTime.of(9, 30), LocalTime.of(10, 0)),   // 30 min overlap
                candidateSlot(LocalTime.of(15, 0), LocalTime.of(17, 0))); // 2 hour overlap

        TimeMatchResult result = timeMatcher.findBestMatch(requester, candidate);

        assertEquals(2 * 3600, result.bestOverlap()); // picks the 2-hour overlap
    }

    // --- Null/empty inputs ---

    @Test
    void nullRequesterSlots_returnsDefaults() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                null,
                List.of(candidateSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))));

        assertEquals(0, result.bestOverlap());
        assertEquals(Long.MAX_VALUE, result.bestDistance());
    }

    @Test
    void emptyCandidateSlots_returnsDefaults() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                List.of(slot(LocalTime.of(9, 0), LocalTime.of(10, 0))),
                Collections.emptyList());

        assertEquals(0, result.bestOverlap());
        assertEquals(Long.MAX_VALUE, result.bestDistance());
    }

    @Test
    void bothNull_returnsDefaults() {
        TimeMatchResult result = timeMatcher.findBestMatch(null, null);

        assertEquals(0, result.bestOverlap());
        assertEquals(Long.MAX_VALUE, result.bestDistance());
    }

    // --- Unsorted input still works ---

    @Test
    void unsortedInput_stillFindsOverlap() {
        // Deliberately out of order
        List<UserAvailability> requester = List.of(
                slot(LocalTime.of(14, 0), LocalTime.of(16, 0)),
                slot(LocalTime.of(9, 0), LocalTime.of(11, 0)));

        List<UserSkillAvailabilityData> candidate = List.of(
                candidateSlot(LocalTime.of(15, 0), LocalTime.of(18, 0)),
                candidateSlot(LocalTime.of(8, 0), LocalTime.of(10, 0)));

        TimeMatchResult result = timeMatcher.findBestMatch(requester, candidate);

        assertTrue(result.bestOverlap() > 0);
    }

    // --- Candidate before requester (reversed gap) ---

    @Test
    void candidateBeforeRequester_returnsDistance() {
        TimeMatchResult result = timeMatcher.findBestMatch(
                List.of(slot(LocalTime.of(14, 0), LocalTime.of(16, 0))),
                List.of(candidateSlot(LocalTime.of(10, 0), LocalTime.of(12, 0))));

        assertEquals(0, result.bestOverlap());
        assertEquals(2 * 3600, result.bestDistance()); // 12:00 to 14:00 = 2 hours
    }
}
