package org.filteredpush.bdq.usecasebuilder.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TestCatalogService}.
 */
public class TestCatalogServiceTest {

    @Test
    public void testLoadCatalogReturnsEntries() {
        TestCatalogService service = new TestCatalogService();
        int count = service.loadCatalog();
        assertTrue(count > 0, "Catalog should contain at least one entry");
    }

    @Test
    public void testEntriesHaveRequiredFields() {
        TestCatalogService service = new TestCatalogService();
        service.loadCatalog();
        for (TestCatalogEntry entry : service.getEntries()) {
            assertNotNull(entry.getIri(), "IRI must not be null");
            assertFalse(entry.getIri().isEmpty(), "IRI must not be empty");
            assertNotNull(entry.getLabel(), "Label must not be null");
        }
    }

    @Test
    public void testGetEntriesBeforeLoadIsEmpty() {
        TestCatalogService service = new TestCatalogService();
        assertTrue(service.getEntries().isEmpty(),
                "Entries should be empty before loadCatalog() is called");
    }

    @Test
    public void testParseCsvLineSimple() {
        List<String> fields = TestCatalogService.parseCsvLine(
                "iri,label,prefLabel,type,dimension");
        assertEquals(5, fields.size());
        assertEquals("iri", fields.get(0));
        assertEquals("dimension", fields.get(4));
    }

    @Test
    public void testParseCsvLineQuoted() {
        List<String> fields = TestCatalogService.parseCsvLine(
                "\"iri with, comma\",label,prefLabel,type,dimension");
        assertEquals(5, fields.size());
        assertEquals("iri with, comma", fields.get(0));
    }

    @Test
    public void testParseCsvLineEscapedQuote() {
        List<String> fields = TestCatalogService.parseCsvLine(
                "\"say \"\"hello\"\"\",label,prefLabel,type,dimension");
        assertEquals(5, fields.size());
        assertEquals("say \"hello\"", fields.get(0));
    }

    @Test
    public void testParseMalformedLineReturnsNull() {
        TestCatalogService service = new TestCatalogService();
        assertNull(service.parseLine("only,three,fields"),
                "Malformed line with fewer than 5 fields should return null");
    }

    @Test
    public void testCatalogContainsValidation() {
        TestCatalogService service = new TestCatalogService();
        service.loadCatalog();
        boolean hasValidation = service.getEntries().stream()
                .anyMatch(e -> "Validation".equals(e.getType()));
        assertTrue(hasValidation, "Catalog should contain at least one Validation test");
    }

    @Test
    public void testCatalogContainsAmendment() {
        TestCatalogService service = new TestCatalogService();
        service.loadCatalog();
        boolean hasAmendment = service.getEntries().stream()
                .anyMatch(e -> "Amendment".equals(e.getType()));
        assertTrue(hasAmendment, "Catalog should contain at least one Amendment test");
    }
}
