package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serializes/parses structured expected-response clauses.
 */
public class ExpectedResponseClauseService {

    public String toCanonicalText(List<ExpectedResponseClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(clauses.get(i).toString());
        }
        return sb.toString();
    }

    public List<ExpectedResponseClause> parseCanonicalText(String text) {
        List<ExpectedResponseClause> clauses = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return clauses;
        }
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            clauses.add(parseLine(trimmed));
        }
        return clauses;
    }

    private ExpectedResponseClause parseLine(String line) {
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
