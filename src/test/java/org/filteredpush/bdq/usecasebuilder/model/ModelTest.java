package org.filteredpush.bdq.usecasebuilder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the domain model classes.
 */
public class ModelTest {

    // -----------------------------------------------------------------------
    // UseCaseDraft
    // -----------------------------------------------------------------------

    @Test
    public void testUseCaseDraftDefaults() {
        UseCaseDraft draft = new UseCaseDraft();
        assertNull(draft.getName());
        assertNull(draft.getDescription());
        assertNull(draft.getFitnessRequirementsText());
    }

    @Test
    public void testUseCaseDraftSetters() {
        UseCaseDraft draft = new UseCaseDraft();
        draft.setName("Test UC");
        draft.setDescription("A description");
        draft.setFitnessRequirementsText("Requirements text");

        assertEquals("Test UC", draft.getName());
        assertEquals("A description", draft.getDescription());
        assertEquals("Requirements text", draft.getFitnessRequirementsText());
    }

    @Test
    public void testUseCaseDraftToStringUsesName() {
        UseCaseDraft draft = new UseCaseDraft();
        draft.setName("My UC");
        assertEquals("My UC", draft.toString());
    }

    @Test
    public void testUseCaseDraftToStringFallback() {
        UseCaseDraft draft = new UseCaseDraft();
        assertEquals("(unnamed use case)", draft.toString());
    }

    // -----------------------------------------------------------------------
    // InformationElementRef
    // -----------------------------------------------------------------------

    @Test
    public void testInformationElementRef() {
        InformationElementRef ref =
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON);
        assertEquals("dwc:scientificName", ref.getQname());
        assertEquals(InfoElementRole.ACTED_UPON, ref.getRole());
    }

    @Test
    public void testInformationElementRefToString() {
        InformationElementRef ref =
                new InformationElementRef("dwc:country", InfoElementRole.CONSULTED);
        assertTrue(ref.toString().contains("dwc:country"));
        assertTrue(ref.toString().contains("Consulted"));
    }

    // -----------------------------------------------------------------------
    // TestDraft
    // -----------------------------------------------------------------------

    @Test
    public void testTestDraftDefaults() {
        TestDraft draft = new TestDraft();
        assertNull(draft.getLabel());
        assertNull(draft.getType());
        assertNull(draft.getResourceType());
    }

    @Test
    public void testTestDraftSetters() {
        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCINAME_NOTEMPTY");
        draft.setPrefLabel("Scientific name not empty");
        draft.setType(TestType.VALIDATION);
        draft.setResourceType(ResourceType.SINGLE_RECORD);
        draft.setDimension("Completeness");
        draft.setCriterionOrEnhancement("NotEmpty");
        draft.setUseCaseReference("UseCase");
        draft.setParameterDefaults("COMPLIANT");
        draft.setExpectedResponse("COMPLIANT if dwc:scientificName is not empty");
        draft.setNotes("Note 1");

        assertEquals("VALIDATION_SCINAME_NOTEMPTY", draft.getLabel());
        assertEquals("Scientific name not empty", draft.getPrefLabel());
        assertEquals(TestType.VALIDATION, draft.getType());
        assertEquals(ResourceType.SINGLE_RECORD, draft.getResourceType());
        assertEquals("Completeness", draft.getDimension());
        assertEquals("NotEmpty", draft.getCriterionOrEnhancement());
        assertEquals("UseCase", draft.getUseCaseReference());
        assertEquals("COMPLIANT", draft.getParameterDefaults());
        assertEquals("COMPLIANT if dwc:scientificName is not empty", draft.getExpectedResponse());
        assertEquals("Note 1", draft.getNotes());
    }

    @Test
    public void testTestDraftToStringUsesLabel() {
        TestDraft draft = new TestDraft();
        draft.setLabel("MY_LABEL");
        assertEquals("MY_LABEL", draft.toString());
    }

    @Test
    public void testTestDraftToStringFallbackToPrefLabel() {
        TestDraft draft = new TestDraft();
        draft.setPrefLabel("My pref label");
        assertEquals("My pref label", draft.toString());
    }

    // -----------------------------------------------------------------------
    // ProjectState
    // -----------------------------------------------------------------------

    @Test
    public void testProjectStateDefaults() {
        ProjectState state = new ProjectState();
        assertNotNull(state.getUseCaseDraft());
        assertNotNull(state.getOutputDirectory());
        assertTrue(state.getOutputDirectory().endsWith("output"));
        assertTrue(state.getInformationElements().isEmpty());
        assertTrue(state.getSelectedExistingTestIris().isEmpty());
        assertTrue(state.getNewTestDrafts().isEmpty());
    }

    @Test
    public void testProjectStateAddRemoveIE() {
        ProjectState state = new ProjectState();
        InformationElementRef ref =
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON);
        state.addInformationElement(ref);
        assertEquals(1, state.getInformationElements().size());
        state.removeInformationElement(0);
        assertTrue(state.getInformationElements().isEmpty());
    }

    @Test
    public void testProjectStateAddNullIEIgnored() {
        ProjectState state = new ProjectState();
        state.addInformationElement(null);
        assertTrue(state.getInformationElements().isEmpty());
    }

    @Test
    public void testProjectStateSelectedTests() {
        ProjectState state = new ProjectState();
        state.addSelectedExistingTest("https://example.org/test/1");
        state.addSelectedExistingTest("https://example.org/test/1"); // duplicate
        assertEquals(1, state.getSelectedExistingTestIris().size());
        state.removeSelectedExistingTest("https://example.org/test/1");
        assertTrue(state.getSelectedExistingTestIris().isEmpty());
    }

    @Test
    public void testProjectStateNewTestDrafts() {
        ProjectState state = new ProjectState();
        TestDraft draft = new TestDraft();
        draft.setLabel("MY_TEST");
        state.addNewTestDraft(draft);
        assertEquals(1, state.getNewTestDrafts().size());
        state.removeNewTestDraft(0);
        assertTrue(state.getNewTestDrafts().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    @Test
    public void testTestTypeDisplayNames() {
        assertEquals("Validation", TestType.VALIDATION.getDisplayName());
        assertEquals("Measure", TestType.MEASURE.getDisplayName());
        assertEquals("Amendment", TestType.AMENDMENT.getDisplayName());
        assertEquals("Issue", TestType.ISSUE.getDisplayName());
    }

    @Test
    public void testInfoElementRoleDisplayNames() {
        assertEquals("ActedUpon", InfoElementRole.ACTED_UPON.getDisplayName());
        assertEquals("Consulted", InfoElementRole.CONSULTED.getDisplayName());
    }

    @Test
    public void testResourceTypeDisplayNames() {
        assertEquals("SingleRecord", ResourceType.SINGLE_RECORD.getDisplayName());
        assertEquals("MultiRecord", ResourceType.MULTI_RECORD.getDisplayName());
    }
}
