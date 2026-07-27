package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationService}.
 */
public class ValidationServiceTest {

    private ValidationService service;

    @BeforeEach
    public void setUp() {
        service = new ValidationService();
    }

    // -----------------------------------------------------------------------
    // Welcome page
    // -----------------------------------------------------------------------

    @Test
    public void testValidateWelcomePageValid() {
        ProjectState state = new ProjectState();
        state.setOutputDirectory("/tmp/output");
        assertTrue(service.validateWelcomePage(state).isEmpty());
    }

    @Test
    public void testValidateWelcomePageMissingDir() {
        ProjectState state = new ProjectState();
        state.setOutputDirectory(null);
        List<String> errors = service.validateWelcomePage(state);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).toLowerCase().contains("output directory"));
    }

    @Test
    public void testValidateWelcomePageBlankDir() {
        ProjectState state = new ProjectState();
        state.setOutputDirectory("   ");
        List<String> errors = service.validateWelcomePage(state);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateWelcomePageNullState() {
        List<String> errors = service.validateWelcomePage(null);
        assertFalse(errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Use case page
    // -----------------------------------------------------------------------

    @Test
    public void testValidateUseCasePageValid() {
        UseCaseDraft draft = new UseCaseDraft();
        draft.setName("My UC");
        assertTrue(service.validateUseCasePage(draft).isEmpty());
    }

    @Test
    public void testValidateUseCasePageMissingName() {
        UseCaseDraft draft = new UseCaseDraft();
        List<String> errors = service.validateUseCasePage(draft);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).toLowerCase().contains("name"));
    }

    @Test
    public void testValidateUseCasePageBlankName() {
        UseCaseDraft draft = new UseCaseDraft();
        draft.setName("  ");
        List<String> errors = service.validateUseCasePage(draft);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateUseCasePageNullDraft() {
        List<String> errors = service.validateUseCasePage(null);
        assertFalse(errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Information elements page
    // -----------------------------------------------------------------------

    @Test
    public void testValidateIEPageValid() {
        ProjectState state = new ProjectState();
        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));
        assertTrue(service.validateInformationElementsPage(state).isEmpty());
    }

    @Test
    public void testValidateIEPageEmpty() {
        ProjectState state = new ProjectState();
        List<String> errors = service.validateInformationElementsPage(state);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).toLowerCase().contains("information element"));
    }

    @Test
    public void testValidateIEPageBlankQname() {
        ProjectState state = new ProjectState();
        state.addInformationElement(
                new InformationElementRef("", InfoElementRole.ACTED_UPON));
        List<String> errors = service.validateInformationElementsPage(state);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateIEPageNullRole() {
        ProjectState state = new ProjectState();
        state.addInformationElement(new InformationElementRef("dwc:country", null));
        List<String> errors = service.validateInformationElementsPage(state);
        assertFalse(errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // New test page
    // -----------------------------------------------------------------------

    @Test
    public void testValidateNewTestPageValid() {
        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCINAME_NOTEMPTY");
        draft.setType(TestType.VALIDATION);
        draft.setInformationElement("dwc:scientificName");
        assertTrue(service.validateNewTestPage(draft).isEmpty());
    }

    @Test
    public void testValidateNewTestPagePrefLabelSuffices() {
        TestDraft draft = new TestDraft();
        draft.setPrefLabel("My test label");
        draft.setType(TestType.MEASURE);
        draft.setInformationElement("dwc:scientificName");
        assertTrue(service.validateNewTestPage(draft).isEmpty());
    }

    @Test
    public void testValidateNewTestPageMissingLabel() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        List<String> errors = service.validateNewTestPage(draft);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateNewTestPageMissingType() {
        TestDraft draft = new TestDraft();
        draft.setLabel("MY_LABEL");
        List<String> errors = service.validateNewTestPage(draft);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).toLowerCase().contains("type"));
    }

    @Test
    public void testValidateNewTestPageNullDraft() {
        List<String> errors = service.validateNewTestPage(null);
        assertFalse(errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // validateForExport
    // -----------------------------------------------------------------------

    @Test
    public void testValidateForExportValid() {
        ProjectState state = buildValidState();
        assertTrue(service.validateForExport(state).isEmpty());
    }

    @Test
    public void testValidateForExportAccumulatesErrors() {
        ProjectState state = new ProjectState(); // all defaults – multiple errors expected
        List<String> errors = service.validateForExport(state);
        assertTrue(errors.size() >= 2,
                "Expected at least 2 errors, got: " + errors);
    }

    @Test
    public void testValidateForExportNullState() {
        List<String> errors = service.validateForExport(null);
        assertFalse(errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProjectState buildValidState() {
        ProjectState state = new ProjectState();
        state.setOutputDirectory("/tmp");
        state.getUseCaseDraft().setName("Test UC");
        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));
        return state;
    }
}
