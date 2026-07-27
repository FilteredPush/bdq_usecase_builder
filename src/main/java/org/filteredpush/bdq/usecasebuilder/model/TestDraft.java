package org.filteredpush.bdq.usecasebuilder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A draft definition for a new BDQ data quality test being authored by the
 * user.
 *
 * <p>Captures the minimal set of descriptors needed for Phase 1: label
 * anatomy, preferred label, test type, resource type, dimension,
 * criterion/enhancement, and the expected response narrative. Phase 2 extends
 * this with optional use-case references and parameter/default descriptors.
 * Phase 3 adds multi-valued actedUpon/consulted information element lists and
 * label-override tracking for auto-suggestion.</p>
 */
public class TestDraft {

    private String label;
    private boolean labelUserOverridden = false;
    private String prefLabel;
    private boolean prefLabelUserOverridden = false;
    private TestType type;
    private ResourceType resourceType;
    /** Legacy single-element field kept for backward-compat; use actedUponElements when possible. */
    private String informationElement;
    private final List<String> actedUponElements = new ArrayList<>();
    private final List<String> consultedElements = new ArrayList<>();
    private String dimension;
    private String criterionOrEnhancement;
    private String useCaseReference;
    private String parameterDefaults;
    private String expectedResponse;
    private String notes;
    private final List<ExpectedResponseClause> expectedResponseClauses = new ArrayList<>();
    private final List<AuthorityDefault> authorityDefaults = new ArrayList<>();
    private final List<ParameterDefinition> parameterDefinitions = new ArrayList<>();
    private final List<ConformanceRow> conformanceRows = new ArrayList<>();

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

    /** Returns {@code true} if the user has manually edited the label (suppresses auto-suggestion). */
    public boolean isLabelUserOverridden() {
        return labelUserOverridden;
    }

    /** Sets whether the label has been manually overridden by the user. */
    public void setLabelUserOverridden(boolean labelUserOverridden) {
        this.labelUserOverridden = labelUserOverridden;
    }

    /** Returns the human-readable preferred label (skos:prefLabel). */
    public String getPrefLabel() {
        return prefLabel;
    }

    /** Sets the human-readable preferred label. */
    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    /** Returns {@code true} if the user has manually edited the prefLabel (suppresses auto-suggestion). */
    public boolean isPrefLabelUserOverridden() {
        return prefLabelUserOverridden;
    }

    /** Sets whether the prefLabel has been manually overridden by the user. */
    public void setPrefLabelUserOverridden(boolean prefLabelUserOverridden) {
        this.prefLabelUserOverridden = prefLabelUserOverridden;
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
     * Returns the list of ActedUpon information elements for this test.
     * These are the primary elements the test acts upon.
     */
    public List<String> getActedUponElements() {
        return Collections.unmodifiableList(actedUponElements);
    }

    /** Replaces the list of ActedUpon information elements. */
    public void setActedUponElements(List<String> elements) {
        actedUponElements.clear();
        if (elements != null) {
            actedUponElements.addAll(elements);
        }
    }

    /** Adds an ActedUpon information element. */
    public void addActedUponElement(String element) {
        if (element != null && !element.trim().isEmpty() && !actedUponElements.contains(element)) {
            actedUponElements.add(element);
        }
    }

    /** Removes an ActedUpon information element. */
    public void removeActedUponElement(String element) {
        actedUponElements.remove(element);
    }

    /**
     * Returns the list of Consulted information elements for this test.
     * These are elements consulted but not directly modified by the test.
     */
    public List<String> getConsultedElements() {
        return Collections.unmodifiableList(consultedElements);
    }

    /** Replaces the list of Consulted information elements. */
    public void setConsultedElements(List<String> elements) {
        consultedElements.clear();
        if (elements != null) {
            consultedElements.addAll(elements);
        }
    }

    /** Adds a Consulted information element. */
    public void addConsultedElement(String element) {
        if (element != null && !element.trim().isEmpty() && !consultedElements.contains(element)) {
            consultedElements.add(element);
        }
    }

    /** Removes a Consulted information element. */
    public void removeConsultedElement(String element) {
        consultedElements.remove(element);
    }

    /**
     * Returns all information elements (both ActedUpon and Consulted) as a
     * combined list, for convenience in contexts where role is not relevant.
     */
    public List<String> getAllInformationElements() {
        List<String> all = new ArrayList<>();
        all.addAll(actedUponElements);
        for (String c : consultedElements) {
            if (!all.contains(c)) {
                all.add(c);
            }
        }
        return Collections.unmodifiableList(all);
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

    /** Returns the structured expected-response clauses in authoring order. */
    public List<ExpectedResponseClause> getExpectedResponseClauses() {
        return Collections.unmodifiableList(expectedResponseClauses);
    }

    /** Replaces structured expected-response clauses. */
    public void setExpectedResponseClauses(List<ExpectedResponseClause> clauses) {
        expectedResponseClauses.clear();
        if (clauses != null) {
            expectedResponseClauses.addAll(clauses);
        }
    }

    /** Returns structured authority/default definitions for this test draft. */
    public List<AuthorityDefault> getAuthorityDefaults() {
        return Collections.unmodifiableList(authorityDefaults);
    }

    /** Replaces authority/default definitions for this test draft. */
    public void setAuthorityDefaults(List<AuthorityDefault> authorities) {
        authorityDefaults.clear();
        if (authorities != null) {
            authorityDefaults.addAll(authorities);
        }
    }

    /** Returns structured parameter definitions for this test draft. */
    public List<ParameterDefinition> getParameterDefinitions() {
        return Collections.unmodifiableList(parameterDefinitions);
    }

    /** Replaces parameter definitions for this test draft. */
    public void setParameterDefinitions(List<ParameterDefinition> parameters) {
        parameterDefinitions.clear();
        if (parameters != null) {
            parameterDefinitions.addAll(parameters);
        }
    }

    /** Returns conformance CSV row definitions for this test draft. */
    public List<ConformanceRow> getConformanceRows() {
        return Collections.unmodifiableList(conformanceRows);
    }

    /** Replaces conformance CSV row definitions for this test draft. */
    public void setConformanceRows(List<ConformanceRow> rows) {
        conformanceRows.clear();
        if (rows != null) {
            conformanceRows.addAll(rows);
        }
    }

    @Override
    public String toString() {
        return (label != null && !label.isEmpty()) ? label
                : (prefLabel != null ? prefLabel : "(unnamed test)");
    }
}
