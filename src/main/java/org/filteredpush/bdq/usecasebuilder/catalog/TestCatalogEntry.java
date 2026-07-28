package org.filteredpush.bdq.usecasebuilder.catalog;

/**
 * A lightweight holder for metadata about an existing BDQ test read from the
 * local test catalog.
 */
public class TestCatalogEntry {

    private final String iri;
    private final String label;
    private final String prefLabel;
    private final String type;
    private final String dimension;

    /**
     * Creates a new catalog entry.
     *
     * @param iri       the IRI of the test
     * @param label     the machine-readable label
     * @param prefLabel the human-readable preferred label
     * @param type      the test type string (e.g., {@code Validation})
     * @param dimension the data quality dimension (e.g., {@code Completeness})
     */
    public TestCatalogEntry(String iri, String label, String prefLabel,
                            String type, String dimension) {
        this.iri = iri;
        this.label = label;
        this.prefLabel = prefLabel;
        this.type = type;
        this.dimension = dimension;
    }

    /**
     * Returns the IRI of the test.
     *
     * @return the IRI string identifying this test
     */
    public String getIri() {
        return iri;
    }

    /**
     * Returns the machine-readable label.
     *
     * @return the machine-readable label for this test
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the human-readable preferred label.
     *
     * @return the skos:prefLabel for this test
     */
    public String getPrefLabel() {
        return prefLabel;
    }

    /**
     * Returns the test type string.
     *
     * @return the test type string (e.g., {@code "Validation"})
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the data quality dimension.
     *
     * @return the data quality dimension string (e.g., {@code "Completeness"})
     */
    public String getDimension() {
        return dimension;
    }

    @Override
    public String toString() {
        return (prefLabel != null && !prefLabel.isEmpty()) ? prefLabel : label;
    }
}
