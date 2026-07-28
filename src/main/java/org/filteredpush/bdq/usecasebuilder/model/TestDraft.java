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
    /** True if this test consults an external source authority. */
    private boolean hasSourceAuthority = false;
    /** True if this test accepts one or more parameters. */
    private boolean hasParameters = false;

    /** Creates an empty test draft. */
    public TestDraft() {
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    /**
     * Returns the machine-oriented label following the
     * {@code TESTTYPE_IE_EVALUATION} naming convention.
     *
     * @return the machine-oriented label, or {@code null} if not yet set
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the machine-oriented label.
     *
     * @param label machine-oriented label following the {@code TESTTYPE_IE_EVALUATION} convention
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns {@code true} if the user has manually edited the label (suppresses auto-suggestion).
     *
     * @return {@code true} if the label has been manually overridden
     */
    public boolean isLabelUserOverridden() {
        return labelUserOverridden;
    }

    /**
     * Sets whether the label has been manually overridden by the user.
     *
     * @param labelUserOverridden {@code true} to suppress auto-suggestion of the label
     */
    public void setLabelUserOverridden(boolean labelUserOverridden) {
        this.labelUserOverridden = labelUserOverridden;
    }

    /**
     * Returns the human-readable preferred label (skos:prefLabel).
     *
     * @return the preferred label, or {@code null} if not yet set
     */
    public String getPrefLabel() {
        return prefLabel;
    }

    /**
     * Sets the human-readable preferred label.
     *
     * @param prefLabel the skos:prefLabel value for this test
     */
    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    /**
     * Returns {@code true} if the user has manually edited the prefLabel (suppresses auto-suggestion).
     *
     * @return {@code true} if the preferred label has been manually overridden
     */
    public boolean isPrefLabelUserOverridden() {
        return prefLabelUserOverridden;
    }

    /**
     * Sets whether the prefLabel has been manually overridden by the user.
     *
     * @param prefLabelUserOverridden {@code true} to suppress auto-suggestion of the preferred label
     */
    public void setPrefLabelUserOverridden(boolean prefLabelUserOverridden) {
        this.prefLabelUserOverridden = prefLabelUserOverridden;
    }

    /**
     * Returns the test type (Validation, Measure, Amendment, or Issue).
     *
     * @return the test type, or {@code null} if not yet set
     */
    public TestType getType() {
        return type;
    }

    /**
     * Sets the test type.
     *
     * @param type the test type (Validation, Measure, Amendment, or Issue)
     */
    public void setType(TestType type) {
        this.type = type;
    }

    /**
     * Returns the resource type (SingleRecord or MultiRecord).
     *
     * @return the resource type, or {@code null} if not yet set
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Sets the resource type.
     *
     * @param resourceType the resource type (SingleRecord or MultiRecord)
     */
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Returns the information element this test targets.
     *
     * @return the information element qualified name, or {@code null} if not set
     */
    public String getInformationElement() {
        return informationElement;
    }

    /**
     * Sets the information element this test targets.
     *
     * @param informationElement the qualified name of the information element
     */
    public void setInformationElement(String informationElement) {
        this.informationElement = informationElement;
    }

    /**
     * Returns the list of ActedUpon information elements for this test.
     * These are the primary elements the test acts upon.
     *
     * @return unmodifiable list of ActedUpon element qualified names; never {@code null}
     */
    public List<String> getActedUponElements() {
        return Collections.unmodifiableList(actedUponElements);
    }

    /**
     * Replaces the list of ActedUpon information elements.
     *
     * @param elements the new list of ActedUpon element qualified names; {@code null} clears the list
     */
    public void setActedUponElements(List<String> elements) {
        actedUponElements.clear();
        if (elements != null) {
            actedUponElements.addAll(elements);
        }
    }

    /**
     * Adds an ActedUpon information element.
     *
     * @param element the qualified name to add; {@code null} and duplicates are ignored
     */
    public void addActedUponElement(String element) {
        if (element != null && !element.trim().isEmpty() && !actedUponElements.contains(element)) {
            actedUponElements.add(element);
        }
    }

    /**
     * Removes an ActedUpon information element.
     *
     * @param element the qualified name to remove
     */
    public void removeActedUponElement(String element) {
        actedUponElements.remove(element);
    }

    /**
     * Returns the list of Consulted information elements for this test.
     * These are elements consulted but not directly modified by the test.
     *
     * @return unmodifiable list of Consulted element qualified names; never {@code null}
     */
    public List<String> getConsultedElements() {
        return Collections.unmodifiableList(consultedElements);
    }

    /**
     * Replaces the list of Consulted information elements.
     *
     * @param elements the new list of Consulted element qualified names; {@code null} clears the list
     */
    public void setConsultedElements(List<String> elements) {
        consultedElements.clear();
        if (elements != null) {
            consultedElements.addAll(elements);
        }
    }

    /**
     * Adds a Consulted information element.
     *
     * @param element the qualified name to add; {@code null} and duplicates are ignored
     */
    public void addConsultedElement(String element) {
        if (element != null && !element.trim().isEmpty() && !consultedElements.contains(element)) {
            consultedElements.add(element);
        }
    }

    /**
     * Removes a Consulted information element.
     *
     * @param element the qualified name to remove
     */
    public void removeConsultedElement(String element) {
        consultedElements.remove(element);
    }

    /**
     * Returns all information elements (both ActedUpon and Consulted) as a
     * combined list, for convenience in contexts where role is not relevant.
     *
     * @return unmodifiable list of all element qualified names; never {@code null}
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
     *
     * @return the dimension label, or {@code null} if not yet set
     */
    public String getDimension() {
        return dimension;
    }

    /**
     * Sets the data quality dimension.
     *
     * @param dimension the dimension label (e.g., {@code Completeness}, {@code Conformance})
     */
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    /**
     * Returns the criterion (for Validation/Measure/Issue) or enhancement
     * (for Amendment) descriptor.
     *
     * @return the criterion or enhancement descriptor, or {@code null} if not yet set
     */
    public String getCriterionOrEnhancement() {
        return criterionOrEnhancement;
    }

    /**
     * Sets the criterion or enhancement descriptor.
     *
     * @param criterionOrEnhancement the criterion (Validation/Measure/Issue) or enhancement (Amendment) value
     */
    public void setCriterionOrEnhancement(String criterionOrEnhancement) {
        this.criterionOrEnhancement = criterionOrEnhancement;
    }

    /**
     * Returns an optional reference to a BDQ use-case concept (bdquc).
     *
     * @return the use-case reference, or {@code null} if not set
     */
    public String getUseCaseReference() {
        return useCaseReference;
    }

    /**
     * Sets the optional use-case reference descriptor.
     *
     * @param useCaseReference the bdquc use-case reference value
     */
    public void setUseCaseReference(String useCaseReference) {
        this.useCaseReference = useCaseReference;
    }

    /**
     * Returns optional parameter/default profile text for this draft.
     *
     * @return the parameter/default profile text, or {@code null} if not set
     */
    public String getParameterDefaults() {
        return parameterDefaults;
    }

    /**
     * Sets optional parameter/default profile text.
     *
     * @param parameterDefaults the parameter/default profile text
     */
    public void setParameterDefaults(String parameterDefaults) {
        this.parameterDefaults = parameterDefaults;
    }

    /**
     * Returns the free-text expected response clause describing what the test
     * should return under various conditions.
     *
     * @return the expected response text, or {@code null} if not yet set
     */
    public String getExpectedResponse() {
        return expectedResponse;
    }

    /**
     * Sets the expected response text.
     *
     * @param expectedResponse free-text narrative of what the test should return
     */
    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    /**
     * Returns additional notes or assumptions for this test draft.
     *
     * @return the notes text, or {@code null} if not set
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets additional notes.
     *
     * @param notes additional notes or assumptions text
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns the structured expected-response clauses in authoring order.
     *
     * @return unmodifiable list of expected-response clauses; never {@code null}
     */
    public List<ExpectedResponseClause> getExpectedResponseClauses() {
        return Collections.unmodifiableList(expectedResponseClauses);
    }

    /**
     * Replaces structured expected-response clauses.
     *
     * @param clauses the new list of clauses; {@code null} clears the list
     */
    public void setExpectedResponseClauses(List<ExpectedResponseClause> clauses) {
        expectedResponseClauses.clear();
        if (clauses != null) {
            expectedResponseClauses.addAll(clauses);
        }
    }

    /**
     * Returns structured authority/default definitions for this test draft.
     *
     * @return unmodifiable list of authority/default definitions; never {@code null}
     */
    public List<AuthorityDefault> getAuthorityDefaults() {
        return Collections.unmodifiableList(authorityDefaults);
    }

    /**
     * Replaces authority/default definitions for this test draft.
     *
     * @param authorities the new list of authority/default definitions; {@code null} clears the list
     */
    public void setAuthorityDefaults(List<AuthorityDefault> authorities) {
        authorityDefaults.clear();
        if (authorities != null) {
            authorityDefaults.addAll(authorities);
        }
    }

    /**
     * Returns structured parameter definitions for this test draft.
     *
     * @return unmodifiable list of parameter definitions; never {@code null}
     */
    public List<ParameterDefinition> getParameterDefinitions() {
        return Collections.unmodifiableList(parameterDefinitions);
    }

    /**
     * Replaces parameter definitions for this test draft.
     *
     * @param parameters the new list of parameter definitions; {@code null} clears the list
     */
    public void setParameterDefinitions(List<ParameterDefinition> parameters) {
        parameterDefinitions.clear();
        if (parameters != null) {
            parameterDefinitions.addAll(parameters);
        }
    }

    /**
     * Returns conformance CSV row definitions for this test draft.
     *
     * @return unmodifiable list of conformance rows; never {@code null}
     */
    public List<ConformanceRow> getConformanceRows() {
        return Collections.unmodifiableList(conformanceRows);
    }

    /**
     * Replaces conformance CSV row definitions for this test draft.
     *
     * @param rows the new list of conformance rows; {@code null} clears the list
     */
    public void setConformanceRows(List<ConformanceRow> rows) {
        conformanceRows.clear();
        if (rows != null) {
            conformanceRows.addAll(rows);
        }
    }

    /**
     * Returns {@code true} if this test consults an external source authority.
     *
     * @return {@code true} if the test uses a source authority
     */
    public boolean isHasSourceAuthority() {
        return hasSourceAuthority;
    }

    /**
     * Sets whether this test consults an external source authority.
     *
     * @param hasSourceAuthority {@code true} if the test uses a source authority
     */
    public void setHasSourceAuthority(boolean hasSourceAuthority) {
        this.hasSourceAuthority = hasSourceAuthority;
    }

    /**
     * Returns {@code true} if this test accepts one or more parameters.
     *
     * @return {@code true} if the test accepts parameters
     */
    public boolean isHasParameters() {
        return hasParameters;
    }

    /**
     * Sets whether this test accepts parameters.
     *
     * @param hasParameters {@code true} if the test accepts one or more parameters
     */
    public void setHasParameters(boolean hasParameters) {
        this.hasParameters = hasParameters;
    }

    @Override
    public String toString() {
        return (label != null && !label.isEmpty()) ? label
                : (prefLabel != null ? prefLabel : "(unnamed test)");
    }
}
