package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.ResourceType;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.service.VocabularyService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page 5 – Define new tests.
 *
 * <p>Lets the user author one or more new BDQ test drafts. A list on the left
 * shows existing drafts; selecting a draft populates the form on the right for
 * editing. The user can add new drafts or delete existing ones.</p>
 */
public class NewTestPage extends WizardPage {

    // List of drafts being edited in this session
    private final DefaultListModel<TestDraft> listModel = new DefaultListModel<>();
    private JList<TestDraft> draftList;

    // Form fields
    private JTextField labelField;
    private JTextField prefLabelField;
    private JComboBox<TestType> typeCombo;
    private JComboBox<ResourceType> resourceTypeCombo;
    private JComboBox<String> dimensionCombo;
    private JComboBox<String> criterionCombo;
    private JComboBox<String> useCaseRefCombo;
    private JComboBox<String> parameterDefaultsCombo;
    private JTextArea expectedResponseArea;
    private JTextArea notesArea;

    private boolean updatingForm = false;
    private final ValidationService validationService = new ValidationService();
    private final VocabularyService vocabularyService;

    /**
     * Creates the new test definition page.
     *
     * @param state shared project state
     */
    public NewTestPage(ProjectState state, VocabularyService vocabularyService) {
        super(state);
        this.vocabularyService = vocabularyService;
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Define New Tests";
    }

    @Override
    public void onEnter() {
        listModel.clear();
        for (TestDraft draft : state.getNewTestDrafts()) {
            listModel.addElement(draft);
        }
        refreshPicklists();
        clearForm();
    }

