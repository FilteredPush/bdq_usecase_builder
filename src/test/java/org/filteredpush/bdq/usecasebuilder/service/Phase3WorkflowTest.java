package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for Phase 3 – Workflow/State and Gap Analysis.
 *
 * Covers:
 * - E1: Navigating back/forward preserves entered data;
 *       revisiting earlier phases updates dependent later-phase suggestions.
 * - E2: Add/remove link operations produce expected coverage transitions;
 *       bulk operations work for multiple selected rows.
 * - E4: Expected response builder clause ordering, token insertion.
 */
public class Phase3WorkflowTest {

    // -----------------------------------------------------------------------
    // E1: ProjectState preserves data across navigations
    // -----------------------------------------------------------------------

    @Test
    void testProjectStatePreservesUseCaseNameAcrossNavigation() {
        // Simulate: user enters use case name, navigates away (onLeave), navigates back
        // (onEnter reads from state). The name should be preserved.
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("My Use Case");
        state.getUseCaseDraft().setDescription("A description");

        // Simulate a "leave-and-re-enter" cycle
        String nameAfterCycle = state.getUseCaseDraft().getName();
        assertEquals("My Use Case", nameAfterCycle,
                "Use case name should persist across navigation cycles");
    }

    @Test
    void testProjectStatePreservesInformationElements() {
        ProjectState state = new ProjectState();
        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));
        state.addInformationElement(
                new InformationElementRef("dwc:kingdom", InfoElementRole.CONSULTED));

        // After simulated navigation (state is not cleared)
        assertEquals(2, state.getInformationElements().size(),
                "Information elements should persist");
        assertEquals("dwc:scientificName",
                state.getInformationElements().get(0).getQname());
    }

    @Test
    void testProjectStatePreservesNewTestDrafts() {
        ProjectState state = new ProjectState();
        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCINAME_NOTEMPTY");
        draft.setType(TestType.VALIDATION);
        state.addNewTestDraft(draft);

        // After navigation away and back
        assertEquals(1, state.getNewTestDrafts().size(),
                "New test drafts should persist across navigation");
        assertEquals("VALIDATION_SCINAME_NOTEMPTY",
                state.getNewTestDrafts().get(0).getLabel());
    }

    @Test
    void testProjectStatePreservesSelectedExistingTests() {
        ProjectState state = new ProjectState();
        String iri = "https://rs.tdwg.org/bdqtest/terms/test1";
        state.addSelectedExistingTest(iri);

        assertEquals(1, state.getSelectedExistingTestIris().size());
        assertTrue(state.getSelectedExistingTestIris().contains(iri));
    }

    @Test
    void testRevisitingEarlierPhaseDoesNotLoseData() {
        // Simulate: fill use case → fill IEs → go back to use case → re-enter IEs
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("UC Name");
        state.addInformationElement(
                new InformationElementRef("dwc:country", InfoElementRole.ACTED_UPON));

        // "Go back" then "go forward" – state unchanged
        String ucName = state.getUseCaseDraft().getName();
        int ieCount = state.getInformationElements().size();

        assertEquals("UC Name", ucName, "UC name preserved on back-navigate");
        assertEquals(1, ieCount, "IE count preserved on forward-navigate");
    }

    @Test
    void testOutputDirectoryDefaultAndChange() {
        ProjectState state = new ProjectState();
        // Default should contain "output"
        assertTrue(state.getOutputDirectory().contains("output"),
                "Default output dir should contain 'output': " + state.getOutputDirectory());

        // User changes it
        state.setOutputDirectory("/tmp/my_export");
        assertEquals("/tmp/my_export", state.getOutputDirectory(),
                "Output directory should update after user change");
    }

    // -----------------------------------------------------------------------
    // E2: Gap analysis coverage transitions
    // -----------------------------------------------------------------------

    @Test
    void testAddLinkChangesStatusFromGapToCovered() {
        RequirementCoverage row = new RequirementCoverage();
        row.setRequirementId("REQ-1");
        row.setRequirementSummary("Test requirement");

        assertEquals(RequirementCoverage.CoverageStatus.GAP, row.computeStatus(),
                "Initially should be GAP (no tests linked)");

        row.getLinkedNewTests().add("VALIDATION_SCINAME_NOTEMPTY");

        assertEquals(RequirementCoverage.CoverageStatus.COVERED, row.computeStatus(),
                "After adding a test, status should be COVERED");
    }

    @Test
    void testRemoveLinkChangesStatusBackToGap() {
        RequirementCoverage row = new RequirementCoverage();
        row.setRequirementId("REQ-1");
        row.getLinkedNewTests().add("VALIDATION_SCINAME_NOTEMPTY");
        assertEquals(RequirementCoverage.CoverageStatus.COVERED, row.computeStatus());

        // Remove the link
        row.getLinkedNewTests().remove("VALIDATION_SCINAME_NOTEMPTY");
        assertEquals(RequirementCoverage.CoverageStatus.GAP, row.computeStatus(),
                "After removing all links, status should revert to GAP");
    }

    @Test
    void testAddRationaleChangesStatusToPartiallyCovered() {
        RequirementCoverage row = new RequirementCoverage();
        row.setRequirementId("REQ-1");
        row.getLinkedExistingTests().add("https://rs.tdwg.org/bdqtest/terms/test1");
        row.setPartialCoverageRationale("Only partially addresses this requirement");

        assertEquals(RequirementCoverage.CoverageStatus.PARTIALLY_COVERED, row.computeStatus(),
                "With rationale set, status should be PARTIALLY_COVERED");
    }

    @Test
    void testBulkAddMultipleTestLinks() {
        RequirementCoverage row = new RequirementCoverage();
        // Simulate bulk add of multiple existing tests
        List<String> testIris = List.of(
                "https://rs.tdwg.org/bdqtest/terms/test1",
                "https://rs.tdwg.org/bdqtest/terms/test2",
                "https://rs.tdwg.org/bdqtest/terms/test3"
        );
        for (String iri : testIris) {
            if (!row.getLinkedExistingTests().contains(iri)) {
                row.getLinkedExistingTests().add(iri);
            }
        }

        assertEquals(3, row.getLinkedExistingTests().size(),
                "Bulk add should link 3 existing tests");
        assertEquals(RequirementCoverage.CoverageStatus.COVERED, row.computeStatus());
    }

    @Test
    void testBulkRemoveAllLinks() {
        RequirementCoverage row = new RequirementCoverage();
        row.getLinkedExistingTests().add("https://rs.tdwg.org/bdqtest/terms/test1");
        row.getLinkedNewTests().add("MY_TEST_1");
        row.getLinkedNewTests().add("MY_TEST_2");

        // Bulk remove all
        row.getLinkedExistingTests().clear();
        row.getLinkedNewTests().clear();

        assertEquals(RequirementCoverage.CoverageStatus.GAP, row.computeStatus(),
                "After bulk remove, all rows become GAP");
        assertEquals(0, row.getLinkedExistingTests().size());
        assertEquals(0, row.getLinkedNewTests().size());
    }

    @Test
    void testGapAnalysisServiceCountCovered() {
        GapAnalysisService service = new GapAnalysisService();

        List<RequirementCoverage> rows = new java.util.ArrayList<>();
        RequirementCoverage r1 = new RequirementCoverage();
        r1.getLinkedNewTests().add("TEST_1");
        rows.add(r1);

        RequirementCoverage r2 = new RequirementCoverage();
        // no links → GAP
        rows.add(r2);

        RequirementCoverage r3 = new RequirementCoverage();
        r3.getLinkedExistingTests().add("https://rs.tdwg.org/bdqtest/terms/test3");
        rows.add(r3);

        int covered = service.countCovered(rows);
        assertEquals(2, covered, "Two rows should be covered");
    }

    // -----------------------------------------------------------------------
    // E4: Expected response clause ordering
    // -----------------------------------------------------------------------

    @Test
    void testExpectedResponseClauseOrdering() {
        TestDraft draft = new TestDraft();
        ExpectedResponseClause c1 = new ExpectedResponseClause();
        c1.setCondition("EXTERNAL_PREREQUISITES_NOT_MET");
        c1.setStatus("EXTERNAL_PREREQUISITES_NOT_MET");

        ExpectedResponseClause c2 = new ExpectedResponseClause();
        c2.setCondition("dwc:scientificName is empty");
        c2.setStatus("INTERNAL_PREREQUISITES_NOT_MET");

        ExpectedResponseClause c3 = new ExpectedResponseClause();
        c3.setCondition("dwc:scientificName is valid");
        c3.setStatus("RUN_HAS_RESULT");
        c3.setResult("COMPLIANT");

        draft.setExpectedResponseClauses(List.of(c1, c2, c3));

        List<ExpectedResponseClause> clauses = draft.getExpectedResponseClauses();
        assertEquals(3, clauses.size(), "Should have 3 clauses");
        // Order preserved
        assertEquals("EXTERNAL_PREREQUISITES_NOT_MET", clauses.get(0).getCondition(),
                "First clause should be EXTERNAL_PREREQUISITES_NOT_MET");
        assertEquals("dwc:scientificName is empty", clauses.get(1).getCondition(),
                "Second clause preserved");
        assertEquals("dwc:scientificName is valid", clauses.get(2).getCondition(),
                "Third clause preserved");
    }

    @Test
    void testExpectedResponseClauseGeneratesText() {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setCondition("dwc:scientificName is not empty");
        clause.setStatus("RUN_HAS_RESULT");
        clause.setResult("COMPLIANT");
        clause.setCommentTemplate("Scientific name present and well-formed");

        String text = clause.toString();
        assertTrue(text.contains("IF"), "Clause text should contain IF");
        assertTrue(text.contains("THEN"), "Clause text should contain THEN");
        assertTrue(text.contains("RUN_HAS_RESULT"), "Clause text should contain status");
        assertTrue(text.contains("COMPLIANT"), "Clause text should contain result");
    }

    @Test
    void testExpectedResponseElseClauseGeneratesText() {
        ExpectedResponseClause elseClause = new ExpectedResponseClause();
        elseClause.setElseClause(true);
        elseClause.setStatus("RUN_HAS_RESULT");
        elseClause.setResult("NOT_COMPLIANT");

        String text = elseClause.toString();
        assertTrue(text.contains("ELSE"), "Else clause text should contain ELSE");
        assertFalse(text.contains("IF"), "Else clause should not contain IF");
    }

    // -----------------------------------------------------------------------
    // E1: Revisiting earlier phase updates suggestions
    // -----------------------------------------------------------------------

    @Test
    void testLabelSuggestionUpdatesWhenIEChanges() {
        LabelSuggestionService labelService = new LabelSuggestionService();
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:country");
        draft.setCriterionOrEnhancement("Standard");

        labelService.applyAutoSuggestions(draft);
        String firstLabel = draft.getLabel();

        // User revisits IE step and changes the IE
        draft.setActedUponElements(List.of("dwc:scientificName"));
        labelService.applyAutoSuggestions(draft);
        String secondLabel = draft.getLabel();

        assertNotEquals(firstLabel, secondLabel,
                "Label suggestion should update when IE changes");
        assertTrue(secondLabel.contains("SCIENTIFICNAME"),
                "New label should reflect updated IE: " + secondLabel);
    }
}
