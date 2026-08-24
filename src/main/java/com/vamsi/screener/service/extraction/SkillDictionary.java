package com.vamsi.screener.service.extraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * A curated technology dictionary loaded from {@code resources/data/skills.txt}.
 *
 * <p>It powers two things: the deterministic resume extractor (used when no LLM key is configured)
 * and required-skill inference for job descriptions that do not list skills explicitly.
 *
 * <p>Matching is boundary-aware, so {@code Java} never matches inside {@code JavaScript}. Aliases of
 * one or two characters ({@code C}, {@code R}, {@code Go}, {@code ML}) are matched case-sensitively
 * to avoid the flood of false positives that a case-insensitive match would produce.
 */
@Component
public class SkillDictionary {

    private static final Logger log = LoggerFactory.getLogger(SkillDictionary.class);
    private static final String RESOURCE_PATH = "data/skills.txt";
    /** Characters that, next to a match, mean the alias is really part of a longer token. */
    private static final String EDGE_CHARS = "A-Za-z0-9+#_";
    /** Short aliases additionally refuse to sit next to a hyphen, to keep "Go-live" out of "Go". */
    private static final String SHORT_ALIAS_EDGE_CHARS = EDGE_CHARS + "-";

    private final List<SkillPattern> patterns;
    private final Map<String, String> canonicalByAlias;

    public SkillDictionary() {
        List<SkillPattern> loadedPatterns = new ArrayList<>();
        Map<String, String> loadedAliases = new LinkedHashMap<>();

        for (String line : readLines()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\|");
            String canonical = parts[0].trim();
            if (canonical.isEmpty()) {
                continue;
            }
            for (String token : parts) {
                String alias = token.trim();
                if (alias.isEmpty()) {
                    continue;
                }
                loadedAliases.putIfAbsent(alias.toLowerCase(Locale.ROOT), canonical);
                loadedPatterns.add(new SkillPattern(canonical, compileAlias(alias)));
            }
        }

        this.patterns = List.copyOf(loadedPatterns);
        this.canonicalByAlias = Map.copyOf(loadedAliases);
        log.info("Loaded {} skills ({} match patterns) from the skill dictionary",
                loadedAliases.values().stream().distinct().count(), patterns.size());
    }

    /**
     * Scans free text and returns every dictionary skill it mentions, in canonical spelling.
     *
     * @param text resume or job-description text
     * @return canonical skill names, in dictionary order
     */
    public Set<String> extractFrom(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        for (SkillPattern skillPattern : patterns) {
            Matcher matcher = skillPattern.pattern().matcher(text);
            if (matcher.find()) {
                found.add(skillPattern.canonical());
            }
        }
        return found;
    }

    /**
     * Maps a free-form skill name onto the dictionary spelling.
     *
     * @param raw skill text, for example {@code "react.js"}
     * @return the canonical name when known, otherwise the tidied input
     */
    public String canonicalize(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        String canonical = canonicalByAlias.get(cleaned.toLowerCase(Locale.ROOT));
        if (canonical != null) {
            return canonical;
        }
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    /** @return number of distinct canonical skills in the dictionary. */
    public int size() {
        return (int) canonicalByAlias.values().stream().distinct().count();
    }

    /**
     * Compiles one alias into a boundary-aware pattern.
     *
     * <p>A dot only blocks a match when it continues the name ({@code node.js}), so a skill that ends
     * a sentence ({@code ...on Kubernetes.}) still matches. Aliases longer than two characters may sit
     * next to a hyphen ({@code Redis-backed}); short ones may not, which keeps {@code Go-live} from
     * counting as Go.
     */
    private static Pattern compileAlias(String alias) {
        boolean shortAlias = alias.length() <= 2;
        String edge = shortAlias ? SHORT_ALIAS_EDGE_CHARS : EDGE_CHARS;
        String regex = "(?<![" + edge + "])(?<![A-Za-z0-9]\\.)"
                + Pattern.quote(alias)
                + "(?![" + edge + "])(?!\\.[A-Za-z0-9])";
        return Pattern.compile(regex, shortAlias ? 0 : Pattern.CASE_INSENSITIVE);
    }

    private static List<String> readLines() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load the skill dictionary from " + RESOURCE_PATH, e);
        }
        return lines;
    }

    /** One compiled alias together with the canonical skill it maps to. */
    private record SkillPattern(String canonical, Pattern pattern) {
    }
}
