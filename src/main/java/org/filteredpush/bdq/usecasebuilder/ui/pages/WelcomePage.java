package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.filteredpush.bdq.usecasebuilder.service.ProjectStateSerializer;
import org.filteredpush.bdq.usecasebuilder.service.RdfProjectLoader;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;

/**
 * Wizard page 1 – Welcome and project setup.
 *
 * <p>Phase 3 additions:</p>
 * <ul>
 *   <li>Load an existing project file (saves/restores output dir, use case, and information
 *       elements via a simple properties format).</li>
 *   <li>Additional IE vocabulary URI – a machine-readable vocabulary whose terms will be
 *       available on the Information Elements page.</li>
 *   <li>Load tests and use cases from an additional RDF document.</li>
 *   <li>Select an existing use case from the loaded RDF document to pre-fill the use case page.</li>
 * </ul>
 */
public class WelcomePage extends WizardPage {

    private JTextField outputDirField;
    private JTextField vocabUriField;
    private JTextField rdfSourceField;
    private JComboBox<String> existingUseCaseCombo;

    private final ValidationService validationService = new ValidationService();
    private final ProjectStateSerializer serializer = new ProjectStateSerializer();
    private final RdfProjectLoader rdfLoader = new RdfProjectLoader();
    private final TestCatalogService catalogService;

    private List<UseCaseDraft> loadedUseCases;

    /**
     * Creates the welcome page.
     *
     * @param state          shared project state
     * @param catalogService test catalog service to populate with tests loaded from RDF
     */
    public WelcomePage(ProjectState state, TestCatalogService catalogService) {
        super(state);
        this.catalogService = catalogService;
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Welcome – Project Setup";
    }

    @Override
    public void onEnter() {
        String dir = state.getOutputDirectory();
        outputDirField.setText(dir != null ? dir : "");
        vocabUriField.setText(nvl(state.getAdditionalVocabUri()));
        rdfSourceField.setText(nvl(state.getAdditionalRdfSource()));
    }

    @Override
    public void onLeave() {
        state.setOutputDirectory(outputDirField.getText().trim());
        String vocabUri = vocabUriField.getText().trim();
        state.setAdditionalVocabUri(vocabUri.isEmpty() ? null : vocabUri);
        String rdfSrc = rdfSourceField.getText().trim();
        state.setAdditionalRdfSource(rdfSrc.isEmpty() ? null : rdfSrc);
        // Apply selected use case if user picked one from the loaded RDF
        applySelectedUseCaseToState();
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return validationService.validateWelcomePage(state);
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Welcome text
        JLabel welcomeLabel = new JLabel(
                "<html><h2>Welcome to the BDQ Use Case Builder</h2>"
                        + "<p>What: capture a BDQ use-case package (scope, terms, tests, and export).</p>"
                        + "<p>Why: a consistent package makes test implementation and review easier.</p>"
                        + "<p>Convention: output defaults to <tt>output/</tt> under your current launch directory "
                        + "and can be changed below.</p>"
                        + "<p>This wizard will guide you through:</p>"
                        + "<ul>"
                        + "<li>Defining a BDQ use case</li>"
                        + "<li>Identifying the information elements involved</li>"
                        + "<li>Selecting existing BDQ tests</li>"
                        + "<li>Defining new BDQ tests</li>"
                        + "<li>Exporting a summary of your work</li>"
                        + "</ul>"
                        + "<p>Click <b>Next</b> to begin, or use the options below to load an existing project "
                        + "or extend the available vocabulary.</p>"
                        + "</html>");
        welcomeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        add(welcomeLabel, BorderLayout.NORTH);

        // Project setup form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Project setup"));

        int row = 0;

        // Row: Output directory
        outputDirField = new JTextField(30);
        outputDirField.setToolTipText("Directory where exported files will be written");
        JButton browseDirButton = new JButton("Browse…");
        browseDirButton.addActionListener(e -> browseForDirectory(outputDirField));
        addFieldRow(form, "Output directory:", outputDirField, browseDirButton, row++);

        // Row: Load existing project
        JTextField projectFileField = new JTextField(30);
        projectFileField.setEditable(false);
        projectFileField.setToolTipText(
                "Load a previously saved project file (restores output dir, use case, and information elements)");
        JButton loadProjectButton = new JButton("Load…");
        loadProjectButton.addActionListener(e -> loadProject(projectFileField));
        addFieldRow(form, "Load project:", projectFileField, loadProjectButton, row++);

        // Row: Additional IE vocabulary URI
        vocabUriField = new JTextField(30);
        vocabUriField.setToolTipText(
                "URI of a machine-readable vocabulary (RDF/OWL) whose terms will be added to the IE picker");
        JButton clearVocabButton = new JButton("Clear");
        clearVocabButton.addActionListener(e -> vocabUriField.setText(""));
        addFieldRow(form, "Additional IE vocabulary URI:", vocabUriField, clearVocabButton, row++);

        // Row: Additional RDF source (tests + use cases)
        rdfSourceField = new JTextField(30);
        rdfSourceField.setToolTipText(
                "Path or URI of an RDF document to load additional tests and use cases from");
        JButton browseRdfButton = new JButton("Browse…");
        browseRdfButton.addActionListener(e -> {
            browseForFile(rdfSourceField, "RDF files", "ttl", "rdf", "owl", "jsonld", "n3", "nt");
        });
        addFieldRow(form, "Additional RDF source:", rdfSourceField, browseRdfButton, row++);

        // Row: "Load RDF" action button
        GridBagConstraints loadRdfC = new GridBagConstraints();
        loadRdfC.gridx = 1; loadRdfC.gridy = row++;
        loadRdfC.anchor = GridBagConstraints.WEST;
        loadRdfC.insets = new Insets(0, 0, 6, 6);
        JButton loadRdfButton = new JButton("Load use cases and tests from RDF");
        loadRdfButton.setToolTipText(
                "Parse the RDF source to find use cases and tests; "
                + "tests are added to the catalog, the selected use case pre-fills step 2");
        loadRdfButton.addActionListener(e -> loadFromRdf());
        form.add(loadRdfButton, loadRdfC);

        // Row: Existing use case selector (populated after loading RDF)
        existingUseCaseCombo = new JComboBox<>();
        existingUseCaseCombo.addItem("(none – start fresh)");
        existingUseCaseCombo.setToolTipText(
                "Select an existing use case from the loaded RDF document or loaded project");
        addFieldRow(form, "Select existing use case:", existingUseCaseCombo, null, row++);

        add(form, BorderLayout.CENTER);
    }

