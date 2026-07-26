package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.ExportService;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Wizard page 6 – Review and export.
 *
 * <p>Displays a read-only summary of the entire project state and provides an
 * Export button to write the output artifacts immediately (without leaving the
 * wizard page). Validation errors, if any, are shown in a notice panel at the
 * top of the page.</p>
 */
public class ReviewExportPage extends WizardPage {

    private JTextArea summaryArea;
    private JLabel validationLabel;
    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the review and export page.
     *
     * @param state shared project state
     */
    public ReviewExportPage(ProjectState state) {
        super(state);
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Review & Export";
    }

    @Override
    public void onEnter() {
        refreshSummary();
    }

    @Override
    public void onLeave() {
        // read-only page; nothing to write back
    }

    @Override
    public List<String> validatePage() {
        // Allow finishing; export will also validate
        return new java.util.ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Review your use case package.</b><br>"
                        + "What: a final check of use-case, terms, reused tests, and new test drafts.<br>"
                        + "Why: this catches missing required details before sharing artifacts.<br>"
                        + "Convention: resolve warnings and keep labels/terms aligned with BDQ vocabularies.<br>"
                        + "Click <b>Export Now</b> to write the output files to the configured directory, "
                        + "or click <b>Finish</b> in the navigation bar to export and close the wizard."
                        + "</html>");
        add(intro, BorderLayout.NORTH);

        // Validation notice
        validationLabel = new JLabel(" ");
        validationLabel.setForeground(new java.awt.Color(0xCC0000));
        validationLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        // Summary area (read-only)
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        summaryArea.setLineWrap(false);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.add(validationLabel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Export button
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton exportButton = new JButton("Export Now");
        exportButton.setToolTipText("Write output files to the configured output directory");
        exportButton.addActionListener(e -> exportNow());
        south.add(exportButton);
        add(south, BorderLayout.SOUTH);
    }

    // -----------------------------------------------------------------------
    // Summary and export
    // -----------------------------------------------------------------------

    private void refreshSummary() {
        StringBuilder sb = new StringBuilder();

        // Validation check
        List<String> errors = validationService.validateForExport(state);
        if (errors.isEmpty()) {
            validationLabel.setText(" ");
        } else {
            validationLabel.setText("<html>⚠ Issues: " + String.join("; ", errors) + "</html>");
        }

        // Use case
        sb.append("=== Use Case ===\n");
        sb.append("Name            : ").append(nvl(state.getUseCaseDraft().getName())).append('\n');
        sb.append("Description     : ").append(nvl(state.getUseCaseDraft().getDescription())).append('\n');
        sb.append("Fitness req.    : ")
                .append(truncate(state.getUseCaseDraft().getFitnessRequirementsText())).append('\n');

        // Information elements
        sb.append("\n=== Information Elements (")
                .append(state.getInformationElements().size()).append(") ===\n");
        for (InformationElementRef ref : state.getInformationElements()) {
            sb.append("  ").append(ref.getQname()).append("  [")
                    .append(ref.getRole() != null ? ref.getRole().getDisplayName() : "?")
                    .append("]\n");
        }

        // Selected existing tests
        sb.append("\n=== Selected Existing Tests (")
                .append(state.getSelectedExistingTestIris().size()).append(") ===\n");
        for (String iri : state.getSelectedExistingTestIris()) {
            sb.append("  ").append(iri).append('\n');
        }

        // New tests
        sb.append("\n=== New Tests Defined (")
                .append(state.getNewTestDrafts().size()).append(") ===\n");
        for (int i = 0; i < state.getNewTestDrafts().size(); i++) {
            TestDraft td = state.getNewTestDrafts().get(i);
            sb.append("  [").append(i + 1).append("] ").append(td).append('\n');
            sb.append("       Type     : ")
                    .append(td.getType() != null ? td.getType().getDisplayName() : "?").append('\n');
            sb.append("       Dimension: ").append(nvl(td.getDimension())).append('\n');
            sb.append("       Criterion: ").append(nvl(td.getCriterionOrEnhancement())).append('\n');
            sb.append("       Use case : ").append(nvl(td.getUseCaseReference())).append('\n');
        }

        // Output
        sb.append("\n=== Output directory ===\n");
        sb.append("  ").append(nvl(state.getOutputDirectory())).append('\n');

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
    }

    private void exportNow() {
        ExportService exportService = new ExportService();
        try {
            String result = exportService.export(state);
            JOptionPane.showMessageDialog(this,
                    result,
                    "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Export failed: " + ex.getMessage(),
                    "Export error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }
}
