package com.vamsi.screener.service.extraction;

import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.EducationEntry;
import com.vamsi.screener.model.ExperienceEntry;
import com.vamsi.screener.util.TextUtils;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Rule-based resume extractor.
 *
 * <p>This is the safety net: it runs when no LLM key is configured, when the model call fails, and
 * in tests. It is deliberately conservative - it prefers returning {@code null} over guessing - and
 * it is fully deterministic, which makes the whole pipeline unit-testable without network access.
 */
@Component
public class HeuristicResumeExtractor {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_CANDIDATE_PATTERN =
            Pattern.compile("(?:\\+?\\d[\\d\\s().-]{7,}\\d)");
    private static final Pattern STATED_YEARS_PATTERN = Pattern.compile(
            "(\\d{1,2}(?:\\.\\d)?)\\s*\\+?\\s*(?:years?|yrs?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOUR_DIGIT_YEAR_PATTERN = Pattern.compile("\\b(19[89]\\d|20[0-4]\\d)\\b");
    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "((?:19|20)\\d{2}\\s*(?:-|to|until)\\s*(?:(?:19|20)\\d{2}|present|current|now))",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_EXPERIENCE_ENTRIES = 12;
    private static final int MAX_EDUCATION_ENTRIES = 6;

    /** Degree labels ordered from highest to lowest, with the tokens that identify them. */
    private static final List<DegreeRule> DEGREE_RULES = List.of(
            new DegreeRule("PhD", List.of("ph.d", "phd", "doctorate", "doctoral")),
            new DegreeRule("Master's degree", List.of("master", "m.tech", "mtech", "m.sc", "msc", "m.s.",
                    "mba", "m.c.a", "mca", "post graduate", "postgraduate")),
            new DegreeRule("Bachelor's degree", List.of("bachelor", "b.tech", "btech", "b.e.", "b.e ",
                    "be(", "b.sc", "bsc", "b.s.", "b.c.a", "bca", "b.com", "undergraduate")),
            new DegreeRule("Diploma", List.of("diploma", "polytechnic")),
            new DegreeRule("High school", List.of("high school", "higher secondary", "intermediate", "12th")));

    private final SkillDictionary skillDictionary;

    public HeuristicResumeExtractor(SkillDictionary skillDictionary) {
        this.skillDictionary = skillDictionary;
    }

    /**
     * Extracts structured data from resume text using regular expressions, section detection and
     * the skill dictionary.
     *
     * @param resumeText normalised resume text
     * @return extracted data, always with {@link AnalysisSource#HEURISTIC}
     */
    public ExtractedResumeData extract(String resumeText) {
        String text = resumeText == null ? "" : resumeText;
        Map<Section, String> sections = splitIntoSections(text);

        Set<String> skills = collectSkills(text, sections.get(Section.SKILLS));
        List<EducationEntry> educations = extractEducation(sections.getOrDefault(Section.EDUCATION, ""));

        return new ExtractedResumeData(
                extractName(text),
                extractEmail(text),
                extractPhone(text),
                null,
                extractSummary(sections.get(Section.SUMMARY)),
                extractYearsOfExperience(text, sections.get(Section.EXPERIENCE)),
                highestEducationLabel(educations),
                skills,
                extractExperience(sections.getOrDefault(Section.EXPERIENCE, "")),
                educations,
                AnalysisSource.HEURISTIC);
    }

    // ------------------------------------------------------------------ contact details

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_CANDIDATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            String digits = candidate.replaceAll("\\D", "");
            if (digits.length() >= 10 && digits.length() <= 15) {
                return candidate.length() > 40 ? candidate.substring(0, 40) : candidate;
            }
        }
        return null;
    }

    /**
     * Picks the candidate name from the resume header: the first short line that reads like a
     * person's name rather than a heading or a contact line.
     */
    private String extractName(String text) {
        String[] lines = text.split("\n");
        int inspected = 0;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (++inspected > 6) {
                break;
            }
            if (!looksLikeName(line)) {
                continue;
            }
            return toDisplayCase(line);
        }
        return null;
    }

    private boolean looksLikeName(String line) {
        if (line.length() > 60 || line.length() < 3) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("resume") || lower.contains("curriculum vitae") || lower.startsWith("cv")) {
            return false;
        }
        if (line.matches(".*[0-9@|:/\\\\].*")) {
            return false;
        }
        String[] words = line.split("\\s+");
        if (words.length < 2 || words.length > 4) {
            return false;
        }
        for (String word : words) {
            if (!word.matches("[A-Za-z][A-Za-z.'-]*")) {
                return false;
            }
        }
        return true;
    }

    private String toDisplayCase(String line) {
        if (!line.equals(line.toUpperCase(Locale.ROOT))) {
            return line;
        }
        StringBuilder builder = new StringBuilder();
        for (String word : line.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    // ------------------------------------------------------------------ skills

    private Set<String> collectSkills(String fullText, String skillsSection) {
        Set<String> skills = new LinkedHashSet<>(skillDictionary.extractFrom(fullText));
        if (skillsSection == null || skillsSection.isBlank()) {
            return skills;
        }
        for (String token : skillsSection.split("[,;|/\\n\\u2022\\u00b7]")) {
            String cleaned = token.replaceAll("^[\\s\\-*:]+", "").replaceAll("[\\s.:]+$", "").trim();
            if (cleaned.length() < 2 || cleaned.length() > 40) {
                continue;
            }
            if (cleaned.split("\\s+").length > 4 || cleaned.endsWith(":")) {
                continue;
            }
            String canonical = skillDictionary.canonicalize(cleaned);
            if (canonical != null) {
                skills.add(canonical);
            }
        }
        return skills;
    }

    // ------------------------------------------------------------------ experience

    private Double extractYearsOfExperience(String fullText, String experienceSection) {
        double statedMax = 0.0D;
        Matcher matcher = STATED_YEARS_PATTERN.matcher(fullText);
        while (matcher.find()) {
            try {
                statedMax = Math.max(statedMax, Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // a malformed number simply does not contribute
            }
        }
        if (statedMax > 0.0D && statedMax <= 50.0D) {
            return statedMax;
        }

        String source = experienceSection == null || experienceSection.isBlank() ? fullText : experienceSection;
        int currentYear = Year.now().getValue();
        int earliest = Integer.MAX_VALUE;
        int latest = Integer.MIN_VALUE;
        Matcher yearMatcher = FOUR_DIGIT_YEAR_PATTERN.matcher(source);
        while (yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            earliest = Math.min(earliest, year);
            latest = Math.max(latest, year);
        }
        if (earliest == Integer.MAX_VALUE) {
            return null;
        }
        if (source.toLowerCase(Locale.ROOT).contains("present")
                || source.toLowerCase(Locale.ROOT).contains("current")) {
            latest = currentYear;
        }
        double span = latest - earliest;
        if (span <= 0.0D || span > 50.0D) {
            return null;
        }
        return span;
    }

    private List<ExperienceEntry> extractExperience(String experienceSection) {
        List<ExperienceEntry> entries = new ArrayList<>();
        if (experienceSection.isBlank()) {
            return entries;
        }
        for (String rawLine : experienceSection.split("\n")) {
            String line = rawLine.replaceAll("^[\\s\\-*\\u2022\\u00b7]+", "").trim();
            if (line.length() < 6 || line.length() > 200) {
                continue;
            }
            Matcher periodMatcher = PERIOD_PATTERN.matcher(line);
            String period = periodMatcher.find() ? periodMatcher.group(1) : null;
            if (period == null && !FOUR_DIGIT_YEAR_PATTERN.matcher(line).find()) {
                continue;
            }
            String title = line;
            if (period != null) {
                title = line.replace(period, "")
                        .replaceAll("\\(\\s*\\)|\\[\\s*\\]", "")
                        .replaceAll("[\\s|,\\-]+$", "")
                        .trim();
            }
            if (title.isEmpty()) {
                continue;
            }
            entries.add(new ExperienceEntry(null, title, period, null));
            if (entries.size() >= MAX_EXPERIENCE_ENTRIES) {
                break;
            }
        }
        return entries;
    }

    // ------------------------------------------------------------------ education

    private List<EducationEntry> extractEducation(String educationSection) {
        List<EducationEntry> entries = new ArrayList<>();
        if (educationSection.isBlank()) {
            return entries;
        }
        for (String rawLine : educationSection.split("\n")) {
            String line = rawLine.replaceAll("^[\\s\\-*\\u2022\\u00b7]+", "").trim();
            if (line.length() < 4 || line.length() > 250) {
                continue;
            }
            String degree = degreeLabelFor(line);
            if (degree == null) {
                continue;
            }
            Matcher yearMatcher = FOUR_DIGIT_YEAR_PATTERN.matcher(line);
            String year = yearMatcher.find() ? yearMatcher.group(1) : null;
            entries.add(new EducationEntry(line, degree, null, year));
            if (entries.size() >= MAX_EDUCATION_ENTRIES) {
                break;
            }
        }
        return entries;
    }

    private String degreeLabelFor(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (DegreeRule rule : DEGREE_RULES) {
            for (String token : rule.tokens()) {
                if (lower.contains(token)) {
                    return rule.label();
                }
            }
        }
        return null;
    }

    private String highestEducationLabel(List<EducationEntry> educations) {
        for (DegreeRule rule : DEGREE_RULES) {
            for (EducationEntry entry : educations) {
                if (rule.label().equals(entry.getDegree())) {
                    return rule.label();
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ summary and sections

    private String extractSummary(String summarySection) {
        if (summarySection == null || summarySection.isBlank()) {
            return null;
        }
        String flattened = summarySection.replace('\n', ' ').replaceAll(" {2,}", " ").trim();
        return TextUtils.truncate(flattened, 500);
    }

    /** Splits the resume into the well-known sections by looking for short heading lines. */
    private Map<Section, String> splitIntoSections(String text) {
        Map<Section, StringBuilder> buffers = new EnumMap<>(Section.class);
        Section current = null;
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            Section heading = headingFor(line);
            if (heading != null) {
                current = heading;
                buffers.computeIfAbsent(current, key -> new StringBuilder());
                continue;
            }
            if (current != null && !line.isEmpty()) {
                buffers.get(current).append(line).append('\n');
            }
        }
        Map<Section, String> sections = new EnumMap<>(Section.class);
        buffers.forEach((section, buffer) -> sections.put(section, buffer.toString().trim()));
        return sections;
    }

    private Section headingFor(String line) {
        if (line.isEmpty() || line.length() > 45) {
            return null;
        }
        String normalized = line.toLowerCase(Locale.ROOT).replaceAll("[^a-z& ]", " ")
                .replaceAll(" {2,}", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        for (Section section : Section.values()) {
            for (String heading : section.headings()) {
                if (normalized.equals(heading) || normalized.startsWith(heading + " ")) {
                    return section;
                }
            }
        }
        return null;
    }

    /** Resume sections the extractor understands. */
    private enum Section {

        SUMMARY(List.of("summary", "professional summary", "profile", "objective", "about me", "about")),
        SKILLS(List.of("skills", "technical skills", "core competencies", "technologies",
                "tech stack", "skill set", "areas of expertise")),
        EXPERIENCE(List.of("experience", "work experience", "professional experience", "employment",
                "employment history", "work history", "internships", "internship")),
        EDUCATION(List.of("education", "academic background", "academics", "qualifications",
                "educational qualifications")),
        PROJECTS(List.of("projects", "personal projects", "academic projects", "key projects"));

        private final List<String> headings;

        Section(List<String> headings) {
            this.headings = headings;
        }

        List<String> headings() {
            return headings;
        }
    }

    /** A degree label together with the tokens that identify it in resume text. */
    private record DegreeRule(String label, List<String> tokens) {
    }
}
