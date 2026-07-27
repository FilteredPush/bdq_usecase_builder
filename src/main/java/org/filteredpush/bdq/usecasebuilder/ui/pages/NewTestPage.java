package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.ResourceType;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.service.ExpectedResponseClauseService;
import org.filteredpush.bdq.usecasebuilder.service.InformationElementTermService;
import org.filteredpush.bdq.usecasebuilder.service.LabelSuggestionService;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.service.VocabularyService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.awt.FlowLayout;
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
 *
 * <p>Field order: Type → Resource type → Information element → Label →
 * Preferred label → Dimension → Criterion/Enhancement → Use-case reference →
 * Expected response clauses → Notes → Source authority → Parameters.</p>
 */
public class NewTestPage extends WizardPage {

    // List of drafts being edited in this session
    private final DefaultListModel<TestDraft> listModel = new DefaultListModel<>();
    private JList<TestDraft> draftList;

    // Form fields (reordered per feedback: type first, then IEs, then labels)
    private JComboBox<TestType> typeCombo;
    private JComboBox<ResourceType> resourceTypeCombo;
    private JComboBox<String> informationElementCombo;
    private JTextField labelField;
    private JTextField prefLabelField;
    private JComboBox<String> dimensionCombo;
    private JComboBox<String> criterionCombo;
    private JComboBox<String> useCaseRefCombo;
    private JComboBox<String> responseValueCombo;
    private JTextField responseConditionField;
    private JTextField responseCommentField;
    private DefaultListModel<ExpectedResponseClause> responseClauseListModel;
    private JList<ExpectedResponseClause> responseClauseList;
    private JTextArea expectedResponseArea;
    private JTextArea notesArea;
    private JCheckBox hasSourceAuthorityCheck;
    private JCheckBox hasParametersCheck;
    private JButton addIfButton;
    private JButton addElseButton;
    private JButton removeClauseButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton saveDraftButton;
    private DefaultListModel<String> coverageListModel;
    private JList<String> coverageList;
    private JPanel testDetailsPanel;
    /** Panel containing IE quick-insert buttons; rebuilt on each page enter. */
    private JPanel ieInsertPanel;