    @Override
    public void onLeave() {
        // Save any in-progress edits
        saveCurrentDraft();
        state.clearNewTestDrafts();
        for (int i = 0; i < listModel.size(); i++) {
            state.addNewTestDraft(listModel.get(i));
        }
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        // Validate all drafts
        List<String> errors = new ArrayList<>();
        for (TestDraft draft : state.getNewTestDrafts()) {
            errors.addAll(validationService.validateNewTestPage(draft));
        }
        return errors;
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel guidance = new JLabel(
                "<html><b>Define new BDQ tests.</b> What: the descriptors needed to represent a new test.<br>"
                        + "Why: consistent descriptors make the test reusable across use cases.<br>"
                        + "Convention: use BDQ label pattern <tt>TESTTYPE_INFORMATIONELEMENT_EVALUATION</tt>, "
                        + "choose dimension/criterion from controlled vocabularies, and keep expected response "
                        + "aligned with bdqval response terms.</html>");
        guidance.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(guidance, BorderLayout.NORTH);

        // Left panel: list of drafts + buttons
        JPanel leftPanel = new JPanel(new BorderLayout(0, 4));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Drafts"));

        draftList = new JList<>(listModel);
        draftList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        draftList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDraftIntoForm(draftList.getSelectedValue());
            }
        });
        leftPanel.add(new JScrollPane(draftList), BorderLayout.CENTER);

        JPanel listButtons = new JPanel();
        JButton addDraftButton = new JButton("New");
        JButton deleteDraftButton = new JButton("Delete");
        addDraftButton.addActionListener(e -> addNewDraft());
        deleteDraftButton.addActionListener(e -> deleteSelectedDraft());
        listButtons.add(addDraftButton);
        listButtons.add(deleteDraftButton);
        leftPanel.add(listButtons, BorderLayout.SOUTH);

        // Right panel: form
        JScrollPane formScroll = new JScrollPane(buildForm());

        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.add(leftPanel, BorderLayout.WEST);
        content.add(formScroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Test details"));

        labelField = new JTextField(30);
        labelField.setToolTipText(
                "Machine-readable label: TESTTYPE_INFORMATIONELEMENT_EVALUATION");
        prefLabelField = new JTextField(30);
        prefLabelField.setToolTipText("Human-readable preferred label (skos:prefLabel)");
        typeCombo = new JComboBox<>(TestType.values());
        resourceTypeCombo = new JComboBox<>(ResourceType.values());
        dimensionCombo = new JComboBox<>();
        dimensionCombo.setEditable(true);
        dimensionCombo.setToolTipText("Data quality dimension (bdqdim)");
        criterionCombo = new JComboBox<>();
        criterionCombo.setEditable(true);
        criterionCombo.setToolTipText(
                "Criterion (bdqcrit) or Enhancement (bdqenh), depending on test type");
        useCaseRefCombo = new JComboBox<>();
        useCaseRefCombo.setEditable(true);
        useCaseRefCombo.setToolTipText("Optional use-case concept reference (bdquc)");
        parameterDefaultsCombo = new JComboBox<>();
        parameterDefaultsCombo.setEditable(true);
        parameterDefaultsCombo.setToolTipText(
                "Optional parameter/default profile, including bdqval response terms");
        expectedResponseArea = new JTextArea(5, 30);
        expectedResponseArea.setLineWrap(true);
        expectedResponseArea.setWrapStyleWord(true);
        expectedResponseArea.setToolTipText("Describe expected response using bdqval terms when possible");
        notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        addRow(form, "Label:", labelField, 0);
        addRow(form, "Preferred label:", prefLabelField, 1);
        addRow(form, "Type *:", typeCombo, 2);
        addRow(form, "Resource type:", resourceTypeCombo, 3);
        addRow(form, "Dimension:", dimensionCombo, 4);
        addRow(form, "Criterion/Enhancement:", criterionCombo, 5);
        addRow(form, "Use-case reference:", useCaseRefCombo, 6);
        addRow(form, "Parameters/defaults:", parameterDefaultsCombo, 7);

        GridBagConstraints lc = labelConstraints(8);
        form.add(new JLabel("Expected response:"), lc);
        GridBagConstraints fc = fieldConstraints(8);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.6;
        form.add(new JScrollPane(expectedResponseArea), fc);

        lc = labelConstraints(9);
        form.add(new JLabel("Notes:"), lc);
        fc = fieldConstraints(9);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        form.add(new JScrollPane(notesArea), fc);

        JButton saveButton = new JButton("Save draft");
        saveButton.addActionListener(e -> saveCurrentDraft());
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 10;
        bc.gridx = 1;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 0, 0, 0);
        form.add(saveButton, bc);

        typeCombo.addActionListener(e -> refreshCriterionPicklistByType());

        return form;
    }

    private void addRow(JPanel form, String labelText, java.awt.Component field, int gridy) {
        form.add(new JLabel(labelText), labelConstraints(gridy));
        form.add(field, fieldConstraints(gridy));
    }

    private GridBagConstraints labelConstraints(int gridy) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(4, 4, 4, 8);
        return c;
    }

    private GridBagConstraints fieldConstraints(int gridy) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = gridy;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 0, 4, 4);
        return c;
    }

    // -----------------------------------------------------------------------
    // Draft management
    // -----------------------------------------------------------------------

    private void addNewDraft() {
        saveCurrentDraft();
        TestDraft draft = new TestDraft();
        listModel.addElement(draft);
        draftList.setSelectedIndex(listModel.size() - 1);
        loadDraftIntoForm(draft);
    }

    private void deleteSelectedDraft() {
        int idx = draftList.getSelectedIndex();
        if (idx >= 0) {
            listModel.remove(idx);
            clearForm();
        }
    }

    private void loadDraftIntoForm(TestDraft draft) {
        if (draft == null) {
            clearForm();
            return;
        }
        updatingForm = true;
        labelField.setText(nvl(draft.getLabel()));
        prefLabelField.setText(nvl(draft.getPrefLabel()));
        typeCombo.setSelectedItem(draft.getType() != null ? draft.getType() : TestType.VALIDATION);
        resourceTypeCombo.setSelectedItem(
                draft.getResourceType() != null ? draft.getResourceType() : ResourceType.SINGLE_RECORD);
        dimensionCombo.setSelectedItem(nvl(draft.getDimension()));
        refreshCriterionPicklistByType();
        criterionCombo.setSelectedItem(nvl(draft.getCriterionOrEnhancement()));
        useCaseRefCombo.setSelectedItem(nvl(draft.getUseCaseReference()));
        parameterDefaultsCombo.setSelectedItem(nvl(draft.getParameterDefaults()));
        expectedResponseArea.setText(nvl(draft.getExpectedResponse()));
        notesArea.setText(nvl(draft.getNotes()));
        updatingForm = false;
    }

    private void saveCurrentDraft() {
        TestDraft draft = draftList.getSelectedValue();
        if (draft == null) {
            return;
        }
        draft.setLabel(labelField.getText().trim());
        draft.setPrefLabel(prefLabelField.getText().trim());
        draft.setType((TestType) typeCombo.getSelectedItem());
        draft.setResourceType((ResourceType) resourceTypeCombo.getSelectedItem());
        draft.setDimension(getSelectedComboText(dimensionCombo));
        draft.setCriterionOrEnhancement(getSelectedComboText(criterionCombo));
        draft.setUseCaseReference(getSelectedComboText(useCaseRefCombo));
        draft.setParameterDefaults(getSelectedComboText(parameterDefaultsCombo));
        draft.setExpectedResponse(expectedResponseArea.getText().trim());
        draft.setNotes(notesArea.getText().trim());
        // Refresh the list cell rendering
        int idx = draftList.getSelectedIndex();
        listModel.set(idx, draft);
    }

    private void clearForm() {
        updatingForm = true;
        labelField.setText("");
        prefLabelField.setText("");
        typeCombo.setSelectedIndex(0);
        resourceTypeCombo.setSelectedIndex(0);
        dimensionCombo.setSelectedItem("");
        refreshCriterionPicklistByType();
        criterionCombo.setSelectedItem("");
        useCaseRefCombo.setSelectedItem("");
        parameterDefaultsCombo.setSelectedItem("");
        expectedResponseArea.setText("");
        notesArea.setText("");
        updatingForm = false;
    }

    private void refreshPicklists() {
        resetComboItems(dimensionCombo, vocabularyService.getBdqDimensions());
        resetComboItems(useCaseRefCombo, vocabularyService.getBdqUseCaseTerms());
        resetComboItems(parameterDefaultsCombo, vocabularyService.getBdqValidationTerms());
        refreshCriterionPicklistByType();
    }

    private void refreshCriterionPicklistByType() {
        if (criterionCombo == null) {
            return;
        }
        String selected = getSelectedComboText(criterionCombo);
        TestType selectedType = (TestType) typeCombo.getSelectedItem();
        List<String> terms = selectedType == TestType.AMENDMENT
                ? vocabularyService.getBdqEnhancements()
                : vocabularyService.getBdqCriteria();
        resetComboItems(criterionCombo, terms);
        criterionCombo.setSelectedItem(selected);
    }

    private void resetComboItems(JComboBox<String> combo, List<String> items) {
        combo.removeAllItems();
        for (String term : items) {
            combo.addItem(term);
        }
    }

    private String getSelectedComboText(JComboBox<String> combo) {
        Object value = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
        return value != null ? value.toString().trim() : "";
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
