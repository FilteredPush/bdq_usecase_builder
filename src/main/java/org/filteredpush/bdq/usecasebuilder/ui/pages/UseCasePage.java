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
    private JTextArea fitnessLeadArea;
    private JTextArea fitnessPropertiesArea;
    private boolean updatingFitnessLeadTemplate;
    private boolean fitnessLeadUserEdited;

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
        loadFitnessClauses(nvl(draft.getFitnessRequirementsText()));
        ensureFitnessTemplate();
    }

    @Override
    public void onLeave() {
        UseCaseDraft draft = state.getUseCaseDraft();
        draft.setName(nameField.getText().trim());
        draft.setDescription(descriptionArea.getText().trim());
        draft.setFitnessRequirementsText(buildFitnessRequirementsText());
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
                        + "What: the purpose and context for a set of data quality tests.<br>"
                        + "Why: this anchors which information elements and test assertions matter.<br>"
                        + "Convention: keep names concise, describe scope in prose, and express "
                        + "fitness requirements as practical, testable statements.</html>");
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
        fitnessLeadArea = new JTextArea(2, 40);
        fitnessLeadArea.setLineWrap(true);
        fitnessLeadArea.setWrapStyleWord(true);
        fitnessLeadArea.setToolTipText(
                "Clause 1 (descriptive): Data are fit for use for [use case name] if they...");
        fitnessPropertiesArea = new JTextArea(5, 40);
        fitnessPropertiesArea.setLineWrap(true);
        fitnessPropertiesArea.setWrapStyleWord(true);
        fitnessPropertiesArea.setToolTipText(
                "One property per line (a bulleted list will be generated in export text)");

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        JScrollPane fitnessLeadScroll = new JScrollPane(fitnessLeadArea);
        JScrollPane fitnessPropsScroll = new JScrollPane(fitnessPropertiesArea);

        JLabel[] labels = {nameLabel, descLabel, fitnessLabel};

        // Layout
        JLabel formTopLabel = introLabel;

        // Use GridBag for the form
        JScrollPane formWrapper = buildForm(
                formTopLabel, nameLabel, nameField,
                descLabel, descScroll,
                fitnessLabel, fitnessLeadScroll, fitnessPropsScroll);
        add(formWrapper, BorderLayout.CENTER);

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
        });
        fitnessLeadArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
        });
    }

    private JScrollPane buildForm(JLabel introLabel,
                                   JLabel nameLabel, JTextField nameField,
                                   JLabel descLabel, JScrollPane descScroll,
                                   JLabel fitnessLabel, JScrollPane fitnessLeadScroll,
                                   JScrollPane fitnessPropsScroll) {
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
        fc.weighty = 0.25;
        form.add(fitnessLeadScroll, fc);

        lc.gridy = 4;
        form.add(new JLabel("Specific properties (one per line):"), lc);
        fc.gridy = 4;
        fc.weighty = 0.7;
        form.add(fitnessPropsScroll, fc);

        return new JScrollPane(form);
    }

    private void ensureFitnessTemplate() {
        String current = fitnessLeadArea.getText().trim();
        if (current.isEmpty() || !fitnessLeadUserEdited) {
            updatingFitnessLeadTemplate = true;
            fitnessLeadArea.setText(defaultFitnessLeadClause());
            updatingFitnessLeadTemplate = false;
        }
    }

    private String defaultFitnessLeadClause() {
        String name = nameField != null ? nameField.getText().trim() : "";
        String bracketedName = name.isEmpty() ? "[use case name]" : name;
        return "Data are fit for use for " + bracketedName + " if they...";
    }

    private void loadFitnessClauses(String text) {
        String[] lines = text.split("\\R");
        StringBuilder lead = new StringBuilder();
        StringBuilder bullets = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("-")) {
                if (bullets.length() > 0) {
                    bullets.append('\n');
                }
                bullets.append(line.substring(1).trim());
            } else if (!line.isEmpty()) {
                if (lead.length() > 0) {
                    lead.append(' ');
                }
                lead.append(line);
            }
        }
        fitnessLeadArea.setText(lead.toString());
        fitnessPropertiesArea.setText(bullets.toString());
        fitnessLeadUserEdited = !lead.toString().trim().isEmpty();
    }

    private String buildFitnessRequirementsText() {
        StringBuilder text = new StringBuilder();
        String lead = fitnessLeadArea.getText().trim();
        if (!lead.isEmpty()) {
            text.append(lead);
        }
        String[] propertyLines = fitnessPropertiesArea.getText().split("\\R");
        for (String propertyLine : propertyLines) {
            String trimmed = propertyLine.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append("- ").append(trimmed);
        }
        return text.toString().trim();
    }

    private void markFitnessLeadEdited() {
        if (!updatingFitnessLeadTemplate) {
            fitnessLeadUserEdited = !fitnessLeadArea.getText().trim().isEmpty();
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
