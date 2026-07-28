package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and summarizes requirement coverage matrix rows.
 */
public class GapAnalysisService {

    /**
     * Builds requirement coverage rows from the fitness requirements text and
     * information elements stored in the given project state.
     *
     * @param state the current project state; must not be {@code null}
     * @return list of {@link RequirementCoverage} rows derived from the state
     */
    public List<RequirementCoverage> buildRows(ProjectState state) {
        Map<String, RequirementCoverage> existingById = new LinkedHashMap<>();
        for (RequirementCoverage row : state.getRequirementCoverageRows()) {
            existingById.put(row.getRequirementId(), row);
        }

        List<RequirementCoverage> rows = new ArrayList<>();
        int seq = 1;
        String[] lines = state.getUseCaseDraft().getFitnessRequirementsText() != null
                ? state.getUseCaseDraft().getFitnessRequirementsText().split("\\R")
                : new String[0];
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            // Skip pure HTML structural tags (from the <ul>/<li> storage format)
            if (isHtmlStructuralTag(line)) {
                continue;
            }
            // Strip <li>...</li> wrapper if present
            line = stripListItemTags(line);
            if (line.isEmpty()) {
                continue;
            }
            RequirementCoverage row = existingById.get("REQ-" + seq);
            if (row == null) {
                row = new RequirementCoverage();
                row.setRequirementId("REQ-" + seq);
            }
            if (line.startsWith("-")) {
                row.setRequirementSummary(line.substring(1).trim());
            } else {
                row.setRequirementSummary(line);
            }
            row.setInformationElements(extractInfoElements(row.getRequirementSummary(), state));
            rows.add(row);
            seq++;
        }

        int ieSeq = 1;
        for (InformationElementRef ref : state.getInformationElements()) {
            String qname = ref.getQname() != null ? ref.getQname().trim() : "";
            if (qname.isEmpty()) {
                continue;
            }
            String id = "IE-" + ieSeq;
            RequirementCoverage row = existingById.get(id);
            if (row == null) {
                row = new RequirementCoverage();
                row.setRequirementId(id);
            }
            row.setRequirementSummary("Information element requirement: " + qname);
            row.setInformationElements(qname);
            rows.add(row);
            ieSeq++;
        }
        return rows;
    }

    /**
     * Counts the number of rows with {@link RequirementCoverage.CoverageStatus#COVERED} status.
     *
     * @param rows the list of requirement coverage rows to evaluate
     * @return the number of covered rows
     */
    public int countCovered(List<RequirementCoverage> rows) {
        int covered = 0;
        for (RequirementCoverage row : rows) {
            if (row.computeStatus() == RequirementCoverage.CoverageStatus.COVERED) {
                covered++;
            }
        }
        return covered;
    }

    private String extractInfoElements(String requirementSummary, ProjectState state) {
        if (requirementSummary == null) {
            return "";
        }
        String lower = requirementSummary.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (InformationElementRef ref : state.getInformationElements()) {
            if (ref.getQname() != null && lower.contains(ref.getQname().toLowerCase())) {
                matches.add(ref.getQname());
            }
        }
        return String.join(", ", matches);
    }

    /**
     * Returns {@code true} if the given line (already trimmed) is a pure HTML
     * structural tag that should not be displayed as a requirement row.
     * Handles {@code <ul>}, {@code </ul>}, {@code <ol>}, {@code </ol>}.
     */
    private boolean isHtmlStructuralTag(String line) {
        String lower = line.toLowerCase();
        return lower.equals("<ul>") || lower.equals("</ul>")
                || lower.equals("<ol>") || lower.equals("</ol>");
    }

    /**
     * Strips an outer {@code <li>…</li>} wrapper from a line, if present,
     * and HTML-unescapes common entities so the plain text is shown.
     *
     * @param line a trimmed line that may start with {@code <li>}
     * @return the plain-text content, or the original line if no wrapper was found
     */
    private String stripListItemTags(String line) {
        String lower = line.toLowerCase();
        if (lower.startsWith("<li>")) {
            line = line.substring(4);
        }
        if (line.toLowerCase().endsWith("</li>")) {
            line = line.substring(0, line.length() - 5);
        }
        // Unescape basic HTML entities used by UseCasePage.escapeHtml()
        line = line.replace("&#39;", "'")
                   .replace("&quot;", "\"")
                   .replace("&gt;", ">")
                   .replace("&lt;", "<")
                   .replace("&amp;", "&");
        return line.trim();
    }
}
