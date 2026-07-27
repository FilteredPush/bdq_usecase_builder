package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.ResourceType;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.service.InformationElementTermService;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.service.VocabularyService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private JComboBox<String> informationElementCombo;
    private JComboBox<String> dimensionCombo;
    private JComboBox<String> criterionCombo;
    private JComboBox<String> useCaseRefCombo;
    private JComboBox<String> responseResultCombo;
    private JTextField responseConditionField;
    private DefaultListModel<String> responseClauseListModel;
    private JList<String> responseClauseList;
    private JTextArea expectedResponseArea;
    private JTextArea notesArea;
    private DefaultListModel<String> coverageListModel;
    private JList<String> coverageList;

    private boolean updatingForm = false;
    private final ValidationService validationService = new ValidationService();
    private final VocabularyService vocabularyService;
    private final TestCatalogService catalogService;

    /**
     * Creates the new test definition page.
     *
     * @param state shared project state
     */
    public NewTestPage(ProjectState state, VocabularyService vocabularyService,
                       TestCatalogService catalogService) {
        super(state);
        this.vocabularyService = vocabularyService;
        this.catalogService = catalogService;
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
        refreshCoverageList();
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
        refreshCoverageList();
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
                        + "as clauses using response.result values.</html>");
        guidance.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(guidance, BorderLayout.NORTH);

        // Left panel: list of drafts + buttons
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Drafts"));

        coverageListModel = new DefaultListModel<>();
        coverageList = new JList<>(coverageListModel);
        coverageList.setVisibleRowCount(6);
        coverageList.setToolTipText("Information elements without tests are marked with ⚠");
        JPanel coveragePanel = new JPanel(new BorderLayout(0, 2));
        coveragePanel.setBorder(BorderFactory.createTitledBorder("Information element coverage"));
        coveragePanel.add(new JScrollPane(coverageList), BorderLayout.CENTER);
        leftPanel.add(coveragePanel, BorderLayout.NORTH);

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
        informationElementCombo = new JComboBox<>();
        informationElementCombo.setEditable(true);
        informationElementCombo.setToolTipText("Information element this test evaluates");
        dimensionCombo = new JComboBox<>();
        dimensionCombo.setEditable(true);
        dimensionCombo.setToolTipText("Data quality dimension (bdqdim)");
        criterionCombo = new JComboBox<>();
        criterionCombo.setEditable(true);
        criterionCombo.setToolTipText(
                "Criterion (bdqcrit) or Enhancement (bdqenh), depending on test type");
        useCaseRefCombo = new JComboBox<>();
        useCaseRefCombo.setEditable(true);
        useCaseRefCombo.setToolTipText("Use-case reference (current use case plus optional bdquc terms)");
        responseResultCombo = new JComboBox<>();
        responseResultCombo.setEditable(false);
        responseResultCombo.setToolTipText("response.result value");
        responseConditionField = new JTextField(24);
        responseConditionField.setToolTipText("Condition for this expected response clause");
        responseClauseListModel = new DefaultListModel<>();
        responseClauseList = new JList<>(responseClauseListModel);
        expectedResponseArea = new JTextArea(5, 30);
        expectedResponseArea.setLineWrap(true);
        expectedResponseArea.setWrapStyleWord(true);
        expectedResponseArea.setEditable(false);
        expectedResponseArea.setToolTipText("Expected response clauses (auto-built from clause controls)");
        notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        addRow(form, "Label:", labelField, 0);
        addRow(form, "Preferred label:", prefLabelField, 1);
        addRow(form, "Type *:", typeCombo, 2);
        addRow(form, "Resource type:", resourceTypeCombo, 3);
        addRow(form, "Information element:", informationElementCombo, 4);
        addRow(form, "Dimension:", dimensionCombo, 5);
        addRow(form, "Criterion/Enhancement:", criterionCombo, 6);
        addRow(form, "Use-case reference:", useCaseRefCombo, 7);

        GridBagConstraints lc = labelConstraints(8);
        form.add(new JLabel("Add expected-response clause:"), lc);
        JPanel clausePanel = new JPanel(new BorderLayout(4, 0));
        clausePanel.add(responseConditionField, BorderLayout.CENTER);
        JPanel clauseRight = new JPanel(new BorderLayout(4, 0));
        clauseRight.add(responseResultCombo, BorderLayout.CENTER);
        JButton addClauseButton = new JButton("Add clause");
        addClauseButton.addActionListener(e -> addExpectedResponseClause());
        clauseRight.add(addClauseButton, BorderLayout.EAST);
        clausePanel.add(clauseRight, BorderLayout.EAST);
        GridBagConstraints fc = fieldConstraints(8);
        form.add(clausePanel, fc);

        lc = labelConstraints(9);
        form.add(new JLabel("Clauses:"), lc);
        fc = fieldConstraints(9);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        form.add(new JScrollPane(responseClauseList), fc);

        JButton removeClauseButton = new JButton("Remove clause");
        removeClauseButton.addActionListener(e -> removeSelectedClause());
        GridBagConstraints rc = new GridBagConstraints();
        rc.gridy = 10;
        rc.gridx = 1;
        rc.anchor = GridBagConstraints.WEST;
        rc.insets = new Insets(2, 0, 0, 0);
        form.add(removeClauseButton, rc);

        lc = labelConstraints(11);
        form.add(new JLabel("Expected response:"), lc);
        fc = fieldConstraints(11);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.6;
        form.add(new JScrollPane(expectedResponseArea), fc);

        lc = labelConstraints(12);
        form.add(new JLabel("Notes:"), lc);
        fc = fieldConstraints(12);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        form.add(new JScrollPane(notesArea), fc);

        JButton saveButton = new JButton("Save draft");
        saveButton.addActionListener(e -> saveCurrentDraft());
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 13;
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
        informationElementCombo.setSelectedItem(nvl(draft.getInformationElement()));
        dimensionCombo.setSelectedItem(nvl(draft.getDimension()));
        refreshCriterionPicklistByType();
        criterionCombo.setSelectedItem(nvl(draft.getCriterionOrEnhancement()));
        useCaseRefCombo.setSelectedItem(nvl(draft.getUseCaseReference()));
        loadExpectedResponseClauses(nvl(draft.getExpectedResponse()));
        refreshResponseResultsByType();
        expectedResponseArea.setText(buildExpectedResponseFromClauses());
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
        draft.setInformationElement(getSelectedComboText(informationElementCombo));
        draft.setDimension(getSelectedComboText(dimensionCombo));
        draft.setCriterionOrEnhancement(getSelectedComboText(criterionCombo));
        draft.setUseCaseReference(getSelectedComboText(useCaseRefCombo));
        expectedResponseArea.setText(buildExpectedResponseFromClauses());
        draft.setExpectedResponse(expectedResponseArea.getText().trim());
        draft.setNotes(notesArea.getText().trim());
        // Refresh the list cell rendering
        int idx = draftList.getSelectedIndex();
        listModel.set(idx, draft);
        refreshCoverageList();
    }

    private void clearForm() {
        updatingForm = true;
        labelField.setText("");
        prefLabelField.setText("");
        typeCombo.setSelectedIndex(0);
        resourceTypeCombo.setSelectedIndex(0);
        informationElementCombo.setSelectedItem("");
        dimensionCombo.setSelectedItem("");
        refreshCriterionPicklistByType();
        criterionCombo.setSelectedItem("");
        useCaseRefCombo.setSelectedItem("");
        responseConditionField.setText("");
        responseClauseListModel.clear();
        refreshResponseResultsByType();
        expectedResponseArea.setText("");
        notesArea.setText("");
        updatingForm = false;
    }

    private void refreshPicklists() {
        List<String> infoTerms = new ArrayList<>();
        for (InformationElementRef ref : state.getInformationElements()) {
            if (ref.getQname() != null && !ref.getQname().trim().isEmpty()) {
                infoTerms.add(ref.getQname().trim());
            }
        }
        resetComboItems(informationElementCombo, infoTerms);
        resetComboItems(dimensionCombo, vocabularyService.getBdqDimensions());
        List<String> useCaseRefs = new ArrayList<>();
        if (state.getUseCaseDraft() != null && state.getUseCaseDraft().getName() != null
                && !state.getUseCaseDraft().getName().trim().isEmpty()) {
            useCaseRefs.add(state.getUseCaseDraft().getName().trim());
        }
        useCaseRefs.addAll(vocabularyService.getBdqUseCaseTerms());
        resetComboItems(useCaseRefCombo, useCaseRefs);
        refreshResponseResultsByType();
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
        refreshResponseResultsByType();
    }

    private void refreshResponseResultsByType() {
        if (responseResultCombo == null) {
            return;
        }
        List<String> responseValues = new ArrayList<>();
        TestType selectedType = (TestType) typeCombo.getSelectedItem();
        if (selectedType == TestType.AMENDMENT) {
            responseValues.add("AMENDED");
            responseValues.add("INTERNAL_PREREQUISITES_NOT_MET");
            responseValues.add("EXTERNAL_PREREQUISITES_NOT_MET");
        } else {
            responseValues.add("COMPLIANT");
            responseValues.add("NOT_COMPLIANT");
            responseValues.add("INTERNAL_PREREQUISITES_NOT_MET");
            responseValues.add("EXTERNAL_PREREQUISITES_NOT_MET");
        }
        resetComboItems(responseResultCombo, responseValues);
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

    private void addExpectedResponseClause() {
        String condition = responseConditionField.getText().trim();
        String result = getSelectedComboText(responseResultCombo);
        if (condition.isEmpty() || result.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Provide both a condition and a response.result value.",
                    "Missing clause details",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        responseClauseListModel.addElement("If " + condition + ", response.result = " + result + ".");
        responseConditionField.setText("");
        expectedResponseArea.setText(buildExpectedResponseFromClauses());
    }

    private void removeSelectedClause() {
        int selected = responseClauseList.getSelectedIndex();
        if (selected >= 0) {
            responseClauseListModel.remove(selected);
            expectedResponseArea.setText(buildExpectedResponseFromClauses());
        }
    }

    private void loadExpectedResponseClauses(String expectedResponse) {
        responseClauseListModel.clear();
        String[] lines = expectedResponse.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                responseClauseListModel.addElement(trimmed);
            }
        }
    }

    private String buildExpectedResponseFromClauses() {
        if (responseClauseListModel.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < responseClauseListModel.size(); i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append(responseClauseListModel.get(i));
        }
        return text.toString();
    }

    private void refreshCoverageList() {
        if (coverageListModel == null) {
            return;
        }
        coverageListModel.clear();

        Set<String> selectedExistingIris = new LinkedHashSet<>(state.getSelectedExistingTestIris());
        Set<String> coveredTerms = new LinkedHashSet<>();
        for (String iri : selectedExistingIris) {
            TestCatalogEntry entry = findCatalogEntry(iri);
            if (entry != null) {
                coveredTerms.addAll(InformationElementTermService.extractQualifiedTerms(
                        entry.getLabel(), entry.getPrefLabel()));
            }
        }

        for (int i = 0; i < listModel.size(); i++) {
            TestDraft draft = listModel.get(i);
            if (draft.getInformationElement() != null && !draft.getInformationElement().trim().isEmpty()) {
                coveredTerms.add(draft.getInformationElement().trim());
            }
            coveredTerms.addAll(InformationElementTermService.extractQualifiedTerms(
                    draft.getLabel(), draft.getPrefLabel(), draft.getExpectedResponse()));
        }

        Set<String> infoElements = new LinkedHashSet<>();
        for (InformationElementRef ref : state.getInformationElements()) {
            if (ref.getQname() != null && !ref.getQname().trim().isEmpty()) {
                infoElements.add(ref.getQname().trim());
            }
        }

        for (String infoElement : infoElements) {
            boolean covered = InformationElementTermService.matchesAnySelectedTerm(
                    Set.of(infoElement), coveredTerms);
            coverageListModel.addElement((covered ? "✓ " : "⚠ ") + infoElement);
        }
        if (coverageListModel.isEmpty()) {
            coverageListModel.addElement("(no information elements defined)");
        }
    }

    private TestCatalogEntry findCatalogEntry(String iri) {
        for (TestCatalogEntry entry : catalogService.getEntries()) {
            if (entry.getIri().equals(iri)) {
                return entry;
            }
        }
        return null;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
