package com.vamsi.screener.service.scoring;

import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.service.extraction.SkillDictionary;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Works out which skills a role actually requires.
 *
 * <p>Recruiters often paste a description without filling in a skills list, so the resolver falls
 * back to scanning the description text with the skill dictionary.
 */
@Component
public class JobSkillResolver {

    private final SkillDictionary skillDictionary;

    public JobSkillResolver(SkillDictionary skillDictionary) {
        this.skillDictionary = skillDictionary;
    }

    /**
     * @param job the role
     * @return explicitly listed required skills, or the skills mentioned in the description text
     */
    public Set<String> requiredSkills(JobDescription job) {
        Set<String> resolved = new LinkedHashSet<>();
        if (job == null) {
            return resolved;
        }
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            for (String raw : job.getRequiredSkills()) {
                String canonical = skillDictionary.canonicalize(raw);
                if (canonical != null && !canonical.isBlank()) {
                    resolved.add(canonical);
                }
            }
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return skillDictionary.extractFrom(job.getDescription());
    }
}
