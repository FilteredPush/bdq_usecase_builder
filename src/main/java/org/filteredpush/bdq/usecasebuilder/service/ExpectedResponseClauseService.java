package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Serializes/parses structured expected-response clauses.
 *
 * <p>The canonical display format is compact and human-readable:</p>
 * <pre>
 *   RESULT if condition;
 *   RESULT if condition;
 *   otherwise RESULT
 * </pre>
 * <p>e.g.:</p>
 * <pre>
 *   INTERNAL_PREREQUISITES_NOT_MET if dwc:taxonRank is bdqval:Empty;
 *   COMPLIANT if dwc:taxonRank is species or lower;
 *   otherwise NOT_COMPLIANT
 * </pre>
 * <p>Legacy format ({@code IF … THEN status=…}) is also parsed for backward
 * compatibility with saved state.</p>
 */
public class ExpectedResponseClauseService {

    /** Status values that stand alone (not wrapped in RUN_HAS_RESULT). */
    private static final Set<String> PREREQUISITE_STATUSES = Set.of(
            "INTERNAL_PREREQUISITES_NOT_MET",
            "EXTERNAL_PREREQUISITES_NOT_MET");

    /**
     * Renders the clause list as a semicolon-separated, multi-line string.
     * Each clause occupies its own line; clauses are separated by "; \n".
     */
    public String toCanonicalText(List<ExpectedResponseClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sb.append(";\n");
            }
            sb.append(clauses.get(i).toString());
        }
        return sb.toString();
    }

    /**
     * Parses a canonical or legacy expected-response text into a list of
     * {@link ExpectedResponseClause} objects.
     *
     * <p>Accepts both the new compact format and the legacy
     * {@code IF … THEN status=… ; result=…} format.</p>
     *
     * <p>The canonical separator between clauses is ";\n" (produced by
     * {@link #toCanonicalText}). Each line is parsed individually; a trailing
     * ";" on a line is treated as a clause separator and stripped. This
     * preserves internal semicolons used by the legacy format for
     * {@code status=…; result=…} assignments.</p>
     */
    public List<ExpectedResponseClause> parseCanonicalText(String text) {
        List<ExpectedResponseClause> clauses = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return clauses;
        }
        // Split on newlines; trailing ";" on each line is the clause separator
        String[] lines = text.split("\\R");
        for (String line : lines) {
            // Strip trailing clause-separator ";"
            String trimmed = line.trim();
            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            clauses.add(parsePart(trimmed));
        }
        return clauses;
    }

    private ExpectedResponseClause parsePart(String token) {
        String upper = token.toUpperCase(Locale.ROOT);

        // Legacy format: IF … THEN … or ELSE THEN …
        if (upper.startsWith("IF ") || upper.startsWith("ELSE")) {
            return parseLegacyLine(token);
        }

        // New compact format: "RESULT if condition" or "otherwise RESULT"
        if (upper.startsWith("OTHERWISE ")) {
            String outcomeToken = token.substring("otherwise ".length()).trim();
            return buildClauseFromOutcome(true, "", outcomeToken);
        }

        // "RESULT if condition"
        int ifIdx = upper.indexOf(" IF ");
        if (ifIdx > 0) {
            String outcomeToken = token.substring(0, ifIdx).trim();
            String condition = token.substring(ifIdx + " IF ".length()).trim();
            return buildClauseFromOutcome(false, condition, outcomeToken);
        }

        // Unrecognized – treat as freeform else clause
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setElseClause(true);
        clause.setStatus(token);
        return clause;
    }

    private ExpectedResponseClause buildClauseFromOutcome(boolean isElse,
                                                           String condition,
                                                           String outcomeToken) {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setElseClause(isElse);
        clause.setCondition(isElse ? "" : condition);
        String upper = outcomeToken.toUpperCase(Locale.ROOT);
        if (PREREQUISITE_STATUSES.contains(upper)) {
            clause.setStatus(upper);
            clause.setResult("");
        } else {
            clause.setStatus("RUN_HAS_RESULT");
            clause.setResult(upper);
        }
        return clause;
    }

    private ExpectedResponseClause parseLegacyLine(String line) {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        String upper = line.toUpperCase(Locale.ROOT);
        boolean isElse = upper.startsWith("ELSE");
        clause.setElseClause(isElse);

        String content = line;
        if (!isElse && upper.startsWith("IF ")) {
            int thenIdx = upper.indexOf(" THEN ");
            if (thenIdx > 2) {
                clause.setCondition(line.substring(3, thenIdx).trim());
                content = line.substring(thenIdx + " THEN ".length()).trim();
            }
        } else if (isElse) {
            int thenIdx = upper.indexOf(" THEN ");
            if (thenIdx >= 0) {
                content = line.substring(thenIdx + " THEN ".length()).trim();
            } else {
                content = line.substring("ELSE".length()).trim();
            }
        }

        String[] parts = content.split(";");
        for (String part : parts) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = p.substring(eq + 1).trim();
            switch (key) {
                case "status":
                    clause.setStatus(value);
                    break;
                case "result":
                    clause.setResult(value);
                    break;
                case "comment":
                    clause.setCommentTemplate(value);
                    break;
                default:
                    break;
            }
        }
        return clause;
    }
}
