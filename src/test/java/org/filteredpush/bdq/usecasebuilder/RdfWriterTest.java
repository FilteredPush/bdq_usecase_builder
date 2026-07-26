package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RdfWriter}.
 */
public class RdfWriterTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Model createTestModel() {
        Model model = ModelFactory.createDefaultModel();
        // Use a valid absolute IRI (not a urn:uuid: with a non-UUID suffix, which
        // the RDF/XML serialiser rejects).
        Resource useCase = model.createResource(
                "https://example.org/usecase/writer-test-001");
        useCase.addProperty(RDF.type, BdqFfdq.UseCase);
        useCase.addProperty(RDFS.label, "Writer Test Use Case");
        return model;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    public void testWriteTurtleCreatesNonEmptyFile() throws Exception {
        RdfWriter writer = new RdfWriter();
        Model model = createTestModel();
        File output = tempDir.resolve("output.ttl").toFile();

        writer.write(model, output.getAbsolutePath(), "TURTLE");

        assertTrue(output.exists(), "Output file should exist");
        assertTrue(output.length() > 0, "Output file should not be empty");
    }

    @Test
    public void testWriteTtlAliasWorks() throws Exception {
        RdfWriter writer = new RdfWriter();
        File output = tempDir.resolve("output-ttl.ttl").toFile();
        writer.write(createTestModel(), output.getAbsolutePath(), "TTL");
        assertTrue(output.exists() && output.length() > 0);
    }

    @Test
    public void testWriteRdfXml() throws Exception {
        RdfWriter writer = new RdfWriter();
        File output = tempDir.resolve("output.rdf").toFile();

        writer.write(createTestModel(), output.getAbsolutePath(), "RDF/XML");

        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }

    @Test
    public void testWriteNTriples() throws Exception {
        RdfWriter writer = new RdfWriter();
        File output = tempDir.resolve("output.nt").toFile();

        writer.write(createTestModel(), output.getAbsolutePath(), "N-TRIPLES");

        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }

    @Test
    public void testWriteUnknownFormatFallsBackToTurtle() {
        RdfWriter writer = new RdfWriter();
        File output = tempDir.resolve("output-unknown.txt").toFile();

        assertDoesNotThrow(() ->
                writer.write(createTestModel(), output.getAbsolutePath(), "UNKNOWN_FORMAT"));
        assertTrue(output.exists());
    }

    @Test
    public void testWriteToStringTurtleContainsExpectedContent() {
        RdfWriter writer = new RdfWriter();
        String result = writer.writeToString(createTestModel(), "TURTLE");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // The serialisation should reference the bdqffdq namespace or UseCase term
        assertTrue(result.contains("UseCase") || result.contains("bdqffdq"),
                "Turtle output should reference bdqffdq:UseCase");
    }

    @Test
    public void testWriteToStringNTriples() {
        RdfWriter writer = new RdfWriter();
        String result = writer.writeToString(createTestModel(), "N-TRIPLES");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testWriteCreatesIntermediateDirectories() throws Exception {
        RdfWriter writer = new RdfWriter();
        File output = tempDir.resolve("subdir/nested/output.ttl").toFile();

        writer.write(createTestModel(), output.getAbsolutePath(), "TURTLE");

        assertTrue(output.exists());
    }
}
