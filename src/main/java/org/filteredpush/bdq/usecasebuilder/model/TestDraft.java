package org.filteredpush.bdq.usecasebuilder.model;

/**
 * A draft definition for a new BDQ data quality test being authored by the
 * user.
 *
 * <p>Captures the minimal set of descriptors needed for Phase 1: label
 * anatomy, preferred label, test type, resource type, dimension,
 * criterion/enhancement, and the expected response narrative. Phase 2 extends
 * this with optional use-case references and parameter/default descriptors.</p>
 */
public class TestDraft {

    private String label;
    private String prefLabel;
    private TestType type;
    private ResourceType resourceType;
    private String informationElement;
    private String dimension;
    private String criterionOrEnhancement;
    private String useCaseReference;
    private String parameterDefaults;
    private String expectedResponse;
    private String notes;

    /** Creates an empty test draft. */
    public TestDraft() {
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    /**
     * Returns the machine-oriented label following the
     * {@code TESTTYPE_IE_EVALUATION} naming convention.
     */
    public String getLabel() {
        return label;
    }

    /** Sets the machine-oriented label. */
    public void setLabel(String label) {
        this.label = label;
    }

    /** Returns the human-readable preferred label (skos:prefLabel). */
    public String getPrefLabel() {
        return prefLabel;
    }

    /** Sets the human-readable preferred label. */
    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    /** Returns the test type (Validation, Measure, Amendment, or Issue). */
    public TestType getType() {
        return type;
    }

    /** Sets the test type. */
    public void setType(TestType type) {
        this.type = type;
    }

    /** Returns the resource type (SingleRecord or MultiRecord). */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /** Sets the resource type. */
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /** Returns the information element this test targets. */
    public String getInformationElement() {
        return informationElement;
    }

    /** Sets the information element this test targets. */
    public void setInformationElement(String informationElement) {
        this.informationElement = informationElement;
    }

    /**
     * Returns the data quality dimension (e.g., {@code Completeness},
     * {@code Conformance}).
     */
    public String getDimension() {
        return dimension;
    }

    /** Sets the data quality dimension. */
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    /**
     * Returns the criterion (for Validation/Measure/Issue) or enhancement
     * (for Amendment) descriptor.
     */
    public String getCriterionOrEnhancement() {
        return criterionOrEnhancement;
    }

    /** Sets the criterion or enhancement descriptor. */
    public void setCriterionOrEnhancement(String criterionOrEnhancement) {
        this.criterionOrEnhancement = criterionOrEnhancement;
    }

    /** Returns an optional reference to a BDQ use-case concept (bdquc). */
    public String getUseCaseReference() {
        return useCaseReference;
    }

    /** Sets the optional use-case reference descriptor. */
    public void setUseCaseReference(String useCaseReference) {
        this.useCaseReference = useCaseReference;
    }

    /** Returns optional parameter/default profile text for this draft. */
    public String getParameterDefaults() {
        return parameterDefaults;
    }

    /** Sets optional parameter/default profile text. */
    public void setParameterDefaults(String parameterDefaults) {
        this.parameterDefaults = parameterDefaults;
    }

    /**
     * Returns the free-text expected response clause describing what the test
     * should return under various conditions.
     */
    public String getExpectedResponse() {
        return expectedResponse;
    }

    /** Sets the expected response text. */
    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    /** Returns additional notes or assumptions for this test draft. */
    public String getNotes() {
        return notes;
    }

    /** Sets additional notes. */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return (label != null && !label.isEmpty()) ? label
                : (prefLabel != null ? prefLabel : "(unnamed test)");
    }
}
