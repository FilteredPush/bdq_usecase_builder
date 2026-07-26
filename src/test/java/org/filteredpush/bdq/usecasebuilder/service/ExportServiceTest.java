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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ExportService}.
 */
public class ExportServiceTest {

    private ExportService service;

    @BeforeEach
    public void setUp() {
        service = new ExportService();
    }

    // -----------------------------------------------------------------------
    // export() integration test
    // -----------------------------------------------------------------------

    @Test
    public void testExportWritesBothFiles(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        String result = service.export(state);

        File mdFile = new File(tempDir.toFile(), "usecase_summary.md");
        File jsonFile = new File(tempDir.toFile(), "project_state.json");

        assertTrue(mdFile.exists(), "Markdown file should be created");
        assertTrue(jsonFile.exists(), "JSON file should be created");
        assertTrue(result.contains("usecase_summary.md"),
                "Export result should mention the markdown file");
        assertTrue(result.contains("project_state.json"),
                "Export result should mention the JSON file");
    }

    // -----------------------------------------------------------------------
    // writeMarkdown
    // -----------------------------------------------------------------------

    @Test
    public void testWriteMarkdownContainsUseCaseName(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File mdFile = new File(tempDir.toFile(), "test.md");
        service.writeMarkdown(state, mdFile);

        String content = new String(Files.readAllBytes(mdFile.toPath()));
        assertTrue(content.contains("Test Use Case"), "Markdown should contain use case name");
    }

    @Test
    public void testWriteMarkdownContainsInformationElements(@TempDir Path tempDir)
            throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File mdFile = new File(tempDir.toFile(), "test.md");
        service.writeMarkdown(state, mdFile);

        String content = new String(Files.readAllBytes(mdFile.toPath()));
        assertTrue(content.contains("dwc:scientificName"),
                "Markdown should list information elements");
        assertTrue(content.contains("ActedUpon"),
                "Markdown should show element role");
    }

    @Test
    public void testWriteMarkdownContainsNewTest(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File mdFile = new File(tempDir.toFile(), "test.md");
        service.writeMarkdown(state, mdFile);

        String content = new String(Files.readAllBytes(mdFile.toPath()));
        assertTrue(content.contains("VALIDATION_SCINAME_NOTEMPTY"),
                "Markdown should include new test label");
    }

    // -----------------------------------------------------------------------
    // writeJson
    // -----------------------------------------------------------------------

    @Test
    public void testWriteJsonContainsUseCaseName(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File jsonFile = new File(tempDir.toFile(), "test.json");
        service.writeJson(state, jsonFile);

        String content = new String(Files.readAllBytes(jsonFile.toPath()));
        assertTrue(content.contains("Test Use Case"),
                "JSON should contain use case name");
    }

    @Test
    public void testWriteJsonContainsInformationElement(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File jsonFile = new File(tempDir.toFile(), "test.json");
        service.writeJson(state, jsonFile);

        String content = new String(Files.readAllBytes(jsonFile.toPath()));
        assertTrue(content.contains("dwc:scientificName"),
                "JSON should include information element");
    }

    @Test
    public void testWriteJsonContainsNewTest(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState(tempDir.toString());
        File jsonFile = new File(tempDir.toFile(), "test.json");
        service.writeJson(state, jsonFile);

        String content = new String(Files.readAllBytes(jsonFile.toPath()));
        assertTrue(content.contains("VALIDATION_SCINAME_NOTEMPTY"),
                "JSON should include new test label");
    }

    @Test
    public void testExportNullStateThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.export(null));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProjectState buildSampleState(String outputDir) {
        ProjectState state = new ProjectState();
        state.setOutputDirectory(outputDir);

        state.getUseCaseDraft().setName("Test Use Case");
        state.getUseCaseDraft().setDescription("A test description");
        state.getUseCaseDraft().setFitnessRequirementsText("Records must have valid taxonomy");

        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));
        state.addInformationElement(
                new InformationElementRef("dwc:kingdom", InfoElementRole.CONSULTED));

        state.addSelectedExistingTest("https://rs.tdwg.org/bdqtest/terms/47ff73ba-0028-4f79-9ce1-ee7b2130f498");

        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCINAME_NOTEMPTY");
        draft.setPrefLabel("Scientific name not empty");
        draft.setType(TestType.VALIDATION);
        draft.setDimension("Completeness");
        draft.setExpectedResponse("COMPLIANT if dwc:scientificName is not empty; otherwise NOT_COMPLIANT");
        state.addNewTestDraft(draft);

        return state;
    }
}
