package org.filteredpush.bdq.usecasebuilder.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One matrix row linking a requirement/information element to tests.
 */
public class RequirementCoverage {

    public enum CoverageStatus {
        COVERED("Covered"),
        PARTIALLY_COVERED("Partially Covered"),
        GAP("Gap");

        private final String displayName;

        CoverageStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String requirementId;
    private String requirementSummary;
    private String informationElements;
    private final List<String> linkedExistingTests = new ArrayList<>();
    private final List<String> linkedNewTests = new ArrayList<>();
    private String partialCoverageRationale;
    private String notes;

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getRequirementSummary() {
        return requirementSummary;
    }

    public void setRequirementSummary(String requirementSummary) {
        this.requirementSummary = requirementSummary;
    }

    public String getInformationElements() {
        return informationElements;
    }

    public void setInformationElements(String informationElements) {
        this.informationElements = informationElements;
    }

    public List<String> getLinkedExistingTests() {
        return linkedExistingTests;
    }

    public List<String> getLinkedNewTests() {
        return linkedNewTests;
    }

    public String getPartialCoverageRationale() {
        return partialCoverageRationale;
    }

    public void setPartialCoverageRationale(String partialCoverageRationale) {
        this.partialCoverageRationale = partialCoverageRationale;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public CoverageStatus computeStatus() {
        boolean hasAny = !linkedExistingTests.isEmpty() || !linkedNewTests.isEmpty();
        if (!hasAny) {
            return CoverageStatus.GAP;
        }
        if (partialCoverageRationale != null && !partialCoverageRationale.trim().isEmpty()) {
            return CoverageStatus.PARTIALLY_COVERED;
        }
        return CoverageStatus.COVERED;
    }
}