    private boolean updatingForm = false;
    private boolean updatingLabel = false;
    private final ValidationService validationService = new ValidationService();
    private final ExpectedResponseClauseService clauseService = new ExpectedResponseClauseService();
    private final LabelSuggestionService labelSuggestionService = new LabelSuggestionService();
    private final VocabularyService vocabularyService;
    private final TestCatalogService catalogService;
    private static final Set<String> PREREQUISITE_STATUSES = Set.of(
            "INTERNAL_PREREQUISITES_NOT_MET",
            "EXTERNAL_PREREQUISITES_NOT_MET");

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
        rebuildIeInsertPanel();
        clearForm();
        setTestDetailsEnabled(false);
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
                        + "Convention: select Type first, pick Information Elements, then labels are suggested "
                        + "automatically. Use the IF condition helper to build expected-response clauses. "
                        + "Fields marked <b>*</b> are required.</html>");
        guidance.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(guidance, BorderLayout.NORTH);

        // Left panel: list of drafts + buttons
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Drafts"));

        coverageListModel = new DefaultListModel<>();
        coverageList = new JList<>(coverageListModel);
        coverageList.setVisibleRowCount(6);
        coverageList.setToolTipText("Information elements without tests are marked with ⚠; unlinked tests marked with ⚡");
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
        testDetailsPanel = new JPanel(new GridBagLayout());
        testDetailsPanel.setBorder(BorderFactory.createTitledBorder("Test details"));

        // ---- Field construction ----
        typeCombo = new JComboBox<>(TestType.values());
        resourceTypeCombo = new JComboBox<>(ResourceType.values());
        informationElementCombo = new JComboBox<>();
        informationElementCombo.setEditable(true);
        informationElementCombo.setToolTipText("Information element this test evaluates (required)");
        labelField = new JTextField(30);
        labelField.setToolTipText("Machine-readable label: TESTTYPE_INFORMATIONELEMENT_EVALUATION (auto-suggested)");
        prefLabelField = new JTextField(30);
        prefLabelField.setToolTipText("Human-readable preferred label (skos:prefLabel, auto-suggested)");
        dimensionCombo = new JComboBox<>();
        dimensionCombo.setEditable(true);
        dimensionCombo.setToolTipText("Data quality dimension (bdqdim), required");
        criterionCombo = new JComboBox<>();
        criterionCombo.setEditable(true);
        criterionCombo.setToolTipText("Criterion (bdqcrit) or Enhancement (bdqenh), depending on test type");
        useCaseRefCombo = new JComboBox<>();
        useCaseRefCombo.setEditable(true);
        useCaseRefCombo.setToolTipText("Use-case reference (required)");
        responseValueCombo = new JComboBox<>();
        responseValueCombo.setEditable(false);
        responseValueCombo.setToolTipText("Expected response outcome (type-aware)");
        responseConditionField = new JTextField(24);
        responseConditionField.setToolTipText("Condition for this expected response clause");
        responseCommentField = new JTextField(24);
        responseCommentField.setToolTipText("Comment template for this clause");
        responseClauseListModel = new DefaultListModel<>();
        responseClauseList = new JList<>(responseClauseListModel);
        expectedResponseArea = new JTextArea(5, 30);
        expectedResponseArea.setLineWrap(true);
        expectedResponseArea.setWrapStyleWord(true);
        expectedResponseArea.setEditable(false);
        expectedResponseArea.setToolTipText("Expected response (auto-built from clause controls)");
        notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        hasSourceAuthorityCheck = new JCheckBox("This test uses a source authority");
        hasSourceAuthorityCheck.setToolTipText("Check if the test consults an external source authority");
        hasParametersCheck = new JCheckBox("This test accepts parameters");
        hasParametersCheck.setToolTipText("Check if the test accepts parameters; lists it in the authorities/parameters section");

        // ---- Field order per feedback: Type → Resource type → IE → Label → PrefLabel → Dimension → Criterion → Use case ----
        int row = 0;
        addRow(testDetailsPanel, "Type *:", typeCombo, row++);
        addRow(testDetailsPanel, "Resource type:", resourceTypeCombo, row++);
        addRow(testDetailsPanel, "Information element *:", informationElementCombo, row++);
        addRow(testDetailsPanel, "Label *:", labelField, row++);
        addRow(testDetailsPanel, "Preferred label *:", prefLabelField, row++);
        addRow(testDetailsPanel, "Dimension *:", dimensionCombo, row++);
        addRow(testDetailsPanel, "Criterion/Enhancement:", criterionCombo, row++);
        addRow(testDetailsPanel, "Use-case reference *:", useCaseRefCombo, row++);

        // IE quick-insert helpers
        GridBagConstraints lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Insert into condition:"), lc);
        ieInsertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        GridBagConstraints fc = fieldConstraints(row++);
        testDetailsPanel.add(ieInsertPanel, fc);

        // IF condition row
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("IF condition:"), lc);
        JPanel clausePanel = new JPanel(new BorderLayout(4, 0));
        clausePanel.add(responseConditionField, BorderLayout.CENTER);
        JPanel clauseRight = new JPanel(new BorderLayout(4, 0));
        clauseRight.add(responseValueCombo, BorderLayout.CENTER);
        addIfButton = new JButton("Add IF");
        addIfButton.addActionListener(e -> addExpectedResponseClause(false));
        clauseRight.add(addIfButton, BorderLayout.EAST);
        clausePanel.add(clauseRight, BorderLayout.EAST);
        fc = fieldConstraints(row++);
        testDetailsPanel.add(clausePanel, fc);

        // Comment template row
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Comment template:"), lc);
        fc = fieldConstraints(row++);
        testDetailsPanel.add(responseCommentField, fc);

        // Clauses list row
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Clauses:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        testDetailsPanel.add(new JScrollPane(responseClauseList), fc);

        // Clause control buttons
        JPanel clauseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        addElseButton = new JButton("Add ELSE");
        addElseButton.setToolTipText("Add an ELSE (fallback) clause");
        addElseButton.addActionListener(e -> addExpectedResponseClause(true));
        JButton addOtherwiseButton = new JButton("Add otherwise →");
        addOtherwiseButton.setToolTipText("Quick-add \"otherwise NOT_COMPLIANT\" (or AMENDED for Amendment type)");
        addOtherwiseButton.addActionListener(e -> addOtherwiseClause());
        removeClauseButton = new JButton("Remove");
        removeClauseButton.addActionListener(e -> removeSelectedClause());
        moveUpButton = new JButton("↑");
        moveUpButton.addActionListener(e -> moveSelectedClause(-1));
        moveDownButton = new JButton("↓");
        moveDownButton.addActionListener(e -> moveSelectedClause(1));
        clauseButtons.add(addElseButton);
        clauseButtons.add(addOtherwiseButton);
        clauseButtons.add(removeClauseButton);
        clauseButtons.add(moveUpButton);
        clauseButtons.add(moveDownButton);
        GridBagConstraints rc = new GridBagConstraints();
        rc.gridy = row++;
        rc.gridx = 1;
        rc.anchor = GridBagConstraints.WEST;
        rc.insets = new Insets(2, 0, 0, 0);
        testDetailsPanel.add(clauseButtons, rc);

        // Expected response preview
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Expected response:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.6;
        testDetailsPanel.add(new JScrollPane(expectedResponseArea), fc);

        // Notes
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Notes:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        testDetailsPanel.add(new JScrollPane(notesArea), fc);

        // Source authority and parameters checkboxes
        fc = fieldConstraints(row++);
        testDetailsPanel.add(hasSourceAuthorityCheck, fc);
        fc = fieldConstraints(row++);
        testDetailsPanel.add(hasParametersCheck, fc);

        // Save button
        saveDraftButton = new JButton("Save draft");
        saveDraftButton.addActionListener(e -> saveCurrentDraft());
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = row;
        bc.gridx = 1;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 0, 0, 0);
        testDetailsPanel.add(saveDraftButton, bc);

        // Wire up type combo to refresh criterion picklist and response results
        typeCombo.addActionListener(e -> {
            refreshCriterionPicklistByType();
            applySuggestions();
        });
        // Wire IE combo to trigger label suggestions
        informationElementCombo.addActionListener(e -> applySuggestions());
        // Wire dimension/criterion to trigger label suggestions
        dimensionCombo.addActionListener(e -> applySuggestions());
        criterionCombo.addActionListener(e -> applySuggestions());

        // Track manual label edits to suppress further auto-suggestions
        labelField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onLabelManualEdit(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onLabelManualEdit(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onLabelManualEdit(); }
        });
        prefLabelField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onPrefLabelManualEdit(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onPrefLabelManualEdit(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onPrefLabelManualEdit(); }
        });

        return testDetailsPanel;
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
        setTestDetailsEnabled(true);
    }

    private void deleteSelectedDraft() {
        int idx = draftList.getSelectedIndex();
        if (idx >= 0) {
            listModel.remove(idx);
            if (listModel.isEmpty()) {
                clearForm();
                setTestDetailsEnabled(false);
            } else {
                int next = Math.min(idx, listModel.size() - 1);
                draftList.setSelectedIndex(next);
            }
        }
    }

    private void loadDraftIntoForm(TestDraft draft) {
        if (draft == null) {
            clearForm();
            setTestDetailsEnabled(false);
            return;
        }
        setTestDetailsEnabled(true);
        updatingForm = true;
        typeCombo.setSelectedItem(draft.getType() != null ? draft.getType() : TestType.VALIDATION);
        resourceTypeCombo.setSelectedItem(
                draft.getResourceType() != null ? draft.getResourceType() : ResourceType.SINGLE_RECORD);
        informationElementCombo.setSelectedItem(nvl(draft.getInformationElement()));
        refreshCriterionPicklistByType();
        dimensionCombo.setSelectedItem(nvl(draft.getDimension()));
        criterionCombo.setSelectedItem(nvl(draft.getCriterionOrEnhancement()));
        String configuredUseCaseRef = nvl(draft.getUseCaseReference());
        if (configuredUseCaseRef.isEmpty()) {
            useCaseRefCombo.setSelectedItem(getDefaultUseCaseReference());
        } else {
            useCaseRefCombo.setSelectedItem(configuredUseCaseRef);
        }
        updatingLabel = true;
        labelField.setText(nvl(draft.getLabel()));
        prefLabelField.setText(nvl(draft.getPrefLabel()));
        updatingLabel = false;
        loadExpectedResponseClauses(draft);
        refreshResponseResultsByType();
        expectedResponseArea.setText(clauseService.toCanonicalText(
                toClauseList(responseClauseListModel)));
        notesArea.setText(nvl(draft.getNotes()));
        hasSourceAuthorityCheck.setSelected(draft.isHasSourceAuthority());
        hasParametersCheck.setSelected(draft.isHasParameters());
        updatingForm = false;
    }

    private void saveCurrentDraft() {
        TestDraft draft = draftList.getSelectedValue();
        if (draft == null) {
            return;
        }
        draft.setType((TestType) typeCombo.getSelectedItem());
        draft.setResourceType((ResourceType) resourceTypeCombo.getSelectedItem());
        draft.setInformationElement(getSelectedComboText(informationElementCombo));
        draft.setDimension(getSelectedComboText(dimensionCombo));
        draft.setCriterionOrEnhancement(getSelectedComboText(criterionCombo));
        draft.setUseCaseReference(getSelectedComboText(useCaseRefCombo));
        draft.setLabel(labelField.getText().trim());
        draft.setPrefLabel(prefLabelField.getText().trim());
        expectedResponseArea.setText(clauseService.toCanonicalText(toClauseList(responseClauseListModel)));
        draft.setExpectedResponseClauses(toClauseList(responseClauseListModel));
        draft.setExpectedResponse(expectedResponseArea.getText().trim());
        draft.setNotes(notesArea.getText().trim());
        draft.setHasSourceAuthority(hasSourceAuthorityCheck.isSelected());
        draft.setHasParameters(hasParametersCheck.isSelected());
        // Refresh the list cell rendering
        int idx = draftList.getSelectedIndex();
        listModel.set(idx, draft);
        refreshCoverageList();
    }

    private void clearForm() {
        updatingForm = true;
        typeCombo.setSelectedIndex(0);
        resourceTypeCombo.setSelectedIndex(0);
        informationElementCombo.setSelectedItem("");
        dimensionCombo.setSelectedItem("");
        refreshCriterionPicklistByType();
        criterionCombo.setSelectedItem("");
        useCaseRefCombo.setSelectedItem(getDefaultUseCaseReference());
        updatingLabel = true;
        labelField.setText("");
        prefLabelField.setText("");
        updatingLabel = false;
        responseConditionField.setText("");
        responseCommentField.setText("");
        responseClauseListModel.clear();
        refreshResponseResultsByType();
        expectedResponseArea.setText("");
        notesArea.setText("");
        hasSourceAuthorityCheck.setSelected(false);
        hasParametersCheck.setSelected(false);
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
        LinkedHashSet<String> useCaseRefSet = new LinkedHashSet<>();
        String defaultUseCaseRef = getDefaultUseCaseReference();
        if (!defaultUseCaseRef.isEmpty()) {
            useCaseRefSet.add(defaultUseCaseRef);
        }
        useCaseRefSet.addAll(vocabularyService.getBdqUseCaseReferenceTerms());
        resetComboItems(useCaseRefCombo, new ArrayList<>(useCaseRefSet));
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
        if (responseValueCombo == null) {
            return;
        }
        List<String> responseValues = new ArrayList<>();
        responseValues.add("INTERNAL_PREREQUISITES_NOT_MET");
        responseValues.add("EXTERNAL_PREREQUISITES_NOT_MET");
        TestType selectedType = (TestType) typeCombo.getSelectedItem();
        if (selectedType == TestType.AMENDMENT) {
            responseValues.add("AMENDED");
            responseValues.add("NOT_AMENDED");
            responseValues.add("COMPLETE");
        } else if (selectedType == TestType.MEASURE) {
            responseValues.add("COMPLETE");
            responseValues.add("NOT_COMPLETE");
        } else if (selectedType == TestType.ISSUE) {
            responseValues.add("HAS_PROBLEM");
            responseValues.add("NO_PROBLEM");
        } else {
            // VALIDATION (default)
            responseValues.add("COMPLIANT");
            responseValues.add("NOT_COMPLIANT");
        }
        resetComboItems(responseValueCombo, responseValues);
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

    private String getDefaultUseCaseReference() {
        if (state.getUseCaseDraft() == null || state.getUseCaseDraft().getName() == null) {
            return "";
        }
        return state.getUseCaseDraft().getName().trim();
    }

    // -----------------------------------------------------------------------
    // Label auto-suggestion
    // -----------------------------------------------------------------------

    /** Applies label/prefLabel suggestions when user hasn't manually overridden them. */
    private void applySuggestions() {
        if (updatingForm || updatingLabel) {
            return;
        }
        TestDraft scratch = buildScratchDraft();
        if (!scratch.isLabelUserOverridden()) {
            String suggested = labelSuggestionService.suggestLabel(scratch);
            if (suggested != null) {
                updatingLabel = true;
                labelField.setText(suggested);
                updatingLabel = false;
            }
        }
        if (!scratch.isPrefLabelUserOverridden()) {
            String suggested = labelSuggestionService.suggestPrefLabel(scratch);
            if (suggested != null) {
                updatingLabel = true;
                prefLabelField.setText(suggested);
                updatingLabel = false;
            }
        }
    }

    /** Builds a scratch TestDraft from the current form state for suggestion purposes. */
    private TestDraft buildScratchDraft() {
        TestDraft scratch = new TestDraft();
        scratch.setType((TestType) typeCombo.getSelectedItem());
        String ie = getSelectedComboText(informationElementCombo);
        if (!ie.isEmpty()) {
            scratch.addActedUponElement(ie);
        }
        scratch.setDimension(getSelectedComboText(dimensionCombo));
        scratch.setCriterionOrEnhancement(getSelectedComboText(criterionCombo));
        // Carry override flags from the real selected draft
        TestDraft current = draftList.getSelectedValue();
        if (current != null) {
            scratch.setLabelUserOverridden(current.isLabelUserOverridden());
            scratch.setPrefLabelUserOverridden(current.isPrefLabelUserOverridden());
        }
        return scratch;
    }

    private void onLabelManualEdit() {
        if (!updatingForm && !updatingLabel) {
            TestDraft draft = draftList.getSelectedValue();
            if (draft != null) {
                draft.setLabelUserOverridden(!labelField.getText().trim().isEmpty());
            }
        }
    }

    private void onPrefLabelManualEdit() {
        if (!updatingForm && !updatingLabel) {
            TestDraft draft = draftList.getSelectedValue();
            if (draft != null) {
                draft.setPrefLabelUserOverridden(!prefLabelField.getText().trim().isEmpty());
            }
        }
    }

    // -----------------------------------------------------------------------
    // IE quick-insert panel (rebuilt on page enter)
    // -----------------------------------------------------------------------

    private void rebuildIeInsertPanel() {
        if (ieInsertPanel == null) {
            return;
        }
        ieInsertPanel.removeAll();
        // bdqval terms first
        for (String val : List.of("bdqval:Empty", "bdqval:NotEmpty")) {
            JButton b = new JButton(val);
            b.setFont(b.getFont().deriveFont(11.0f));
            b.setToolTipText("Append \"" + val + "\" to the IF condition");
            final String token = val;
            b.addActionListener(e -> appendToCondition(token));
            ieInsertPanel.add(b);
        }
        // IE terms from the use case
        for (InformationElementRef ref : state.getInformationElements()) {
            String qname = ref.getQname();
            if (qname == null || qname.trim().isEmpty()) {
                continue;
            }
            JButton b = new JButton(qname.trim());
            b.setFont(b.getFont().deriveFont(11.0f));
            b.setToolTipText("Append \"" + qname.trim() + "\" to the IF condition");
            final String token = qname.trim();
            b.addActionListener(e -> appendToCondition(token));
            ieInsertPanel.add(b);
        }
        ieInsertPanel.revalidate();
        ieInsertPanel.repaint();
    }

    private void appendToCondition(String token) {
        String current = responseConditionField.getText();
        if (current.isEmpty()) {
            responseConditionField.setText(token);
        } else {
            responseConditionField.setText(current + " " + token);
        }
        responseConditionField.requestFocusInWindow();
    }

    // -----------------------------------------------------------------------
    // Expected-response clause management
    // -----------------------------------------------------------------------

    private void addExpectedResponseClause(boolean elseClause) {
        String condition = responseConditionField.getText().trim();
        String selectedValue = getSelectedComboText(responseValueCombo);
        String comment = responseCommentField.getText().trim();
        if ((!elseClause && condition.isEmpty()) || selectedValue.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Provide required clause details.",
                    "Missing clause details",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ExpectedResponseClause clause = buildClause(elseClause, condition, selectedValue, comment);
        responseClauseListModel.addElement(clause);
        responseConditionField.setText("");
        responseCommentField.setText("");
        expectedResponseArea.setText(clauseService.toCanonicalText(toClauseList(responseClauseListModel)));
    }

    /** Quick-adds the default "otherwise" else clause appropriate for the selected test type. */
    private void addOtherwiseClause() {
        TestType type = (TestType) typeCombo.getSelectedItem();
        String defaultOutcome;
        if (type == TestType.AMENDMENT) {
            defaultOutcome = "NOT_AMENDED";
        } else if (type == TestType.MEASURE) {
            defaultOutcome = "NOT_COMPLETE";
        } else if (type == TestType.ISSUE) {
            defaultOutcome = "NO_PROBLEM";
        } else {
            defaultOutcome = "NOT_COMPLIANT";
        }
        ExpectedResponseClause clause = buildClause(true, "", defaultOutcome, "");
        responseClauseListModel.addElement(clause);
        expectedResponseArea.setText(clauseService.toCanonicalText(toClauseList(responseClauseListModel)));
    }

    private ExpectedResponseClause buildClause(boolean elseClause, String condition,
                                                String outcomeToken, String comment) {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setElseClause(elseClause);
        clause.setCondition(elseClause ? "" : condition);
        if (PREREQUISITE_STATUSES.contains(outcomeToken)) {
            clause.setStatus(outcomeToken);
            clause.setResult("");
        } else {
            clause.setStatus("RUN_HAS_RESULT");
            clause.setResult(outcomeToken);
        }
        clause.setCommentTemplate(comment);
        return clause;
    }

    private void removeSelectedClause() {
        int selected = responseClauseList.getSelectedIndex();
        if (selected >= 0) {
            responseClauseListModel.remove(selected);
            expectedResponseArea.setText(clauseService.toCanonicalText(toClauseList(responseClauseListModel)));
        }
    }

    private void moveSelectedClause(int delta) {
        int selected = responseClauseList.getSelectedIndex();
        int target = selected + delta;
        if (selected < 0 || target < 0 || target >= responseClauseListModel.size()) {
            return;
        }
        ExpectedResponseClause tmp = responseClauseListModel.get(selected);
        responseClauseListModel.set(selected, responseClauseListModel.get(target));
        responseClauseListModel.set(target, tmp);
        responseClauseList.setSelectedIndex(target);
        expectedResponseArea.setText(clauseService.toCanonicalText(toClauseList(responseClauseListModel)));
    }

    private void loadExpectedResponseClauses(TestDraft draft) {
        responseClauseListModel.clear();
        List<ExpectedResponseClause> clauses = draft.getExpectedResponseClauses().isEmpty()
                ? clauseService.parseCanonicalText(nvl(draft.getExpectedResponse()))
                : draft.getExpectedResponseClauses();
        for (ExpectedResponseClause clause : clauses) {
            responseClauseListModel.addElement(clause);
        }
    }

    private List<ExpectedResponseClause> toClauseList(DefaultListModel<ExpectedResponseClause> model) {
        List<ExpectedResponseClause> clauses = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            ExpectedResponseClause clause = model.get(i);
            if (clause != null) {
                clauses.add(clause);
            }
        }
        return clauses;
    }

    private void refreshCoverageList() {
        if (coverageListModel == null) {
            return;
        }

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

        coverageListModel.clear();
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

    private void setTestDetailsEnabled(boolean enabled) {
        typeCombo.setEnabled(enabled);
        resourceTypeCombo.setEnabled(enabled);
        informationElementCombo.setEnabled(enabled);
        labelField.setEnabled(enabled);
        prefLabelField.setEnabled(enabled);
        dimensionCombo.setEnabled(enabled);
        criterionCombo.setEnabled(enabled);
        useCaseRefCombo.setEnabled(enabled);
        responseConditionField.setEnabled(enabled);
        responseValueCombo.setEnabled(enabled);
        responseCommentField.setEnabled(enabled);
        responseClauseList.setEnabled(enabled);
        expectedResponseArea.setEnabled(enabled);
        notesArea.setEnabled(enabled);
        hasSourceAuthorityCheck.setEnabled(enabled);
        hasParametersCheck.setEnabled(enabled);
        addIfButton.setEnabled(enabled);
        addElseButton.setEnabled(enabled);
        removeClauseButton.setEnabled(enabled);
        moveUpButton.setEnabled(enabled);
        moveDownButton.setEnabled(enabled);
        saveDraftButton.setEnabled(enabled);
        if (ieInsertPanel != null) {
            ieInsertPanel.setEnabled(enabled);
            for (java.awt.Component c : ieInsertPanel.getComponents()) {
                c.setEnabled(enabled);
            }
        }
        if (testDetailsPanel != null) {
            testDetailsPanel.setEnabled(enabled);
        }
    }
}
