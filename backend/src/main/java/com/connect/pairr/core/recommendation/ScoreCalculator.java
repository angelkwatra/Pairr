package com.connect.pairr.core.recommendation;

import com.connect.pairr.model.dto.TimeMatchResult;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.enums.ProficiencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    private final TimeMatcher timeMatcher;

    @Value("${recommendation.time-matching.max-overlap-hours:4}")
    private int maxOverlapHours;

    // Weights
    private static final double TIME_WEIGHT = 0.45; // reduced from 0.5
    private static final double PROFICIENCY_WEIGHT = 0.20; // reduced from 0.25
    private static final double SKILL_RATING_WEIGHT = 0.15;
    private static final double USER_RATING_WEIGHT = 0.10;
    private static final double SESSION_COUNT_WEIGHT = 0.10; // New factor

    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SESSION_COUNT_CAP = 20; // Experience beyond 20 sessions has diminishing returns for the score


    // Compute proficiency similarity (1 = same, 0 = max difference)
    public double proficiencyScore(ProficiencyLevel requester, ProficiencyLevel candidate) {
        int diff = Math.abs(requester.getLevel() - candidate.getLevel());
        int maxDiff = ProficiencyLevel.values().length - 1;
        return 1.0 - ((double) diff / maxDiff);
    }

    // Compute session count score (experience)
    public double sessionCountScore(Long completedSessions) {
        if (completedSessions == null || completedSessions == 0) return 0;
        return Math.min((double) completedSessions / SESSION_COUNT_CAP, 1.0);
    }

    // Compute skill rating similarity (1 = same, 0 = max difference)
    public double skillRatingScore(BigDecimal requesterRating, BigDecimal candidateRating) {
        if (requesterRating == null || candidateRating == null) return 0;
        return 1 - Math.abs(normalize(candidateRating) - normalize(requesterRating));
    }

    // Compute user rating score
    public double userRatingScore(BigDecimal requesterRating, BigDecimal candidateRating) {
        if (requesterRating == null || candidateRating == null) return 0;
        return 1 - Math.abs(normalize(candidateRating) - normalize(requesterRating));
    }

    // Compute time score
    public double computeTimeScore(List<UserAvailability> requesterSlots, List<UserSkillAvailabilityData> candidateSlots) {
        TimeMatchResult timeMatchResult = timeMatcher.findBestMatch(requesterSlots, candidateSlots);
        if (timeMatchResult.bestOverlap() > 0) {
            double maxOverlapSeconds = maxOverlapHours * SECONDS_PER_HOUR;
            return Math.min(timeMatchResult.bestOverlap() / maxOverlapSeconds, 1.0);
        }
        if (timeMatchResult.bestDistance() == Long.MAX_VALUE) return 0;
        return 1.0 / (1 + timeMatchResult.bestDistance());
    }


    // Final score combining all factors
    public double computeFinalScore(double timeScore, double proficiencyScore, double skillScore, double userRatingScore, double sessionScore) {
        double raw = (timeScore * TIME_WEIGHT) +
                (proficiencyScore * PROFICIENCY_WEIGHT) +
                (skillScore * SKILL_RATING_WEIGHT) +
                (userRatingScore * USER_RATING_WEIGHT) +
                (sessionScore * SESSION_COUNT_WEIGHT);
        return Math.round(raw * 100.0) / 100.0;
    }

    // private helper
    private double normalize(BigDecimal value) {
        if (value == null) return 0;
        return value.divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP).doubleValue();
    }
}
