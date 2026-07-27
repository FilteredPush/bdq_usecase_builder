package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for Phase 3 – ShaclValidationService.
 *
 * Covers requirement E6: Blocking violations prevent export by default;
 * warning-only state allows export; validation report file is produced.
 */
public class ShaclValidationServiceTest {

    private ShaclValidationService service;

    @BeforeEach
    void setUp() {
        service = new ShaclValidationService();
    }

    // -----------------------------------------------------------------------
    // E6: Blocking violations
    // -----------------------------------------------------------------------

    @Test
    void testBlockingErrorWhenUseCaseNameMissing() {
        ProjectState state = new ProjectState();
        // Use case has no name
        ShaclValidationService.ValidationReport report = service.validate(state);
        assertFalse(report.isValid(),
                "Should have blocking error when use case name is missing");
        assertTrue(report.getBlockingErrors().stream()
                        .anyMatch(e -> e.toLowerCase().contains("name")
                                || e.toLowerCase().contains("label")),
                "Error should mention name/label: " + report.getBlockingErrors());
    }

    @Test
    void testBlockingErrorWhenTestTypeMissing() {
        ProjectState state = buildMinimalState();
        TestDraft draft = new TestDraft();
        draft.setLabel("MY_TEST");
        // No type set
        state.addNewTestDraft(draft);

        ShaclValidationService.ValidationReport report = service.validate(state);
        assertFalse(report.isValid(),
                "Should have blocking error when test type is missing");
        assertTrue(report.getBlockingErrors().stream()
                        .anyMatch(e -> e.toLowerCase().contains("type")),
                "Error should mention type: " + report.getBlockingErrors());
    }

    @Test
    void testBlockingErrorWhenTestLabelMissing() {
        ProjectState state = buildMinimalState();
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        // No label or prefLabel
        state.addNewTestDraft(draft);

        ShaclValidationService.ValidationReport report = service.validate(state);
        assertFalse(report.isValid(),
                "Should have blocking error when test has no label");
        assertTrue(report.getBlockingErrors().stream()
                        .anyMatch(e -> e.toLowerCase().contains("label")),
                "Error should mention label: " + report.getBlockingErrors());
    }

    // -----------------------------------------------------------------------
    // E6: Warning-only state allows export (isValid() = true)
    // -----------------------------------------------------------------------

    @Test
    void testWarningOnlyAllowsExport() {
        // Minimal valid state: name set, but no description (warning) and no IEs (warning)
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("My Use Case");
        // No description (warning), no IEs (warning)

        ShaclValidationService.ValidationReport report = service.validate(state);
        assertTrue(report.isValid(),
                "Should be valid (no blocking errors) when only warnings exist");
        assertFalse(report.getWarnings().isEmpty(),
                "Should have at least one warning");
    }

    @Test
    void testCleanStateNoIssues() {
        ProjectState state = buildFullState();
        ShaclValidationService.ValidationReport report = service.validate(state);
        assertTrue(report.isValid(), "Full state should have no blocking errors");
    }

    @Test
    void testNullStateProducesBlockingError() {
        ShaclValidationService.ValidationReport report = service.validate(null);
        assertFalse(report.isValid());
        assertFalse(report.getBlockingErrors().isEmpty());
    }

    // -----------------------------------------------------------------------
    // E6: Validation report file is produced
    // -----------------------------------------------------------------------

    @Test
    void testWriteReportCreatesFile(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildMinimalState();
        ShaclValidationService.ValidationReport report = service.validate(state);

        File reportFile = service.writeReport(report, tempDir.toFile());
        assertTrue(reportFile.exists(), "Validation report file should be created");
        assertTrue(reportFile.getName().endsWith(".md"),
                "Report file should be markdown: " + reportFile.getName());

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("Validation Report"),
                "Report should contain header");
    }

    @Test
    void testReportContainsBlockingErrors(@TempDir Path tempDir) throws Exception {
        ProjectState state = new ProjectState();
        // No name → blocking error
        ShaclValidationService.ValidationReport report = service.validate(state);
        File reportFile = service.writeReport(report, tempDir.toFile());

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("Blocking Error") || content.contains("❌"),
                "Report should indicate blocking errors");
    }

    @Test
    void testReportContainsWarnings(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildMinimalState();
        // No description, no IEs → warnings
        ShaclValidationService.ValidationReport report = service.validate(state);
        File reportFile = service.writeReport(report, tempDir.toFile());

        String content = Files.readString(reportFile.toPath());
        // Should mention warnings
        assertTrue(content.contains("Warnings") || content.contains("⚠"),
                "Report should mention warnings when present");
    }

    @Test
    void testReportPassedIndicatorWhenClean(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildFullState();
        ShaclValidationService.ValidationReport report = service.validate(state);
        File reportFile = service.writeReport(report, tempDir.toFile());

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("✅") || content.contains("passed"),
                "Report should show pass indicator when clean");
    }

    // -----------------------------------------------------------------------
    // ValidationReport inner class
    // -----------------------------------------------------------------------

    @Test
    void testValidationReportIsValidOnlyWithNoBlockingErrors() {
        ShaclValidationService.ValidationReport report = new ShaclValidationService.ValidationReport();
        assertTrue(report.isValid(), "Empty report should be valid");

        report.addWarning("Some warning");
        assertTrue(report.isValid(), "Warnings alone should not invalidate");

        report.addBlockingError("Fatal problem");
        assertFalse(report.isValid(), "Blocking error should invalidate");
    }

    @Test
    void testValidationReportCounts() {
        ShaclValidationService.ValidationReport report = new ShaclValidationService.ValidationReport();
        report.addBlockingError("Error 1");
        report.addBlockingError("Error 2");
        report.addWarning("Warning 1");

        assertEquals(2, report.errorCount());
        assertEquals(1, report.warningCount());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProjectState buildMinimalState() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("Test UC");
        return state;
    }

    private ProjectState buildFullState() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("Test UC");
        state.getUseCaseDraft().setDescription("A description");
        state.getUseCaseDraft().setFitnessRequirementsText("Records need taxonomy");
        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));

        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCIENTIFICNAME_NOTEMPTY");
        draft.setPrefLabel("Scientific name not empty");
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setDimension("Completeness");
        draft.setCriterionOrEnhancement("NotEmpty");
        draft.setExpectedResponse("COMPLIANT if not empty");
        state.addNewTestDraft(draft);

        return state;
    }
}
