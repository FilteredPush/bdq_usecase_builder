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
}
