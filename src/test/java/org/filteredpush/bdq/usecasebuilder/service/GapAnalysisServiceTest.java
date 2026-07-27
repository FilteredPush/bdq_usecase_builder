package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GapAnalysisServiceTest {

    @Test
    public void testGapCanBecomeCoveredAfterLinkingNewTest() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setFitnessRequirementsText(
                "Data are fit for use for test if they...\n- include dwc:scientificName");
        state.addInformationElement(new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));

        GapAnalysisService service = new GapAnalysisService();
        List<RequirementCoverage> rows = service.buildRows(state);
        assertTrue(rows.stream().anyMatch(r -> r.computeStatus() == RequirementCoverage.CoverageStatus.GAP));

        RequirementCoverage requirement = rows.get(0);
        requirement.getLinkedNewTests().add("VALIDATION_SCINAME_NOTEMPTY");

        assertEquals(RequirementCoverage.CoverageStatus.COVERED, requirement.computeStatus());
        assertEquals(1, service.countCovered(rows));
    }
}
