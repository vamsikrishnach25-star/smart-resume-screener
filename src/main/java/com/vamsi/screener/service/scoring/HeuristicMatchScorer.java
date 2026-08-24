package com.vamsi.screener.service.scoring;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Resume;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic scorer used when the LLM is unavailable.
 *
 * <p>It is intentionally simple and explainable: weighted skill coverage, an experience ratio and a
 * light education signal. Because it is pure arithmetic it is reproducible, instant and free, which
 * makes it a good baseline to compare the model's judgement against.
 */
@Component
public class HeuristicMatchScorer {

    private static final String[] DEGREE_TOKENS = {
            "bachelor", "b.tech", "btech", "b.e", "bsc", "b.sc", "master", "m.tech", "mtech",
            "msc", "m.sc", "mba", "phd", "degree", "graduate"
    };

    private final JobSkillResolver jobSkillResolver;
    private final AppProperties.Scoring config;

    public HeuristicMatchScorer(JobSkillResolver jobSkillResolver, AppProperties properties) {
        this.jobSkillResolver = jobSkillResolver;
        this.config = properties.getScoring();
    }

    /**
     * Scores one candidate against one role.
     *
     * @param resume the parsed candidate
     * @param job    the role
     * @return an assessment tagged with {@link AnalysisSource#HEURISTIC}
     */
    public MatchAssessment score(Resume resume, JobDescription job) {
        Set<String> requiredSkills = jobSkillResolver.requiredSkills(job);
        Map<String, String> candidateSkills = indexByLowerCase(resume.getSkills());

        Set<String> matched = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();
        for (String required : requiredSkills) {
            if (candidateSkills.containsKey(required.toLowerCase(Locale.ROOT))) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }

        double skillScore = skillScore(requiredSkills.size(), matched.size());
        double experienceScore = experienceScore(resume.getYearsOfExperience(), job.getMinYearsExperience());
        double educationScore = educationScore(resume, job);

        double overall = skillScore * config.getSkillWeight()
                + experienceScore * config.getExperienceWeight()
                + educationScore * config.getEducationWeight();

        String justification = buildJustification(
                resume, job, requiredSkills, matched, missing, experienceScore);

        return MatchAssessment.of(overall, skillScore, experienceScore, educationScore,
                matched, missing, justification, AnalysisSource.HEURISTIC);
    }

    private double skillScore(int requiredCount, int matchedCount) {
        if (requiredCount == 0) {
            return 5.0D;
        }
        double coverage = (double) matchedCount / (double) requiredCount;
        return 1.0D + 9.0D * coverage;
    }

    private double experienceScore(Double candidateYears, Double requiredYears) {
        if (candidateYears == null) {
            return 5.0D;
        }
        if (requiredYears == null || requiredYears <= 0.0D) {
            return candidateYears >= 3.0D ? 8.0D : 6.5D;
        }
        double ratio = candidateYears / requiredYears;
        if (ratio >= 1.0D) {
            return Math.min(10.0D, 8.5D + (ratio - 1.0D) * 1.5D);
        }
        return Math.max(1.0D, 9.0D * ratio);
    }

    private double educationScore(Resume resume, JobDescription job) {
        boolean jobAsksForDegree = containsDegreeToken(job.getDescription());
        boolean candidateHasDegree =
                (resume.getHighestEducation() != null && !resume.getHighestEducation().isBlank())
                        || !resume.getEducations().isEmpty();

        if (!jobAsksForDegree) {
            return candidateHasDegree ? 8.0D : 7.0D;
        }
        return candidateHasDegree ? 9.0D : 4.5D;
    }

    private boolean containsDegreeToken(String description) {
        if (description == null) {
            return false;
        }
        String lower = description.toLowerCase(Locale.ROOT);
        for (String token : DEGREE_TOKENS) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String buildJustification(Resume resume,
                                      JobDescription job,
                                      Set<String> requiredSkills,
                                      Set<String> matched,
                                      Set<String> missing,
                                      double experienceScore) {
        StringBuilder text = new StringBuilder();
        text.append("Rule-based assessment: ")
                .append(matched.size()).append(" of ").append(requiredSkills.size())
                .append(" required skills evidenced");
        if (!matched.isEmpty()) {
            text.append(" (").append(String.join(", ", limit(matched))).append(")");
        }
        text.append(". ");

        if (!missing.isEmpty()) {
            text.append("Not evidenced: ").append(String.join(", ", limit(missing))).append(". ");
        }

        Double years = resume.getYearsOfExperience();
        Double required = job.getMinYearsExperience();
        if (years == null) {
            text.append("Total years of experience could not be determined from the resume. ");
        } else if (required != null && required > 0.0D) {
            text.append(String.format(Locale.ROOT, "%.1f years of experience against %.1f required. ",
                    years, required));
        } else {
            text.append(String.format(Locale.ROOT, "%.1f years of experience. ", years));
        }

        if (experienceScore < 5.0D) {
            text.append("Experience is below what the role asks for. ");
        }
        text.append("Scored without the language model, so no semantic judgement was applied.");
        return text.toString();
    }

    private Set<String> limit(Set<String> skills) {
        Set<String> limited = new LinkedHashSet<>();
        for (String skill : skills) {
            if (limited.size() >= 8) {
                break;
            }
            limited.add(skill);
        }
        return limited;
    }

    private Map<String, String> indexByLowerCase(Set<String> skills) {
        Map<String, String> index = new LinkedHashMap<>();
        if (skills == null) {
            return index;
        }
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                index.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
            }
        }
        return index;
    }
}
