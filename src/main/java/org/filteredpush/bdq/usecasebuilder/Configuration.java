package org.filteredpush.bdq.usecasebuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles configuration for the BDQ Use Case Builder.
 *
 * <p>Reads a Java properties file that specifies known IRI locations for the
 * bdqffdq ontology, the bdqtest RDF, and any other supporting vocabularies.
 * Falls back to built-in defaults (shipped as {@code /config.properties} on
 * the classpath) when a user-supplied file is absent or cannot be read.</p>
 *
 * <p>Configuration keys:</p>
 * <ul>
 *   <li>{@value #KEY_BDQFFDQ_IRI} – IRI of the bdqffdq OWL ontology</li>
 *   <li>{@value #KEY_BDQTEST_IRI} – IRI of the bdqtest RDF file</li>
 *   <li>{@value #KEY_EXTRA_IRIS}  – comma-separated list of additional IRIs to load</li>
 *   <li>{@value #KEY_USECASE_BASE_IRI} – base IRI used when minting new use case IRIs</li>
 * </ul>
 */
public class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    /** Configuration key for the bdqffdq OWL ontology IRI. */
    public static final String KEY_BDQFFDQ_IRI = "bdqffdq.ontology.iri";

    /** Configuration key for the bdqtest RDF IRI. */
    public static final String KEY_BDQTEST_IRI = "bdqtest.rdf.iri";

    /** Configuration key for additional RDF IRIs (comma-separated). */
    public static final String KEY_EXTRA_IRIS = "extra.rdf.iris";

    /** Configuration key for the base IRI used when minting new use case IRIs. */
    public static final String KEY_USECASE_BASE_IRI = "usecase.base.iri";

    private final Properties properties;

    /**
     * Creates a Configuration using only the built-in classpath defaults.
     */
    public Configuration() {
        this.properties = loadDefaults();
    }

    /**
     * Creates a Configuration, loading from the given file path and falling back to
     * built-in defaults for any keys not present in that file.
     *
     * @param configFilePath path to a Java properties file; may be {@code null}
     */
    public Configuration(String configFilePath) {
        this.properties = loadDefaults();
        if (configFilePath != null && !configFilePath.isEmpty()) {
            loadFromFile(configFilePath);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Properties loadDefaults() {
        Properties defaults = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                defaults.load(is);
                logger.debug("Loaded built-in config.properties from classpath");
            } else {
                logger.warn("Built-in config.properties not found on classpath");
            }
        } catch (IOException e) {
            logger.warn("Could not load built-in config.properties: {}", e.getMessage());
        }
        return defaults;
    }

    private void loadFromFile(String filePath) {
        File configFile = new File(filePath);
        if (configFile.exists() && configFile.isFile()) {
            try (InputStream is = new FileInputStream(configFile)) {
                properties.load(is);
                logger.info("Loaded configuration from {}", filePath);
            } catch (IOException e) {
                logger.warn("Could not read configuration file {}: {}", filePath, e.getMessage());
            }
        } else {
            logger.info("Configuration file '{}' not found; using built-in defaults", filePath);
        }
    }

    // -----------------------------------------------------------------------
    // Public accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the IRI of the bdqffdq OWL ontology, as configured.
     *
     * @return IRI string, or {@code null} if not set
     */
    public String getBdqFfdqOntologyIri() {
        return properties.getProperty(KEY_BDQFFDQ_IRI);
    }

    /**
     * Returns the IRI of the bdqtest RDF file, as configured.
     *
     * @return IRI string, or {@code null} if not set
     */
    public String getBdqTestRdfIri() {
        return properties.getProperty(KEY_BDQTEST_IRI);
    }

    /**
     * Returns the base IRI to use when minting new use-case IRIs.
     *
     * @return base IRI string; defaults to {@code "urn:uuid:"}
     */
    public String getUsecaseBaseIri() {
        return properties.getProperty(KEY_USECASE_BASE_IRI, "urn:uuid:");
    }

    /**
     * Returns any additional RDF IRIs listed in the {@value #KEY_EXTRA_IRIS} key.
     *
     * @return mutable list of IRI strings; never {@code null}, may be empty
     */
    public List<String> getExtraRdfIris() {
        String extras = properties.getProperty(KEY_EXTRA_IRIS, "");
        List<String> result = new ArrayList<>();
        if (extras != null && !extras.trim().isEmpty()) {
            for (String iri : extras.split(",")) {
                String trimmed = iri.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    /**
     * Returns the value associated with {@code key}.
     *
     * @param key the property key
     * @return the value, or {@code null} if not set
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Returns the value associated with {@code key}, or {@code defaultValue} if absent.
     *
     * @param key          the property key
     * @param defaultValue the fallback value
     * @return the value, or {@code defaultValue}
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
