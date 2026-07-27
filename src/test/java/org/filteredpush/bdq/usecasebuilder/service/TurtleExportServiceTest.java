package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for Phase 3 – TurtleExportService.
 *
 * Covers requirements E5 and E7:
 * - E5: Minimal export excludes unselected existing tests;
 *       Include-existing mode includes selected/referenced existing tests.
 * - E7: Export writes to default output directory when unchanged;
 *       Export writes to user-selected directory when changed.
 */
public class TurtleExportServiceTest {

    private TurtleExportService service;

    @BeforeEach
    void setUp() {
        service = new TurtleExportService();
    }

    // -----------------------------------------------------------------------
    // E7: Output directory behavior
    // -----------------------------------------------------------------------

    @Test
    void testExportWritesToSpecifiedDirectory(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        state.setOutputDirectory(tempDir.toString());

        File ttlFile = service.exportMinimal(state, tempDir.toFile());
        assertTrue(ttlFile.exists(),
                "Turtle file should be created in specified directory");
        assertTrue(ttlFile.getAbsolutePath().startsWith(tempDir.toAbsolutePath().toString()),
                "File should be in the specified directory");
    }

    @Test
    void testExportMinimalCreatesCorrectFilename(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        File ttlFile = service.exportMinimal(state, tempDir.toFile());

        assertEquals("usecase_new.ttl", ttlFile.getName(),
                "Minimal export should create usecase_new.ttl");
    }

    @Test
    void testExportWithExistingCreatesCorrectFilename(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        state.addSelectedExistingTest(
                "https://rs.tdwg.org/bdqtest/terms/47ff73ba-0028-4f79-9ce1-ee7b2130f498");

        TestCatalogService catalogService = new TestCatalogService();
        catalogService.loadCatalog();

        File ttlFile = service.exportWithExisting(state, tempDir.toFile(), catalogService);
        assertEquals("usecase_with_existing.ttl", ttlFile.getName(),
                "Include-existing export should create usecase_with_existing.ttl");
    }

    @Test
    void testExportCreatesOutputDirectoryIfNotExists(@TempDir Path tempDir) throws Exception {
        File nestedDir = tempDir.resolve("nested/output").toFile();
        assertFalse(nestedDir.exists(), "Directory should not exist yet");

        ProjectState state = buildSampleState();
        File ttlFile = service.exportMinimal(state, nestedDir);
        assertTrue(ttlFile.exists(), "Turtle file should be created even if dir was missing");
    }

    // -----------------------------------------------------------------------
    // E5: Minimal mode excludes existing tests
    // -----------------------------------------------------------------------

    @Test
    void testMinimalExportExcludesExistingTests(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        String existingIri =
                "https://rs.tdwg.org/bdqtest/terms/47ff73ba-0028-4f79-9ce1-ee7b2130f498";
        state.addSelectedExistingTest(existingIri);

        File ttlFile = service.exportMinimal(state, tempDir.toFile());
        String content = Files.readString(ttlFile.toPath());
        // The UUID portion of the IRI should NOT appear in minimal mode
        assertFalse(content.contains("47ff73ba-0028-4f79-9ce1-ee7b2130f498"),
                "Minimal export should NOT include selected existing test");
    }

    @Test
    void testMinimalExportContainsNewTest(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        File ttlFile = service.exportMinimal(state, tempDir.toFile());
        String content = Files.readString(ttlFile.toPath());

        assertTrue(content.contains("VALIDATION_SCIENTIFICNAME_NOTEMPTY"),
                "Minimal export should include new test label");
    }

    @Test
    void testMinimalExportContainsUseCaseName(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        File ttlFile = service.exportMinimal(state, tempDir.toFile());
        String content = Files.readString(ttlFile.toPath());

        assertTrue(content.contains("Test Use Case"),
                "Minimal export should include use case name");
    }

    // -----------------------------------------------------------------------
    // E5: Include-existing mode includes referenced tests
    // -----------------------------------------------------------------------

