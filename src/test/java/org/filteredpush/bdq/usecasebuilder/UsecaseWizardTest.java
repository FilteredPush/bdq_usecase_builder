package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UsecaseWizard}.
 */
public class UsecaseWizardTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private UsecaseWizard wizardWithInput(Model model, String input) {
        Configuration config = new Configuration();
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new UsecaseWizard(model, config, is);
    }

    private Model emptyModel() {
        return ModelFactory.createDefaultModel();
    }

    private Model modelWithTests() {
        Model model = ModelFactory.createDefaultModel();
        Resource v1 = model.createResource("https://rs.tdwg.org/bdqtest/terms/v001");
        v1.addProperty(RDF.type, BdqFfdq.Validation);
        v1.addProperty(RDFS.label, "Validation One");
        Resource m1 = model.createResource("https://rs.tdwg.org/bdqtest/terms/m001");
        m1.addProperty(RDF.type, BdqFfdq.Measure);
        m1.addProperty(RDFS.label, "Measure One");
        return model;
    }

    // -----------------------------------------------------------------------
    // findAvailableTests
    // -----------------------------------------------------------------------

    @Test
    public void testFindAvailableTestsInEmptyModel() {
        UsecaseWizard wizard = wizardWithInput(emptyModel(), "4\n");
        List<UsecaseWizard.TestEntry> tests = wizard.findAvailableTests();
        assertNotNull(tests);
        assertTrue(tests.isEmpty());
    }

    @Test
    public void testFindAvailableTestsFindsValidation() {
        UsecaseWizard wizard = wizardWithInput(modelWithTests(), "4\n");
        List<UsecaseWizard.TestEntry> tests = wizard.findAvailableTests();
        assertFalse(tests.isEmpty(), "Should find at least one test");
    }

    @Test
    public void testFindAvailableTestsFindsAllTypes() {
        // Load the test fixture which has Validation, Measure, and Amendment
        Model model = ModelFactory.createDefaultModel();
        String uri = getClass().getResource("/test-data.ttl").toString();
        model.read(uri, "TURTLE");

        UsecaseWizard wizard = wizardWithInput(model, "4\n");
        List<UsecaseWizard.TestEntry> tests = wizard.findAvailableTests();
        // The fixture has 3 test instances (Validation + Measure + Amendment)
        assertEquals(3, tests.size(), "Should find 3 tests from test-data.ttl");
    }

    // -----------------------------------------------------------------------
    // Wizard flow
    // -----------------------------------------------------------------------

    @Test
    public void testQuitImmediatelyReturnsNull() {
        UsecaseWizard wizard = wizardWithInput(emptyModel(), "4\n");
        UsecaseModel result = wizard.run();
        assertNull(result, "Quitting without creating a use case should return null");
    }

    @Test
    public void testCreateUseCaseThenFinish() {
        // Simulate: 1 (create), label, empty IRI (accept suggested), description, 3 (finish)
        String input = "1\nMy Use Case\n\nA description\n3\n";
        UsecaseWizard wizard = wizardWithInput(emptyModel(), input);
        UsecaseModel result = wizard.run();

        assertNotNull(result, "Should return a use case after option 3");
        assertEquals("My Use Case", result.getLabel());
        assertEquals("A description", result.getDescription());
        assertNotNull(result.getIri(), "IRI should be auto-generated");
    }

    @Test
    public void testCreateUseCaseWithCustomIri() {
        String customIri = "https://example.org/usecase/my-usecase";
        String input = "1\nNamed UC\n" + customIri + "\nDesc\n3\n";
        UsecaseWizard wizard = wizardWithInput(emptyModel(), input);
        UsecaseModel result = wizard.run();

        assertNotNull(result);
        assertEquals(customIri, result.getIri());
    }

    @Test
    public void testAddAllTestsThenFinish() {
        Model model = modelWithTests();
        // 1=create, label, IRI, desc, 2=add tests, 'all', 3=finish
        String input = "1\nMy UC\n\n\n2\nall\n3\n";
        UsecaseWizard wizard = wizardWithInput(model, input);
        UsecaseModel result = wizard.run();

        assertNotNull(result);
        assertEquals(2, result.getTestIris().size(),
                "Both tests from the model should be added");
    }

    @Test
    public void testAddSpecificTestByNumber() {
        Model model = modelWithTests();
        // 1=create, label, IRI, desc, 2=add tests, select test 1, 3=finish
        String input = "1\nMy UC\n\n\n2\n1\n3\n";
        UsecaseWizard wizard = wizardWithInput(model, input);
        UsecaseModel result = wizard.run();

        assertNotNull(result);
        assertEquals(1, result.getTestIris().size());
    }

    @Test
    public void testEmptyLabelDoesNotCreateUseCase() {
        // Press Enter for empty label, then quit
        String input = "1\n\n4\n";
        UsecaseWizard wizard = wizardWithInput(emptyModel(), input);
        UsecaseModel result = wizard.run();
        assertNull(result);
    }
}
