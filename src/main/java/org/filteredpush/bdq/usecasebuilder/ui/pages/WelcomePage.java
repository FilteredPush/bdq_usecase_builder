package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Wizard page 1 – Welcome and project setup.
 *
 * <p>Collects the output directory for exported artifacts. The user can either
 * type a path or browse using the native file chooser.</p>
 */
public class WelcomePage extends WizardPage {

    private JTextField outputDirField;
    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the welcome page.
     *
     * @param state shared project state
     */
    public WelcomePage(ProjectState state) {
        super(state);
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
    }

    @Override
    public void onLeave() {
        state.setOutputDirectory(outputDirField.getText().trim());
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
                        + "<p>Click <b>Next</b> to begin.</p>"
                        + "</html>");
        welcomeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        add(welcomeLabel, BorderLayout.NORTH);

        // Output directory form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Project setup"));

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(6, 6, 6, 6);

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.insets = new Insets(6, 0, 6, 6);

        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.insets = new Insets(6, 0, 6, 6);

        labelGbc.gridx = 0;
        labelGbc.gridy = 0;
        form.add(new JLabel("Output directory:"), labelGbc);

        outputDirField = new JTextField(30);
        outputDirField.setToolTipText("Directory where exported files will be written");
        fieldGbc.gridx = 1;
        fieldGbc.gridy = 0;
        form.add(outputDirField, fieldGbc);

        JButton browseButton = new JButton("Browse…");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (!outputDirField.getText().trim().isEmpty()) {
                chooser.setCurrentDirectory(new java.io.File(outputDirField.getText().trim()));
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        buttonGbc.gridx = 2;
        buttonGbc.gridy = 0;
        form.add(browseButton, buttonGbc);

        add(form, BorderLayout.CENTER);
    }
}
