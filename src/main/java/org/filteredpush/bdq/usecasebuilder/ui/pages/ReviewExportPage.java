package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.ExportService;
import org.filteredpush.bdq.usecasebuilder.service.ProjectStateSerializer;
import org.filteredpush.bdq.usecasebuilder.service.ShaclValidationService;
import org.filteredpush.bdq.usecasebuilder.service.TurtleExportService;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Wizard page – Review, Validate & Export.
 *
 * <p>Phase 3 additions:</p>
 * <ul>
 *   <li>SHACL-aligned pre-export validation with blocking errors / warnings.</li>
 *   <li>Turtle export with two modes:
 *     <ol>
 *       <li>Minimal – new use case + new tests only.</li>
 *       <li>Include existing – also includes selected existing test stubs.</li>
 *     </ol>
 *   </li>
 *   <li>Validation report ({@code validation_report.md}) written to output dir.</li>
 *   <li>Export blocked by default when blocking errors exist; warnings allow export.</li>
 * </ul>
 */
public class ReviewExportPage extends WizardPage {

    private JTextArea summaryArea;
    private JTextArea validationArea;
    private JLabel validationStatusLabel;
    private JRadioButton minimalModeBtn;
    private JRadioButton withExistingModeBtn;
    private JButton validateBtn;
    private JButton exportBtn;
    private JCheckBox overrideBlockingCheckbox;

    private final ValidationService validationService = new ValidationService();
    private final ShaclValidationService shaclService = new ShaclValidationService();
    private final TurtleExportService turtleExportService = new TurtleExportService();
    private final ProjectStateSerializer projectSerializer = new ProjectStateSerializer();
    private ShaclValidationService.ValidationReport lastShaclReport = null;

    public ReviewExportPage(ProjectState state) {
        super(state);
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return "Review, Validate & Export";
    }

    @Override
    public void onEnter() {
        markVisited();
        lastShaclReport = null;
        refreshSummary();
        updateExportButtonState();
    }

    @Override
    public void onLeave() {
        // read-only page
    }

    @Override
    public List<String> validatePage() {
        return new java.util.ArrayList<>();
    }

    @Override
    public CompletionStatus getCompletionStatus() {
        if (!isVisited()) {
            return CompletionStatus.NOT_STARTED;
        }
        if (lastShaclReport != null && lastShaclReport.isValid()) {
            return CompletionStatus.READY;
        }
        return CompletionStatus.IN_PROGRESS;
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Review, Validate &amp; Export.</b><br>"
                + "Run SHACL-aligned validation before export. Blocking errors prevent export. "
                + "Warnings are informational and allow export.<br>"
                + "Choose Turtle export mode: <b>Minimal</b> (new use case + new tests only) or "
                + "<b>Include Existing</b> (also includes selected existing test stubs).</html>");
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(intro, BorderLayout.NORTH);

        // Center: summary + validation results
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Summary area
        JLabel summaryLabel = new JLabel("Project Summary:");
        summaryLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        centerPanel.add(summaryLabel);

        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        summaryArea.setLineWrap(false);
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setPreferredSize(new java.awt.Dimension(800, 260));
        centerPanel.add(summaryScroll);

        // Validation status
        validationStatusLabel = new JLabel(" ");
        validationStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        centerPanel.add(validationStatusLabel);

        validationArea = new JTextArea();
        validationArea.setEditable(false);
        validationArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        validationArea.setLineWrap(true);
        validationArea.setWrapStyleWord(true);
        JScrollPane validationScroll = new JScrollPane(validationArea);
        validationScroll.setPreferredSize(new java.awt.Dimension(800, 160));
        centerPanel.add(validationScroll);

        add(new JScrollPane(centerPanel), BorderLayout.CENTER);

