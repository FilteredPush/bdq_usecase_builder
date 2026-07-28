package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pre-export validation service aligned to bdqffdq SHACL-constraint behavior.
 *
 * <p>Checks are categorised as:</p>
 * <ul>
 *   <li><b>Blocking errors</b> – violations that must be resolved before
 *       export is allowed.</li>
 *   <li><b>Warnings</b> – issues that are flagged for attention but do not
 *       prevent export.</li>
 * </ul>
 *
 * <p>Call {@link #validate(ProjectState)} to obtain a {@link ValidationReport}
 * that can be used to gate export or display findings in the UI. Call
 * {@link #writeReport(ValidationReport, File)} to persist a human-readable
 * Markdown report alongside the Turtle output.</p>
 */
public class ShaclValidationService {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Validates the project state against bdqffdq-aligned SHACL constraints.
     *
     * @param state the project state to validate; must not be {@code null}
     * @return a {@link ValidationReport} containing blocking errors and warnings
     */
    public ValidationReport validate(ProjectState state) {
        if (state == null) {
            ValidationReport r = new ValidationReport();
            r.addBlockingError("Project state must not be null.");
            return r;
        }

        ValidationReport report = new ValidationReport();
        validateUseCase(state.getUseCaseDraft(), report);
        validateInformationElements(state, report);
        validateTests(state, report);
        return report;
    }

    /**
     * Writes a human-readable Markdown validation report to the given file.
     *
     * @param report    the validation report to write
     * @param outputDir directory where the report file will be created
     * @return the written report file
     * @throws IOException on write failure
     */
    public File writeReport(ValidationReport report, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }
        File reportFile = new File(outputDir, "validation_report.md");
        try (PrintWriter pw = new PrintWriter(reportFile, StandardCharsets.UTF_8.name())) {
            pw.println("# SHACL Validation Report");
            pw.println();
            pw.println("*Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT) + "*");
            pw.println();

            if (report.getBlockingErrors().isEmpty() && report.getWarnings().isEmpty()) {
                pw.println("✅ **All checks passed.** Export is permitted.");
                pw.println();
            } else {
                if (!report.getBlockingErrors().isEmpty()) {
                    pw.println("## ❌ Blocking Errors (" + report.getBlockingErrors().size() + ")");
                    pw.println();
                    pw.println("The following issues **must be resolved** before export:");
                    pw.println();
                    for (String err : report.getBlockingErrors()) {
                        pw.println("- " + err);
                    }
                    pw.println();
                }
                if (!report.getWarnings().isEmpty()) {
                    pw.println("## ⚠️ Warnings (" + report.getWarnings().size() + ")");
                    pw.println();
                    pw.println("These issues are flagged for attention but do not block export:");
                    pw.println();
                    for (String w : report.getWarnings()) {
                        pw.println("- " + w);
                    }
                    pw.println();
                }
                if (report.isValid()) {
                    pw.println("✅ Export is permitted (no blocking errors).");
                    pw.println();
                }
            }

            if (!report.getInfoMessages().isEmpty()) {
                pw.println("## ℹ️ Information");
                pw.println();
                for (String info : report.getInfoMessages()) {
                    pw.println("- " + info);
                }
                pw.println();
            }

            pw.println("---");
            pw.println("*bdq_usecase_builder SHACL-aligned pre-export validation*");
        }
        return reportFile;
    }

    // -----------------------------------------------------------------------
    // Validation checks
    // -----------------------------------------------------------------------

    private void validateUseCase(UseCaseDraft uc, ValidationReport report) {
        if (uc == null) {
            report.addBlockingError("Use case draft is null – no use case has been defined.");
            return;
        }
        // Blocking: Use case must have a name (maps to rdfs:label requirement)
        if (isBlank(uc.getName())) {
            report.addBlockingError(
                    "Use case is missing a name (rdfs:label). "
                    + "All bdqffdq:UseCase resources must have a label.");
        }
        // Warning: description recommended
        if (isBlank(uc.getDescription())) {
            report.addWarning(
                    "Use case has no description. Adding rdfs:comment is recommended "
                    + "for human readability of the exported Turtle.");
        }
        // Warning: fitness requirements text recommended
        if (isBlank(uc.getFitnessRequirementsText())) {
            report.addWarning(
                    "Use case has no fitness-for-use requirements text. "
                    + "Documenting why this use case exists is strongly recommended.");
        }
    }

    private void validateInformationElements(ProjectState state, ValidationReport report) {
        // Warning if no IEs: export will produce an incomplete use case
        if (state.getInformationElements().isEmpty()) {
            report.addWarning(
                    "No information elements defined. Tests without information elements "
                    + "may be incomplete in the exported Turtle.");
        }
    }

    private void validateTests(ProjectState state, ValidationReport report) {
        List<TestDraft> drafts = state.getNewTestDrafts();
        if (drafts.isEmpty() && state.getSelectedExistingTestIris().isEmpty()) {
            report.addWarning(
                    "No tests are defined or selected. The exported use case will not "
                    + "reference any data quality tests.");
        }
        for (int i = 0; i < drafts.size(); i++) {
            validateTestDraft(drafts.get(i), i + 1, report);
        }
    }

    private void validateTestDraft(TestDraft td, int seq, ValidationReport report) {
        String prefix = "Test #" + seq + " (" + label(td) + "): ";
        // Blocking: type required (bdqffdq tests must have a type)
        if (td.getType() == null) {
            report.addBlockingError(prefix + "test type is required. "
                    + "bdqffdq:DataQualityNeed subclass (Validation/Measure/Amendment/Issue) must be set.");
        }
        // Blocking: at least one label
        if (isBlank(td.getLabel()) && isBlank(td.getPrefLabel())) {
            report.addBlockingError(prefix + "neither rdfs:label nor skos:prefLabel is set. "
                    + "At least one label is required.");
        }
        // Warning: no information elements
        if (td.getActedUponElements().isEmpty() && td.getConsultedElements().isEmpty()
                && isBlank(td.getInformationElement())) {
            report.addWarning(prefix + "no information elements assigned. "
                    + "bdqffdq tests should reference at least one ActedUpon or Consulted element.");
        }
        // Warning: no expected response
        if (isBlank(td.getExpectedResponse()) && td.getExpectedResponseClauses().isEmpty()) {
            report.addWarning(prefix + "no expected response defined. "
                    + "A specification with hasExpectedResponse is expected by bdqffdq.");
        }
        // Warning: dimension recommended
        if (isBlank(td.getDimension())) {
            report.addWarning(prefix + "dimension not set. "
                    + "bdqffdq tests should reference a bdqdim dimension.");
        }
        // Warning: criterion/enhancement recommended
        if (isBlank(td.getCriterionOrEnhancement())) {
            report.addWarning(prefix + "criterion/enhancement not set. "
                    + "Validations should reference a bdqcrit criterion; "
                    + "Amendments should reference a bdqenh enhancement.");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String label(TestDraft td) {
        if (!isBlank(td.getLabel())) {
            return td.getLabel();
        }
        if (!isBlank(td.getPrefLabel())) {
            return td.getPrefLabel();
        }
        return "unnamed";
    }

    // -----------------------------------------------------------------------
    // ValidationReport inner class
    // -----------------------------------------------------------------------

    /**
     * Holds the results of a SHACL-aligned validation run.
     */
    public static class ValidationReport {

        private final List<String> blockingErrors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infoMessages = new ArrayList<>();

        /** Adds a blocking error. */
        public void addBlockingError(String message) {
            blockingErrors.add(message);
        }

        /** Adds a warning. */
        public void addWarning(String message) {
            warnings.add(message);
        }

        /** Adds an informational message. */
        public void addInfo(String message) {
            infoMessages.add(message);
        }

        /**
         * Returns {@code true} if there are no blocking errors (warnings are
         * allowed).
         */
        public boolean isValid() {
            return blockingErrors.isEmpty();
        }

        /**
         * Returns {@code true} if there are no blocking errors and no warnings.
         */
        public boolean isClean() {
            return blockingErrors.isEmpty() && warnings.isEmpty();
        }

        /** Returns an unmodifiable view of blocking errors. */
        public List<String> getBlockingErrors() {
            return Collections.unmodifiableList(blockingErrors);
        }

        /** Returns an unmodifiable view of warnings. */
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /** Returns an unmodifiable view of informational messages. */
        public List<String> getInfoMessages() {
            return Collections.unmodifiableList(infoMessages);
        }

        /**
         * Returns total count of blocking errors.
         *
         * @return the number of blocking errors
         */
        public int errorCount() {
            return blockingErrors.size();
        }

        /**
         * Returns total count of warnings.
         *
         * @return the number of warnings
         */
        public int warningCount() {
            return warnings.size();
        }
    }
}
