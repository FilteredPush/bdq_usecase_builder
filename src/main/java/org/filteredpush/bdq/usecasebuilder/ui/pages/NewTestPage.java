package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.ResourceType;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
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
    private JTextField dimensionField;
    private JTextField criterionField;
    private JTextArea expectedResponseArea;
    private JTextArea notesArea;

    private boolean updatingForm = false;
    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the new test definition page.
     *
     * @param state shared project state
     */
    public NewTestPage(ProjectState state) {
        super(state);
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

        add(leftPanel, BorderLayout.WEST);
        add(formScroll, BorderLayout.CENTER);
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
        dimensionField = new JTextField(20);
        dimensionField.setToolTipText(
                "Data quality dimension (e.g. Completeness, Conformance, Resolution)");
        criterionField = new JTextField(20);
        criterionField.setToolTipText(
                "Criterion (for Validation/Measure/Issue) or Enhancement (for Amendment)");
        expectedResponseArea = new JTextArea(5, 30);
        expectedResponseArea.setLineWrap(true);
        expectedResponseArea.setWrapStyleWord(true);
        notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        addRow(form, "Label:", labelField, 0);
        addRow(form, "Preferred label:", prefLabelField, 1);
        addRow(form, "Type *:", typeCombo, 2);
        addRow(form, "Resource type:", resourceTypeCombo, 3);
        addRow(form, "Dimension:", dimensionField, 4);
        addRow(form, "Criterion/Enhancement:", criterionField, 5);

        GridBagConstraints lc = labelConstraints(6);
        form.add(new JLabel("Expected response:"), lc);
        GridBagConstraints fc = fieldConstraints(6);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.6;
        form.add(new JScrollPane(expectedResponseArea), fc);

        lc = labelConstraints(7);
        form.add(new JLabel("Notes:"), lc);
        fc = fieldConstraints(7);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        form.add(new JScrollPane(notesArea), fc);

        JButton saveButton = new JButton("Save draft");
        saveButton.addActionListener(e -> saveCurrentDraft());
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 8;
        bc.gridx = 1;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 0, 0, 0);
        form.add(saveButton, bc);

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
        dimensionField.setText(nvl(draft.getDimension()));
        criterionField.setText(nvl(draft.getCriterionOrEnhancement()));
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
        draft.setDimension(dimensionField.getText().trim());
        draft.setCriterionOrEnhancement(criterionField.getText().trim());
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
        dimensionField.setText("");
        criterionField.setText("");
        expectedResponseArea.setText("");
        notesArea.setText("");
        updatingForm = false;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
