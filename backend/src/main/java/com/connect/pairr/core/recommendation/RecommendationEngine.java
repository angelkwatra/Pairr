package com.connect.pairr.core.recommendation;

import com.connect.pairr.model.dto.Recommendation;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.entity.UserSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final ScoreCalculator scoreCalculator;

    /**
     * Generates recommendations using a priority queue to efficiently find top N candidates.
     *
     * Uses a min-heap (PriorityQueue) to maintain only the top N recommendations,
     * avoiding the need to sort all candidates. This is especially beneficial when
     * there are many candidates (1000+) but only a few recommendations are needed.
     *
     * Complexity: O(n log k) where:
     *   n = number of candidates
     *   k = numberOfCandidates (typically 10-100)
     *
     * @param requesterAvailabilities Requester's availability slots
     * @param requesterUserSkill Requester's skill information
     * @param groupedCandidates Map of candidate IDs to their availability data
     * @param numberOfCandidates Maximum number of recommendations to return
     * @return List of top recommendations sorted by score (descending)
     */
    public List<Recommendation> recommend(
            List<UserAvailability> requesterAvailabilities,
            UserSkill requesterUserSkill,
            Map<UUID, List<UserSkillAvailabilityData>> groupedCandidates,
            int numberOfCandidates
    ) {
        if (requesterAvailabilities == null || requesterAvailabilities.isEmpty()
                || groupedCandidates == null || groupedCandidates.isEmpty()) {
            return List.of();
        }

        // Use min-heap to keep only top N candidates
        // Comparator: lower score = higher priority (so we can remove worst easily)
        PriorityQueue<Recommendation> topRecommendations = new PriorityQueue<>(
                numberOfCandidates + 1, // +1 to avoid resizing
                Comparator.comparingDouble(Recommendation::score) // Min-heap: smallest score at top
        );

        for (var entry : groupedCandidates.entrySet()) {
            UUID candidateId = entry.getKey();
            List<UserSkillAvailabilityData> candidateData = entry.getValue();
            if (candidateData == null || candidateData.isEmpty()) continue;

            UserSkillAvailabilityData sample = candidateData.get(0);

            double finalScore = scoreCalculator.computeFinalScore(
                    scoreCalculator.computeTimeScore(requesterAvailabilities, candidateData),
                    scoreCalculator.proficiencyScore(requesterUserSkill.getProficiency(), sample.proficiency()),
                    scoreCalculator.skillRatingScore(requesterUserSkill.getRating(), sample.rating()),
                    scoreCalculator.userRatingScore(requesterUserSkill.getUser().getOverallRating(), sample.overallRating()),
                    scoreCalculator.sessionCountScore(sample.completedSessionsCount())
            );

            Recommendation recommendation = Recommendation.builder()
                    .userId(candidateId)
                    .displayName(sample.displayName())
                    .score(finalScore)
                    .build();

            topRecommendations.offer(recommendation);

            // Keep only top N
            if (topRecommendations.size() > numberOfCandidates) {
                topRecommendations.poll(); // Remove worst
            }
        }

        return topRecommendations.stream()
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed())
                .toList();
    }
}