    @Test
    void testIncludeExistingModeContainsExistingTests(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        String existingIri =
                "https://rs.tdwg.org/bdqtest/terms/47ff73ba-0028-4f79-9ce1-ee7b2130f498";
        state.addSelectedExistingTest(existingIri);

        TestCatalogService catalogService = new TestCatalogService();
        catalogService.loadCatalog();
        File ttlFile = service.exportWithExisting(state, tempDir.toFile(), catalogService);
        String content = Files.readString(ttlFile.toPath());

        // Jena may use prefixed form (bdqtest:47ff73ba-...) or full IRI;
        // check for the UUID portion which will appear either way
        assertTrue(content.contains("47ff73ba-0028-4f79-9ce1-ee7b2130f498"),
                "Include-existing export should contain referenced existing test (UUID portion)");
    }

    @Test
    void testIncludeExistingModeAlsoContainsNewTest(@TempDir Path tempDir) throws Exception {
        ProjectState state = buildSampleState();
        String existingIri =
                "https://rs.tdwg.org/bdqtest/terms/47ff73ba-0028-4f79-9ce1-ee7b2130f498";
        state.addSelectedExistingTest(existingIri);

        TestCatalogService catalogService = new TestCatalogService();
        catalogService.loadCatalog();
        File ttlFile = service.exportWithExisting(state, tempDir.toFile(), catalogService);
        String content = Files.readString(ttlFile.toPath());

        assertTrue(content.contains("VALIDATION_SCIENTIFICNAME_NOTEMPTY"),
                "Include-existing export should also include new tests");
        assertTrue(content.contains("47ff73ba-0028-4f79-9ce1-ee7b2130f498"),
                "Include-existing export should include existing test (UUID portion)");
    }

    // -----------------------------------------------------------------------
    // Model building (internal)
    // -----------------------------------------------------------------------

    @Test
    void testBuildModelContainsUseCaseResource() {
        ProjectState state = buildSampleState();
        Model model = service.buildModel(state, false, null);

        ResIterator it = model.listSubjectsWithProperty(
                RDF.type, BdqFfdq.UseCase);
        assertTrue(it.hasNext(), "Model should contain a bdqffdq:UseCase resource");
    }

    @Test
    void testBuildModelContainsPolicyResource() {
        ProjectState state = buildSampleState();
        Model model = service.buildModel(state, false, null);

        ResIterator it = model.listSubjectsWithProperty(
                RDF.type, BdqFfdq.Policy);
        assertTrue(it.hasNext(), "Model should contain a bdqffdq:Policy resource");
    }

    @Test
    void testBuildModelNewTestHasCorrectType() {
        ProjectState state = buildSampleState();
        Model model = service.buildModel(state, false, null);

        ResIterator it = model.listSubjectsWithProperty(RDF.type, BdqFfdq.Validation);
        assertTrue(it.hasNext(),
                "Model should contain a bdqffdq:Validation resource for the new test");
    }

    @Test
    void testBuildModelMultipleActedUponIEsAreIncluded() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("Multi-IE UC");

        TestDraft draft = new TestDraft();
        draft.setLabel("TEST_MULTI");
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.addActedUponElement("dwc:kingdom");
        draft.addConsultedElement("dwc:taxonRank");
        state.addNewTestDraft(draft);

        Model model = service.buildModel(state, false, null);