        // South: export mode + buttons
        add(buildSouthPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSouthPanel() {
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        // Export mode radio buttons
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.setBorder(BorderFactory.createTitledBorder("Turtle Export Mode"));
        minimalModeBtn = new JRadioButton("Minimal (new use case + new tests only)", true);
        withExistingModeBtn = new JRadioButton(
                "Include Existing (also includes selected existing test stubs)");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(minimalModeBtn);
        modeGroup.add(withExistingModeBtn);
        modePanel.add(minimalModeBtn);
        modePanel.add(withExistingModeBtn);
        south.add(modePanel);

        // Override checkbox
        overrideBlockingCheckbox = new JCheckBox(
                "Override: allow export even with blocking errors (export will be marked non-conformant)");
        overrideBlockingCheckbox.setForeground(Color.RED);
        overrideBlockingCheckbox.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        overrideBlockingCheckbox.setVisible(false);
        overrideBlockingCheckbox.addActionListener(e -> updateExportButtonState());
        south.add(overrideBlockingCheckbox);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        validateBtn = new JButton("Run Validation");
        exportBtn = new JButton("Export Now");
        JButton exportNowLegacyBtn = new JButton("Export (Markdown + JSON)");

        validateBtn.addActionListener(e -> runValidation());
        exportBtn.addActionListener(e -> exportTurtle());
        exportNowLegacyBtn.addActionListener(e -> exportLegacy());

        validateBtn.setToolTipText("Run SHACL-aligned validation checks before export");
        exportBtn.setToolTipText("Export RDF/Turtle to the configured output directory");
        exportNowLegacyBtn.setToolTipText("Export Markdown summary + JSON + conformance CSVs");

        JButton saveProjectBtn = new JButton("Save current project…");
        saveProjectBtn.setToolTipText("Save the current project state to a properties file for later re-use");
        saveProjectBtn.addActionListener(e -> saveProject());

        btnPanel.add(validateBtn);
        btnPanel.add(exportBtn);
        btnPanel.add(exportNowLegacyBtn);
        btnPanel.add(saveProjectBtn);
        south.add(btnPanel);

        return south;
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private void runValidation() {
        lastShaclReport = shaclService.validate(state);

        StringBuilder sb = new StringBuilder();
        if (lastShaclReport.getBlockingErrors().isEmpty()
                && lastShaclReport.getWarnings().isEmpty()) {
            sb.append("✅ All validation checks passed.\n");
            validationStatusLabel.setText("✅ No issues found – export is permitted.");
            validationStatusLabel.setForeground(new Color(0x2E7D32));
        } else {
            if (!lastShaclReport.getBlockingErrors().isEmpty()) {
                sb.append("❌ BLOCKING ERRORS (").append(lastShaclReport.errorCount())
                        .append("):\n");
                for (String err : lastShaclReport.getBlockingErrors()) {
                    sb.append("  • ").append(err).append('\n');
                }
                sb.append('\n');
            }
            if (!lastShaclReport.getWarnings().isEmpty()) {
                sb.append("⚠ WARNINGS (").append(lastShaclReport.warningCount()).append("):\n");
                for (String w : lastShaclReport.getWarnings()) {
                    sb.append("  • ").append(w).append('\n');
                }
            }
            if (!lastShaclReport.getBlockingErrors().isEmpty()) {
                validationStatusLabel.setText(
                        "❌ " + lastShaclReport.errorCount() + " blocking error(s) – export blocked.");
                validationStatusLabel.setForeground(Color.RED);
                overrideBlockingCheckbox.setVisible(true);
            } else {
                validationStatusLabel.setText(
                        "⚠ " + lastShaclReport.warningCount()
                        + " warning(s) – export is permitted with caution.");
                validationStatusLabel.setForeground(new Color(0xE65100));
                overrideBlockingCheckbox.setVisible(false);
            }
        }
        validationArea.setText(sb.toString());
        validationArea.setCaretPosition(0);
        updateExportButtonState();

        // Write validation report file
        try {
            File outputDir = new File(state.getOutputDirectory());
            shaclService.writeReport(lastShaclReport, outputDir);
        } catch (IOException ex) {
            // Non-fatal – just log
        }
    }

    private void exportTurtle() {
        boolean minimal = minimalModeBtn.isSelected();

        // Gate on blocking errors unless override checked
        if (lastShaclReport == null) {
            lastShaclReport = shaclService.validate(state);
        }
        if (!lastShaclReport.isValid() && !overrideBlockingCheckbox.isSelected()) {
            JOptionPane.showMessageDialog(this,
                    "Export blocked: " + lastShaclReport.errorCount() + " blocking error(s). "
                    + "Run Validation to see details, then fix issues or enable override.",
                    "Validation blocking export",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        File outputDir = new File(state.getOutputDirectory());
        try {
            File ttlFile;
            if (minimal) {
                ttlFile = turtleExportService.exportMinimal(state, outputDir);
            } else {
                TestCatalogService catalogService = new TestCatalogService();
                catalogService.loadCatalog();
                ttlFile = turtleExportService.exportWithExisting(state, outputDir, catalogService);
            }

            // Write validation report alongside
            File reportFile = shaclService.writeReport(lastShaclReport, outputDir);

            String message = "Exported Turtle:\n  " + ttlFile.getAbsolutePath()
                    + "\n\nValidation report:\n  " + reportFile.getAbsolutePath();
            if (!lastShaclReport.isValid() && overrideBlockingCheckbox.isSelected()) {
                message += "\n\n⚠ EXPORTED AS NON-CONFORMANT (override was enabled).";
            }
            JOptionPane.showMessageDialog(this, message, "Turtle Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Turtle export failed: " + ex.getMessage(),
                    "Export error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "BDQ project files (*.properties)", "properties"));
        // Start from the output directory if configured
        String outDir = state.getOutputDirectory();
        if (outDir != null && !outDir.trim().isEmpty()) {
            chooser.setCurrentDirectory(new File(outDir.trim()));
        }
        chooser.setSelectedFile(new File("bdq_project.properties"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".properties")) {
            file = new File(file.getAbsolutePath() + ".properties");
        }
        // Warn before overwriting an existing file
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "The file already exists:\n" + file.getAbsolutePath()
                    + "\n\nOverwrite?",
                    "Overwrite confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            projectSerializer.save(state, file);
            JOptionPane.showMessageDialog(this,
                    "Project saved to:\n" + file.getAbsolutePath(),
                    "Project saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save project file:\n" + ex.getMessage(),
                    "Save error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLegacy() {
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

    private void updateExportButtonState() {
        if (lastShaclReport == null) {
            exportBtn.setEnabled(true);
            exportBtn.setToolTipText("Run validation first, then export");
            return;
        }
        boolean canExport = lastShaclReport.isValid() || overrideBlockingCheckbox.isSelected();
        exportBtn.setEnabled(canExport);
        if (!canExport) {
            exportBtn.setToolTipText(
                    "Export blocked by " + lastShaclReport.errorCount() + " error(s). "
                    + "Fix issues or enable override checkbox.");
        } else {
            exportBtn.setToolTipText("Export RDF/Turtle to the configured output directory");
        }
    }

    // -----------------------------------------------------------------------
    // Summary
    // -----------------------------------------------------------------------

    private void refreshSummary() {
        StringBuilder sb = new StringBuilder();

        List<String> errors = validationService.validateForExport(state);
        if (!errors.isEmpty()) {
            sb.append("⚠ Validation issues: ").append(String.join("; ", errors))
                    .append("\n\n");
        }

        sb.append("=== Use Case ===\n");
        sb.append("Name            : ").append(nvl(state.getUseCaseDraft().getName())).append('\n');
        sb.append("Description     : ")
                .append(nvl(state.getUseCaseDraft().getDescription())).append('\n');
        sb.append("Fitness req.    : ")
                .append(truncate(state.getUseCaseDraft().getFitnessRequirementsText()))
                .append('\n');

        sb.append("\n=== Information Elements (")
                .append(state.getInformationElements().size()).append(") ===\n");
        for (InformationElementRef ref : state.getInformationElements()) {
            sb.append("  ").append(ref.getQname()).append("  [")
                    .append(ref.getRole() != null ? ref.getRole().getDisplayName() : "?")
                    .append("]\n");
        }

        sb.append("\n=== Selected Existing Tests (")
                .append(state.getSelectedExistingTestIris().size()).append(") ===\n");
        for (String iri : state.getSelectedExistingTestIris()) {
            sb.append("  ").append(iri).append('\n');
        }

        sb.append("\n=== New Tests Defined (")
                .append(state.getNewTestDrafts().size()).append(") ===\n");
        for (int i = 0; i < state.getNewTestDrafts().size(); i++) {
            TestDraft td = state.getNewTestDrafts().get(i);
            sb.append("  [").append(i + 1).append("] ").append(td).append('\n');
            sb.append("       Type     : ")
                    .append(td.getType() != null ? td.getType().getDisplayName() : "?").append('\n');
            sb.append("       ActedUpon: ")
                    .append(td.getActedUponElements().isEmpty()
                            ? nvl(td.getInformationElement())
                            : String.join(", ", td.getActedUponElements()))
                    .append('\n');
            sb.append("       Consulted: ")
                    .append(String.join(", ", td.getConsultedElements())).append('\n');
            sb.append("       Dimension: ").append(nvl(td.getDimension())).append('\n');
            sb.append("       Criterion: ").append(nvl(td.getCriterionOrEnhancement()))
                    .append('\n');
        }

        sb.append("\n=== Gap Analysis Matrix (")
                .append(state.getRequirementCoverageRows().size()).append(") ===\n");
        for (RequirementCoverage row : state.getRequirementCoverageRows()) {
            sb.append("  ").append(row.getRequirementId()).append(" ")
                    .append(nvl(row.getRequirementSummary())).append(" → ")
                    .append(row.computeStatus().getDisplayName()).append('\n');
        }

        sb.append("\n=== Output directory ===\n");
        sb.append("  ").append(nvl(state.getOutputDirectory())).append('\n');

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
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
