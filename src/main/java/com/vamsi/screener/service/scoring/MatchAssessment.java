package com.vamsi.screener.service.scoring;

import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.Recommendation;
import com.vamsi.screener.util.TextUtils;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * How well one candidate fits one role, as produced by either scoring engine.
 *
 * @param matchScore      overall fit, 1-10
 * @param skillScore      coverage of the required stack, 1-10
 * @param experienceScore depth and relevance of experience, 1-10
 * @param educationScore  education fit, 1-10
 * @param matchedSkills   required skills the candidate evidences
 * @param missingSkills   required skills not found
 * @param justification   short explanation aimed at a hiring manager
 * @param recommendation  band derived from {@code matchScore}
 * @param source          which engine produced the assessment
 */
public record MatchAssessment(
        double matchScore,
        double skillScore,
        double experienceScore,
        double educationScore,
        Set<String> matchedSkills,
        Set<String> missingSkills,
        String justification,
        Recommendation recommendation,
        AnalysisSource source) {

    /**
     * Builds an assessment with every score clamped to the 1-10 band and the recommendation derived
     * from the overall score, so both engines always produce comparable output.
     */
    public static MatchAssessment of(double matchScore,
                                     double skillScore,
                                     double experienceScore,
                                     double educationScore,
                                     Set<String> matchedSkills,
                                     Set<String> missingSkills,
                                     String justification,
                                     AnalysisSource source) {
        double overall = TextUtils.clampScore(matchScore);
        return new MatchAssessment(
                overall,
                TextUtils.clampScore(skillScore),
                TextUtils.clampScore(experienceScore),
                TextUtils.clampScore(educationScore),
                matchedSkills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(matchedSkills),
                missingSkills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(missingSkills),
                justification,
                Recommendation.fromScore(overall),
                source);
    }
}
