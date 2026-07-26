package org.filteredpush.bdq.usecasebuilder;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link Configuration}.
 */
public class ConfigurationTest {

    @Test
    public void testDefaultConstructorLoadsClasspathDefaults() {
        Configuration config = new Configuration();
        // The built-in config.properties defines both IRIs
        assertNotNull(config.getBdqFfdqOntologyIri(),
                "bdqffdq ontology IRI should be set by built-in defaults");
        assertNotNull(config.getBdqTestRdfIri(),
                "bdqtest RDF IRI should be set by built-in defaults");
    }

    @Test
    public void testUsecaseBaseIriHasDefault() {
        Configuration config = new Configuration();
        String baseIri = config.getUsecaseBaseIri();
        assertNotNull(baseIri);
        assertFalse(baseIri.isEmpty());
    }

    @Test
    public void testExtraRdfIrisIsEmptyByDefault() {
        Configuration config = new Configuration();
        List<String> extras = config.getExtraRdfIris();
        assertNotNull(extras);
        // The built-in config does not set any extra IRIs
        assertEquals(0, extras.size());
    }

    @Test
    public void testMissingConfigFileFallsBackToDefaults() {
        Configuration config = new Configuration("nonexistent-config.properties");
        // Should still return the built-in defaults
        assertNotNull(config.getBdqFfdqOntologyIri());
        assertNotNull(config.getBdqTestRdfIri());
    }

    @Test
    public void testGetPropertyWithDefault() {
        Configuration config = new Configuration();
        String value = config.getProperty("no.such.key", "fallback");
        assertEquals("fallback", value);
    }

    @Test
    public void testGetPropertyNullWhenAbsent() {
        Configuration config = new Configuration();
        assertNull(config.getProperty("absolutely.not.set"));
    }

    @Test
    public void testNullConfigPathUsesDefaults() {
        Configuration config = new Configuration(null);
        assertNotNull(config.getBdqFfdqOntologyIri());
    }
}
