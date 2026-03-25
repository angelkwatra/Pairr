package com.connect.pairr.core.recommendation;

import com.connect.pairr.model.dto.TimeMatchResult;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.enums.ProficiencyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreCalculatorTest {

    @Mock
    private TimeMatcher timeMatcher;

    @InjectMocks
    private ScoreCalculator scoreCalculator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scoreCalculator, "maxOverlapHours", 4);
    }

    // --- proficiencyScore ---

    @Test
    void proficiencyScore_sameLevel_returns1() {
        assertEquals(1.0, scoreCalculator.proficiencyScore(
                ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.INTERMEDIATE));
    }

    @Test
    void proficiencyScore_beginnerVsExpert_returns0() {
        // diff = |1-4| = 3, maxDiff = 3, score = 1 - 3/3 = 0
        assertEquals(0.0, scoreCalculator.proficiencyScore(
                ProficiencyLevel.BEGINNER, ProficiencyLevel.EXPERT));
    }

    @Test
    void proficiencyScore_adjacentLevels() {
        // diff = |1-2| = 1, maxDiff = 3, score = 1 - 1/3 ≈ 0.667
        double score = scoreCalculator.proficiencyScore(
                ProficiencyLevel.BEGINNER, ProficiencyLevel.AMATEUR);
        assertEquals(0.667, score, 0.001);
    }

    // --- skillRatingScore ---

    @Test
    void skillRatingScore_sameRatings_returns1() {
        double score = scoreCalculator.skillRatingScore(
                BigDecimal.valueOf(4), BigDecimal.valueOf(4));
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void skillRatingScore_nullRequester_returns0() {
        assertEquals(0, scoreCalculator.skillRatingScore(null, BigDecimal.valueOf(4)));
    }

    @Test
    void skillRatingScore_nullCandidate_returns0() {
        assertEquals(0, scoreCalculator.skillRatingScore(BigDecimal.valueOf(4), null));
    }

    @Test
    void skillRatingScore_differentRatings() {
        // requester=5, candidate=1 → normalized: 1.0, 0.2 → 1 - |0.2 - 1.0| = 0.2
        double score = scoreCalculator.skillRatingScore(
                BigDecimal.valueOf(5), BigDecimal.valueOf(1));
        assertEquals(0.2, score, 0.001);
    }

    // --- userRatingScore ---

    @Test
    void userRatingScore_sameRatings_returns1() {
        double score = scoreCalculator.userRatingScore(
                BigDecimal.valueOf(3), BigDecimal.valueOf(3));
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void userRatingScore_nullRequester_returns0() {
        assertEquals(0, scoreCalculator.userRatingScore(null, BigDecimal.valueOf(3)));
    }

    @Test
    void userRatingScore_nullCandidate_returns0() {
        assertEquals(0, scoreCalculator.userRatingScore(BigDecimal.valueOf(3), null));
    }

    // --- computeTimeScore ---

    @Test
    void computeTimeScore_positiveOverlap_returnsCappedScore() {
        // 2 hours overlap, max 4 hours → 0.5
        when(timeMatcher.findBestMatch(anyList(), anyList()))
                .thenReturn(new TimeMatchResult(2 * 3600, Long.MAX_VALUE));

        double score = scoreCalculator.computeTimeScore(
                List.of(new UserAvailability()), List.of());
        assertEquals(0.5, score, 0.001);
    }

    @Test
    void computeTimeScore_overlapExceedsMax_cappedAt1() {
        // 6 hours overlap, max 4 hours → capped at 1.0
        when(timeMatcher.findBestMatch(anyList(), anyList()))
                .thenReturn(new TimeMatchResult(6 * 3600, Long.MAX_VALUE));

        double score = scoreCalculator.computeTimeScore(
                List.of(new UserAvailability()), List.of());
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void computeTimeScore_noOverlap_inverseDistance() {
        // No overlap, 1 hour distance → 1/(1+3600) ≈ 0.000278
        when(timeMatcher.findBestMatch(anyList(), anyList()))
                .thenReturn(new TimeMatchResult(0, 3600));

        double score = scoreCalculator.computeTimeScore(
                List.of(new UserAvailability()), List.of());
        assertTrue(score > 0 && score < 0.01);
    }

    @Test
    void computeTimeScore_maxValueDistance_returns0() {
        when(timeMatcher.findBestMatch(anyList(), anyList()))
                .thenReturn(new TimeMatchResult(0, Long.MAX_VALUE));

        double score = scoreCalculator.computeTimeScore(
                List.of(new UserAvailability()), List.of());
        assertEquals(0.0, score);
    }

    // --- sessionCountScore ---

    @Test
    void sessionCountScore_zero_returns0() {
        assertEquals(0.0, scoreCalculator.sessionCountScore(0L));
    }

    @Test
    void sessionCountScore_null_returns0() {
        assertEquals(0.0, scoreCalculator.sessionCountScore(null));
    }

    @Test
    void sessionCountScore_halfCap_returns05() {
        assertEquals(0.5, scoreCalculator.sessionCountScore(10L));
    }

    @Test
    void sessionCountScore_atCap_returns1() {
        assertEquals(1.0, scoreCalculator.sessionCountScore(20L));
    }

    @Test
    void sessionCountScore_exceedsCap_cappedAt1() {
        assertEquals(1.0, scoreCalculator.sessionCountScore(50L));
    }

    // --- computeTimeScore ---
    // ... existing tests ...

    // --- computeFinalScore ---

    @Test
    void computeFinalScore_allZeros_returns0() {
        assertEquals(0.0, scoreCalculator.computeFinalScore(0, 0, 0, 0, 0));
    }

    @Test
    void computeFinalScore_allOnes_returns1() {
        assertEquals(1.0, scoreCalculator.computeFinalScore(1, 1, 1, 1, 1));
    }

    @Test
    void computeFinalScore_verifyWeights() {
        // time=1.0, others=0.0 → 0.45*1 = 0.45
        assertEquals(0.45, scoreCalculator.computeFinalScore(1, 0, 0, 0, 0));

        // prof=1.0, others=0.0 → 0.20*1 = 0.20
        assertEquals(0.2, scoreCalculator.computeFinalScore(0, 1, 0, 0, 0));

        // skill=1.0, others=0.0 → 0.15*1 = 0.15
        assertEquals(0.15, scoreCalculator.computeFinalScore(0, 0, 1, 0, 0));

        // user=1.0, others=0.0 → 0.10*1 = 0.1
        assertEquals(0.1, scoreCalculator.computeFinalScore(0, 0, 0, 1, 0));

        // session=1.0, others=0.0 → 0.10*1 = 0.1
        assertEquals(0.1, scoreCalculator.computeFinalScore(0, 0, 0, 0, 1));
    }

    @Test
    void computeFinalScore_roundsToTwoDecimals() {
        // 0.45*0.3333 + 0.20*0.6667 + 0.15*0.5 + 0.10*0.8 + 0.10*0.5
        double score = scoreCalculator.computeFinalScore(0.3333, 0.6667, 0.5, 0.8, 0.5);
        assertEquals(score, Math.round(score * 100.0) / 100.0);
    }
}