        // There should be InformationElement resources
        ResIterator it = model.listSubjectsWithProperty(RDF.type,
                model.createResource(BdqFfdq.NS + "InformationElement"));
        List<String> ieUris = new java.util.ArrayList<>();
        while (it.hasNext()) {
            ieUris.add(it.next().getURI());
        }
        // 2 actedUpon + 1 consulted = 3 IE resources
        assertTrue(ieUris.size() >= 3,
                "Model should have at least 3 IE resources for 2 actedUpon + 1 consulted, got: "
                + ieUris.size());
    }

    @Test
    void testBuildModelWithExistingTestsNull() {
        ProjectState state = buildSampleState();
        // Should not throw when catalogService is null in minimal mode
        Model model = service.buildModel(state, false, null);
        assertNotNull(model);
    }

    @Test
    void testBuildModelExcludesExistingInMinimalMode() {
        ProjectState state = buildSampleState();
        String existingIri = "https://rs.tdwg.org/bdqtest/terms/12345";
        state.addSelectedExistingTest(existingIri);

        Model model = service.buildModel(state, false, null); // minimal mode
        // The model should not reference the existing test IRI
        assertFalse(model.containsResource(model.createResource(existingIri)),
                "Minimal mode model should not reference existing test");
    }

    @Test
    void testBuildModelIncludesExistingInIncludeMode() {
        ProjectState state = buildSampleState();
        String existingIri = "https://rs.tdwg.org/bdqtest/terms/12345";
        state.addSelectedExistingTest(existingIri);

        Model model = service.buildModel(state, true, null); // include-existing mode
        assertTrue(model.containsResource(model.createResource(existingIri)),
                "Include-existing mode model should reference existing test");
    }

    @Test
    void testExportNullStateThrows(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class,
                () -> service.exportMinimal(null, tempDir.toFile()));
    }

    // -----------------------------------------------------------------------
    // E3: Multi-valued actedUpon/consulted on TestDraft
    // -----------------------------------------------------------------------

    @Test
    void testTestDraftSupportsMultipleActedUponElements() {
        TestDraft draft = new TestDraft();
        draft.addActedUponElement("dwc:scientificName");
        draft.addActedUponElement("dwc:kingdom");
        draft.addActedUponElement("dwc:phylum");

        assertEquals(3, draft.getActedUponElements().size(),
                "Draft should support 3 actedUpon elements");
        assertTrue(draft.getActedUponElements().contains("dwc:scientificName"));
        assertTrue(draft.getActedUponElements().contains("dwc:kingdom"));
        assertTrue(draft.getActedUponElements().contains("dwc:phylum"));
    }

    @Test
    void testTestDraftSupportsMultipleConsultedElements() {
        TestDraft draft = new TestDraft();
        draft.addConsultedElement("dwc:kingdom");
        draft.addConsultedElement("dwc:phylum");

        assertEquals(2, draft.getConsultedElements().size(),
                "Draft should support 2 consulted elements");
    }

    @Test
    void testTestDraftDeduplicatesActedUponElements() {
        TestDraft draft = new TestDraft();
        draft.addActedUponElement("dwc:scientificName");
        draft.addActedUponElement("dwc:scientificName"); // duplicate

        assertEquals(1, draft.getActedUponElements().size(),
                "Duplicates should be ignored");
    }

    @Test
    void testTestDraftGetAllInformationElements() {
        TestDraft draft = new TestDraft();
        draft.addActedUponElement("dwc:scientificName");
        draft.addActedUponElement("dwc:kingdom");
        draft.addConsultedElement("dwc:taxonRank");

        List<String> all = draft.getAllInformationElements();
        assertEquals(3, all.size(), "getAllInformationElements should return 3 unique elements");
        assertTrue(all.contains("dwc:scientificName"));
        assertTrue(all.contains("dwc:kingdom"));
        assertTrue(all.contains("dwc:taxonRank"));
    }

    @Test
    void testTestDraftGetAllInformationElementsDeduplicatesOverlap() {
        TestDraft draft = new TestDraft();
        draft.addActedUponElement("dwc:scientificName");
        draft.addConsultedElement("dwc:scientificName"); // same element, different role

        // getAllInformationElements should deduplicate
        List<String> all = draft.getAllInformationElements();
        assertEquals(1, all.size(),
                "getAllInformationElements should deduplicate across roles");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProjectState buildSampleState() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("Test Use Case");
        state.getUseCaseDraft().setDescription("A test description");
        state.addInformationElement(
                new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));

        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCIENTIFICNAME_NOTEMPTY");
        draft.setPrefLabel("Scientific name not empty");
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setDimension("Completeness");
        draft.setCriterionOrEnhancement("NotEmpty");
        draft.setExpectedResponse("COMPLIANT if dwc:scientificName is not empty");

        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setCondition("dwc:scientificName is not empty");
        clause.setStatus("RUN_HAS_RESULT");
        clause.setResult("COMPLIANT");
        draft.setExpectedResponseClauses(List.of(clause));

        state.addNewTestDraft(draft);
        return state;
    }
}