    private void addFieldRow(JPanel form, String labelText,
                              java.awt.Component field,
                              java.awt.Component button,
                              int gridy) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = gridy;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(6, 6, 6, 6);
        form.add(new JLabel(labelText), labelGbc);

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.gridy = gridy;
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.insets = new Insets(6, 0, 6, 6);
        form.add(field, fieldGbc);

        if (button != null) {
            GridBagConstraints buttonGbc = new GridBagConstraints();
            buttonGbc.gridx = 2;
            buttonGbc.gridy = gridy;
            buttonGbc.insets = new Insets(6, 0, 6, 6);
            form.add(button, buttonGbc);
        }
    }

    // -----------------------------------------------------------------------
    // Action handlers
    // -----------------------------------------------------------------------

    private void browseForDirectory(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!targetField.getText().trim().isEmpty()) {
            chooser.setCurrentDirectory(new File(targetField.getText().trim()));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseForFile(JTextField targetField, String description, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extensions.length > 0) {
            chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadProject(JTextField projectFileDisplay) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "BDQ project files (*.properties)", "properties"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            ProjectState loaded = serializer.load(file);
            // Merge into current state (non-destructively)
            state.setOutputDirectory(loaded.getOutputDirectory());
            if (loaded.getUseCaseDraft() != null) {
                state.setUseCaseDraft(loaded.getUseCaseDraft());
            }
            state.clearInformationElements();
            for (InformationElementRef ref : loaded.getInformationElements()) {
                state.addInformationElement(ref);
            }
            if (loaded.getAdditionalVocabUri() != null) {
                state.setAdditionalVocabUri(loaded.getAdditionalVocabUri());
            }
            if (loaded.getAdditionalRdfSource() != null) {
                state.setAdditionalRdfSource(loaded.getAdditionalRdfSource());
            }
            // Refresh displayed fields
            onEnter();
            projectFileDisplay.setText(file.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Project loaded successfully from:\n" + file.getAbsolutePath(),
                    "Project loaded",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load project file:\n" + ex.getMessage(),
                    "Load error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveProject() {
        onLeave(); // flush form → state first
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "BDQ project files (*.properties)", "properties"));
        chooser.setSelectedFile(new File("bdq_project.properties"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".properties")) {
            file = new File(file.getAbsolutePath() + ".properties");
        }
        try {
            serializer.save(state, file);
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

    private void loadFromRdf() {
        String rdfSrc = rdfSourceField.getText().trim();
        if (rdfSrc.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter an RDF source path or URI first.",
                    "No RDF source",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Load use cases
        loadedUseCases = rdfLoader.loadUseCases(rdfSrc);
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement("(none – start fresh)");
        for (UseCaseDraft uc : loadedUseCases) {
            comboModel.addElement(uc.getName() != null ? uc.getName() : "(unnamed use case)");
        }
        existingUseCaseCombo.setModel(comboModel);
        // Load tests and inject into catalog
        List<TestCatalogEntry> loadedTests = rdfLoader.loadTestEntries(rdfSrc);
        if (catalogService != null) {
            catalogService.addEntries(loadedTests);
        }
        // Store the RDF source in state
        state.setAdditionalRdfSource(rdfSrc);
        // Report results
        StringBuilder msg = new StringBuilder();
        msg.append("Loaded from: ").append(rdfSrc).append("\n\n");
        msg.append("Use cases found: ").append(loadedUseCases.size()).append("\n");
        msg.append("Tests loaded into catalog: ").append(loadedTests.size()).append("\n");
        if (!loadedUseCases.isEmpty()) {
            msg.append("\nSelect a use case from the drop-down to use as your starting use case.");
        }
        JOptionPane.showMessageDialog(this, msg.toString(),
                "RDF load complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applySelectedUseCaseToState() {
        if (loadedUseCases == null || loadedUseCases.isEmpty()) {
            return;
        }
        int selectedIdx = existingUseCaseCombo.getSelectedIndex();
        if (selectedIdx <= 0) {
            return; // "(none – start fresh)" selected
        }
        int ucIdx = selectedIdx - 1; // offset for the "(none)" entry
        if (ucIdx >= 0 && ucIdx < loadedUseCases.size()) {
            UseCaseDraft selected = loadedUseCases.get(ucIdx);
            state.setUseCaseDraft(selected);
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
