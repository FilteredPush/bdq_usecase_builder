package org.filteredpush.bdq.usecasebuilder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Paths;

/**
 * Central in-memory state object that accumulates all authoring inputs across
 * the wizard session.
 *
 * <p>A single {@code ProjectState} instance is created when the wizard starts
 * and is passed through each page. Pages read and write their portions of the
 * state. The {@code ExportService} reads the completed state to produce output
 * artifacts.</p>
 */
public class ProjectState {

    private String outputDirectory;
    private UseCaseDraft useCaseDraft;
    private final List<InformationElementRef> informationElements = new ArrayList<>();
    private final List<String> selectedExistingTestIris = new ArrayList<>();
    private final List<TestDraft> newTestDrafts = new ArrayList<>();
    private final List<RequirementCoverage> requirementCoverageRows = new ArrayList<>();
    /** URI of an additional vocabulary to load IE terms from (optional). */
    private String additionalVocabUri;
    /** File path or URI of an additional RDF document to load tests and use cases from (optional). */
    private String additionalRdfSource;

    /** Creates an empty project state. */
    public ProjectState() {
        this.useCaseDraft = new UseCaseDraft();
        this.outputDirectory = Paths.get(System.getProperty("user.dir"), "output").toString();
    }

    // -----------------------------------------------------------------------
    // Output directory
    // -----------------------------------------------------------------------

    /** Returns the user-chosen output directory for exported artifacts. */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /** Sets the output directory for exported artifacts. */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // -----------------------------------------------------------------------
    // Use case draft
    // -----------------------------------------------------------------------

    /**
     * Returns the current use case draft. Never {@code null}.
     *
     * @return the current {@link UseCaseDraft}; never {@code null}
     */
    public UseCaseDraft getUseCaseDraft() {
        return useCaseDraft;
    }

    /**
     * Replaces the current use case draft.
     *
     * @param useCaseDraft the new use case draft to store
     */
    public void setUseCaseDraft(UseCaseDraft useCaseDraft) {
        this.useCaseDraft = useCaseDraft;
    }

    // -----------------------------------------------------------------------
    // Information elements
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the information element list.
     *
     * @return list of {@link InformationElementRef}; never {@code null}
     */
    public List<InformationElementRef> getInformationElements() {
        return Collections.unmodifiableList(informationElements);
    }

    /**
     * Adds an information element reference to the list.
     *
     * @param ref the element to add; {@code null} values are ignored
     */
    public void addInformationElement(InformationElementRef ref) {
        if (ref != null) {
            informationElements.add(ref);
        }
    }

    /**
     * Removes the information element at the given index.
     *
     * @param index zero-based index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeInformationElement(int index) {
        informationElements.remove(index);
    }

    /** Clears all information elements. */
    public void clearInformationElements() {
        informationElements.clear();
    }

    // -----------------------------------------------------------------------
    // Selected existing tests
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the selected existing test IRIs.
     *
     * @return list of IRI strings; never {@code null}
     */
    public List<String> getSelectedExistingTestIris() {
        return Collections.unmodifiableList(selectedExistingTestIris);
    }

    /**
     * Adds an existing test IRI to the selection. Duplicates are silently
     * ignored.
     *
     * @param iri the test IRI to add; {@code null}/empty values are ignored
     */
    public void addSelectedExistingTest(String iri) {
        if (iri != null && !iri.isEmpty() && !selectedExistingTestIris.contains(iri)) {
            selectedExistingTestIris.add(iri);
        }
    }

    /**
     * Removes an existing test IRI from the selection.
     *
     * @param iri the test IRI to remove
     */
    public void removeSelectedExistingTest(String iri) {
        selectedExistingTestIris.remove(iri);
    }

    /** Clears all selected existing tests. */
    public void clearSelectedExistingTests() {
        selectedExistingTestIris.clear();
    }

    /**
     * Sets the full list of selected existing test IRIs, replacing any previous selection.
     *
     * @param iris the list of test IRIs to select; {@code null} clears the selection
     */
    public void setSelectedExistingTestIris(List<String> iris) {
        selectedExistingTestIris.clear();
        if (iris != null) {
            selectedExistingTestIris.addAll(iris);
        }
    }

    // -----------------------------------------------------------------------
    // New test drafts
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the new test drafts.
     *
     * @return list of {@link TestDraft}; never {@code null}
     */
    public List<TestDraft> getNewTestDrafts() {
        return Collections.unmodifiableList(newTestDrafts);
    }

    /**
     * Adds a new test draft.
     *
     * @param draft the draft to add; {@code null} values are ignored
     */
    public void addNewTestDraft(TestDraft draft) {
        if (draft != null) {
            newTestDrafts.add(draft);
        }
    }

    /**
     * Removes the new test draft at the given index.
     *
     * @param index zero-based index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeNewTestDraft(int index) {
        newTestDrafts.remove(index);
    }

    /** Clears all new test drafts. */
    public void clearNewTestDrafts() {
        newTestDrafts.clear();
    }

    // -----------------------------------------------------------------------
    // Gap analysis matrix rows
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of requirement coverage rows.
     *
     * @return unmodifiable list of {@link RequirementCoverage} rows; never {@code null}
     */
    public List<RequirementCoverage> getRequirementCoverageRows() {
        return Collections.unmodifiableList(requirementCoverageRows);
    }

    /**
     * Replaces requirement coverage rows.
     *
     * @param rows the new list of coverage rows; {@code null} clears the list
     */
    public void setRequirementCoverageRows(List<RequirementCoverage> rows) {
        requirementCoverageRows.clear();
        if (rows != null) {
            requirementCoverageRows.addAll(rows);
        }
    }

    // -----------------------------------------------------------------------
    // Additional vocabulary URI
    // -----------------------------------------------------------------------

    /**
     * Returns the URI of an additional vocabulary to load IE terms from, or {@code null}.
     *
     * @return the additional vocabulary URI, or {@code null} if not set
     */
    public String getAdditionalVocabUri() {
        return additionalVocabUri;
    }

    /**
     * Sets the URI of an additional IE vocabulary to load.
     *
     * @param additionalVocabUri the URI of the additional vocabulary; may be {@code null}
     */
    public void setAdditionalVocabUri(String additionalVocabUri) {
        this.additionalVocabUri = additionalVocabUri;
    }

    // -----------------------------------------------------------------------
    // Additional RDF source
    // -----------------------------------------------------------------------

    /**
     * Returns the file path or URI of an additional RDF document to load tests and
     * use cases from, or {@code null}.
     *
     * @return the additional RDF source path or URI, or {@code null} if not set
     */
    public String getAdditionalRdfSource() {
        return additionalRdfSource;
    }

    /**
     * Sets the file path or URI of an additional RDF document.
     *
     * @param additionalRdfSource the file path or URI of the additional RDF document; may be {@code null}
     */
    public void setAdditionalRdfSource(String additionalRdfSource) {
        this.additionalRdfSource = additionalRdfSource;
    }
}
