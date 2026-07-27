package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight required-field validation service for the Swing wizard.
 *
 * <p>Each {@code validate*} method checks the fields relevant to the
 * corresponding wizard page and returns a (possibly empty) list of human-
 * readable error messages. An empty list means the page passes validation.</p>
 */
public class ValidationService {

    /**
     * Validates the project setup fields on the Welcome page.
     *
     * <p>Required: output directory must be set to a non-blank value.</p>
     *
     * @param state the current project state
     * @return list of validation messages; empty if valid
     */
    public List<String> validateWelcomePage(ProjectState state) {
        List<String> errors = new ArrayList<>();
        if (state == null) {
            errors.add("Project state must not be null.");
            return errors;
        }
        if (isBlank(state.getOutputDirectory())) {
            errors.add("Output directory is required.");
        }
        return errors;
    }

    /**
     * Validates the Use Case page.
     *
     * <p>Required: use case name.</p>
     *
     * @param draft the use case draft to validate
     * @return list of validation messages; empty if valid
     */
    public List<String> validateUseCasePage(UseCaseDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("Use case draft must not be null.");
            return errors;
        }
        if (isBlank(draft.getName())) {
            errors.add("Use case name is required.");
        }
        return errors;
    }

    /**
     * Validates the Information Elements page.
     *
     * <p>At least one information element with a non-blank qualified name and a
     * non-null role is required.</p>
     *
     * @param state the current project state
     * @return list of validation messages; empty if valid
     */
    public List<String> validateInformationElementsPage(ProjectState state) {
        List<String> errors = new ArrayList<>();
        if (state == null) {
            errors.add("Project state must not be null.");
            return errors;
        }
        if (state.getInformationElements().isEmpty()) {
            errors.add("At least one information element is required.");
            return errors;
        }
        for (int i = 0; i < state.getInformationElements().size(); i++) {
            InformationElementRef ref = state.getInformationElements().get(i);
            if (isBlank(ref.getQname())) {
                errors.add("Information element #" + (i + 1) + " must have a qualified name.");
            }
            if (ref.getRole() == null) {
                errors.add("Information element #" + (i + 1) + " must have a role assigned.");
            }
        }
        return errors;
    }

    /**
     * Validates the New Test page for a single test draft.
     *
     * <p>Required: label (or preferred label) and test type.</p>
     *
     * @param draft the test draft to validate
     * @return list of validation messages; empty if valid
     */
    public List<String> validateNewTestPage(TestDraft draft) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("Test draft must not be null.");
            return errors;
        }
        if (isBlank(draft.getLabel()) && isBlank(draft.getPrefLabel())) {
            errors.add("A label or preferred label is required for the new test.");
        }
        if (draft.getType() == null) {
            errors.add("Test type (Validation, Measure, Amendment, or Issue) is required.");
        }
        if (isBlank(draft.getInformationElement())) {
            errors.add("Information element is required for each new test draft.");
        }
        return errors;
    }

    /**
     * Validates the complete project state before export.
     *
     * <p>Combines use-case, information-element, and (if present) new-test
     * validations. The Existing Tests page has no hard requirements.</p>
     *
     * @param state the current project state
     * @return list of all validation messages; empty if fully valid
     */
    public List<String> validateForExport(ProjectState state) {
        if (state == null) {
            return Collections.singletonList("Project state must not be null.");
        }
        List<String> errors = new ArrayList<>();
        errors.addAll(validateUseCasePage(state.getUseCaseDraft()));
        errors.addAll(validateInformationElementsPage(state));
        for (TestDraft draft : state.getNewTestDrafts()) {
            errors.addAll(validateNewTestPage(draft));
        }
        return errors;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
