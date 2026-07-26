package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Wizard page 2 – Define the use case.
 *
 * <p>Collects the use case name, a description, and the fitness-for-use
 * requirements narrative that motivates the use case.</p>
 */
public class UseCasePage extends WizardPage {

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextArea fitnessArea;

    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the use case definition page.
     *
     * @param state shared project state
     */
    public UseCasePage(ProjectState state) {
        super(state);
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Define Use Case";
    }

    @Override
    public void onEnter() {
        UseCaseDraft draft = state.getUseCaseDraft();
        nameField.setText(nvl(draft.getName()));
        descriptionArea.setText(nvl(draft.getDescription()));
        fitnessArea.setText(nvl(draft.getFitnessRequirementsText()));
    }

    @Override
    public void onLeave() {
        UseCaseDraft draft = state.getUseCaseDraft();
        draft.setName(nameField.getText().trim());
        draft.setDescription(descriptionArea.getText().trim());
        draft.setFitnessRequirementsText(fitnessArea.getText().trim());
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return validationService.validateUseCasePage(state.getUseCaseDraft());
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Define your use case.</b> "
                        + "A use case describes the purpose and context for a set of data quality tests. "
                        + "Provide a concise name and then describe the fitness-for-use requirements "
                        + "that the tests must satisfy.</html>");
        add(intro, BorderLayout.NORTH);

        JLabel introLabel = new JLabel(
                "<html><small>Fields marked * are required.</small></html>");
        introLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        // Form
        JLabel nameLabel = new JLabel("Use case name *:");
        nameField = new JTextField(40);
        nameField.setToolTipText(
                "A short, unique name for the use case (e.g. Spatial quality for specimens)");

        JLabel descLabel = new JLabel("Description:");
        descriptionArea = new JTextArea(4, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setToolTipText("A paragraph describing what the use case is for");

        JLabel fitnessLabel = new JLabel("Fitness-for-use requirements:");
        fitnessArea = new JTextArea(6, 40);
        fitnessArea.setLineWrap(true);
        fitnessArea.setWrapStyleWord(true);
        fitnessArea.setToolTipText(
                "Narrative text listing the requirements the data must meet to be fit for this use");

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        JScrollPane fitnessScroll = new JScrollPane(fitnessArea);

        JLabel[] labels = {nameLabel, descLabel, fitnessLabel};

        // Layout
        JLabel formTopLabel = introLabel;

        // Use GridBag for the form
        JScrollPane formWrapper = buildForm(
                formTopLabel, nameLabel, nameField,
                descLabel, descScroll,
                fitnessLabel, fitnessScroll);
        add(formWrapper, BorderLayout.CENTER);
    }

    private JScrollPane buildForm(JLabel introLabel,
                                   JLabel nameLabel, JTextField nameField,
                                   JLabel descLabel, JScrollPane descScroll,
                                   JLabel fitnessLabel, JScrollPane fitnessScroll) {
        // Use GridBagLayout
        java.awt.Container form = new javax.swing.JPanel(new GridBagLayout());
        form.setBackground(getBackground());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(6, 0, 4, 8);

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(6, 0, 4, 0);
        fc.gridwidth = GridBagConstraints.REMAINDER;

        // Intro
        fc.gridy = 0;
        form.add(introLabel, fc);

        // Name
        lc.gridy = 1;
        form.add(nameLabel, lc);
        fc.gridy = 1;
        fc.gridwidth = GridBagConstraints.REMAINDER;
        form.add(nameField, fc);

        // Description
        lc.gridy = 2;
        form.add(descLabel, lc);
        fc.gridy = 2;
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.3;
        form.add(descScroll, fc);

        // Fitness
        lc.gridy = 3;
        form.add(fitnessLabel, lc);
        fc.gridy = 3;
        fc.weighty = 0.7;
        form.add(fitnessScroll, fc);

        return new JScrollPane(form);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
