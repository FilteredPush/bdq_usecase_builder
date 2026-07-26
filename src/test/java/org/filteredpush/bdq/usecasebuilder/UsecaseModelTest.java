package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UsecaseModel}.
 */
public class UsecaseModelTest {

    private static final String TEST_IRI   = "urn:uuid:test-usecase-001";
    private static final String TEST_LABEL = "Test Use Case";
    private static final String TEST_DESC  = "A test use case for unit testing";

    @Test
    public void testConstructorPopulatesFields() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        assertEquals(TEST_IRI,   model.getIri());
        assertEquals(TEST_LABEL, model.getLabel());
        assertEquals(TEST_DESC,  model.getDescription());
    }

    @Test
    public void testNoArgConstructorAllowsSetters() {
        UsecaseModel model = new UsecaseModel();
        model.setIri(TEST_IRI);
        model.setLabel(TEST_LABEL);
        model.setDescription(TEST_DESC);
        assertEquals(TEST_IRI,   model.getIri());
        assertEquals(TEST_LABEL, model.getLabel());
        assertEquals(TEST_DESC,  model.getDescription());
    }

    @Test
    public void testAddTestIncreasesCount() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        assertEquals(0, model.getTestIris().size());

        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-001");
        assertEquals(1, model.getTestIris().size());

        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-002");
        assertEquals(2, model.getTestIris().size());
    }

    @Test
    public void testAddDuplicateTestIsIgnored() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        String iri = "https://rs.tdwg.org/bdqtest/terms/test-001";
        model.addTest(iri);
        model.addTest(iri);  // duplicate
        assertEquals(1, model.getTestIris().size());
    }

    @Test
    public void testAddNullOrEmptyTestIsIgnored() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        model.addTest(null);
        model.addTest("");
        assertEquals(0, model.getTestIris().size());
    }

    @Test
    public void testGetTestIrisReturnsDefensiveCopy() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-001");

        List<String> copy = model.getTestIris();
        copy.add("external-modification");  // modify the returned copy

        // Internal list must be unaffected
        assertEquals(1, model.getTestIris().size());
    }

    @Test
    public void testToModelContainsUseCaseTriple() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        Model rdfModel = model.toModel();

        assertNotNull(rdfModel);
        assertTrue(rdfModel.size() > 0);
        assertTrue(
                rdfModel.contains(
                        rdfModel.createResource(TEST_IRI),
                        RDF.type,
                        BdqFfdq.UseCase),
                "Model should contain <usecase> a bdqffdq:UseCase");
    }

    @Test
    public void testToModelWithNoTestsContainsNoPolicies() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        Model rdfModel = model.toModel();

        ResIterator policies = rdfModel.listSubjectsWithProperty(RDF.type, BdqFfdq.Policy);
        assertEquals(0, policies.toList().size(), "No policies expected when no tests added");
    }

    @Test
    public void testToModelCreatesOnePolicyPerTest() {
        UsecaseModel model = new UsecaseModel(TEST_IRI, TEST_LABEL, TEST_DESC);
        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-001");
        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-002");
        model.addTest("https://rs.tdwg.org/bdqtest/terms/test-003");

        Model rdfModel = model.toModel();
        long policyCount = rdfModel.listSubjectsWithProperty(RDF.type, BdqFfdq.Policy)
                .toList().size();
        assertEquals(3, policyCount, "Expected one bdqffdq:Policy per test");
    }
}
