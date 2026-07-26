package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RdfLoader}.
 */
public class RdfLoaderTest {

    private RdfLoader loaderWithNoRemote;

    @BeforeEach
    public void setUp() {
        // Use a configuration that has no remote IRIs so tests do not make
        // network calls.
        Configuration emptyConfig = new Configuration() {
            @Override public String getBdqFfdqOntologyIri() { return null; }
            @Override public String getBdqTestRdfIri()      { return null; }
            @Override public List<String> getExtraRdfIris() { return Collections.emptyList(); }
        };
        loaderWithNoRemote = new RdfLoader(emptyConfig);
    }

    @Test
    public void testCreateEmptyModelIsEmpty() {
        Model model = loaderWithNoRemote.createEmptyModel();
        assertNotNull(model);
        assertEquals(0, model.size());
    }

    @Test
    public void testLoadAllWithNoSourcesReturnsEmptyModel() {
        Model model = loaderWithNoRemote.loadAll(Collections.emptyList());
        assertNotNull(model);
        assertEquals(0, model.size());
    }

    @Test
    public void testLoadLocalTurtleFile() {
        Model model = loaderWithNoRemote.createEmptyModel();
        // Resolve the test fixture as a file: URI that Jena can read
        String testFileUri = getClass().getResource("/test-data.ttl").toString();
        loaderWithNoRemote.loadIntoModel(model, testFileUri);
        assertTrue(model.size() > 0, "Model should contain triples from test-data.ttl");
    }

    @Test
    public void testLoadAllWithLocalFile() {
        String testFileUri = getClass().getResource("/test-data.ttl").toString();
        Model model = loaderWithNoRemote.loadAll(Collections.singletonList(testFileUri));
        assertTrue(model.size() > 0);
    }

    @Test
    public void testLoadInvalidSourceDoesNotThrow() {
        Model model = loaderWithNoRemote.createEmptyModel();
        // An unreachable host must not propagate an exception
        assertDoesNotThrow(() ->
                loaderWithNoRemote.loadIntoModel(model,
                        "http://invalid.nonexistent.host.example/rdf.ttl"));
        // Model must remain usable
        assertNotNull(model);
    }

    @Test
    public void testLoadMalformedRdfDoesNotThrow() {
        Model model = loaderWithNoRemote.createEmptyModel();
        // Passing a raw string that is not a valid IRI / file path must not throw
        assertDoesNotThrow(() ->
                loaderWithNoRemote.loadIntoModel(model, "this-is-not-valid-rdf-or-iri"));
        assertNotNull(model);
    }
}
