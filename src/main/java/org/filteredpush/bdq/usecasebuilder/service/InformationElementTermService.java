package org.filteredpush.bdq.usecasebuilder.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for extracting and matching qualified information-element terms.
 */
public final class InformationElementTermService {

    private static final Pattern TERM_PATTERN =
            Pattern.compile("\\b(?:dwc|ac):[A-Za-z][A-Za-z0-9]*\\b");

    private InformationElementTermService() {
    }

    /**
     * Extracts dwc:/ac: terms from one or more source strings.
     *
     * @param sources one or more strings to scan for qualified terms; {@code null} elements are ignored
     * @return ordered set of matched qualified term names; never {@code null}
     */
    public static Set<String> extractQualifiedTerms(String... sources) {
        Set<String> result = new LinkedHashSet<>();
        if (sources == null) {
            return result;
        }
        Arrays.stream(sources)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .forEach(source -> {
                    Matcher matcher = TERM_PATTERN.matcher(source);
                    while (matcher.find()) {
                        result.add(matcher.group());
                    }
                });
        return result;
    }

    /**
     * Returns true when candidate terms match any selected term by qname or local name.
     *
     * @param candidateTerms the set of candidate qualified terms to test; may be {@code null}
     * @param selectedTerms  the set of already-selected terms to match against; may be {@code null}
     * @return {@code true} if at least one candidate term matches a selected term
     */
    public static boolean matchesAnySelectedTerm(Set<String> candidateTerms, Set<String> selectedTerms) {
        if (candidateTerms == null || candidateTerms.isEmpty()
                || selectedTerms == null || selectedTerms.isEmpty()) {
            return false;
        }
        Set<String> candidateNormalized = normalizeWithLocalNames(candidateTerms);
        Set<String> selectedNormalized = normalizeWithLocalNames(selectedTerms);
        for (String value : candidateNormalized) {
            if (selectedNormalized.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeWithLocalNames(Set<String> terms) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String term : terms) {
            if (term == null || term.trim().isEmpty()) {
                continue;
            }
            String qname = term.trim().toLowerCase(Locale.ROOT);
            normalized.add(qname);
            int split = qname.indexOf(':');
            if (split > 0 && split < qname.length() - 1) {
                normalized.add(qname.substring(split + 1));
            }
        }
        return normalized;
    }
}
