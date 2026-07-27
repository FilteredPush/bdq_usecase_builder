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
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
 * <p>Phase 3 enhancements:</p>
 * <ul>
 *   <li>Multi-valued information elements with explicit ActedUpon / Consulted roles.</li>
 *   <li>Compact two-column layout: Type+ResourceType on one row; Dimension+Criterion on one row;
 *       source authority and parameters checkboxes on one row.</li>
 *   <li>Save button disabled until a label is provided.</li>
 *   <li>Label auto-suggestion fires on every IE change; manual "Suggest" button overrides user edits.</li>
 *   <li>Source-authority and parameters flags are auto-detected from the expected-response text.</li>
 * </ul>
 */
public class NewTestPage extends WizardPage {

    // List of drafts being edited in this session
    private final DefaultListModel<TestDraft> listModel = new DefaultListModel<>();
    private JList<TestDraft> draftList;

    // ---- Form fields ----

    // Compact row 0: Type + Resource type
    private JComboBox<TestType> typeCombo;
    private JComboBox<ResourceType> resourceTypeCombo;

    // Multi-IE panel (row 1)
    private JComboBox<String> ieAddCombo;        // combo to select/type the term to add
    private JRadioButton actedUponRadio;
    private JRadioButton consultedRadio;
    private JButton addIeButton;
    private DefaultListModel<String> actedUponListModel;
    private DefaultListModel<String> consultedListModel;
    private JList<String> actedUponList;
    private JList<String> consultedList;
    private JButton removeActedUponButton;
    private JButton removeConsultedButton;

    // Label row (row 2) + Suggest button
    private JTextField labelField;
    private JButton suggestLabelsButton;
    private JTextField prefLabelField;

    // Compact row 3: Dimension + Criterion/Enhancement
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

    // Compact: both checkboxes on one row
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
    private static final float IE_BUTTON_FONT_SIZE = 11.0f;
    /** Pre-compiled pattern for detecting bdqffdq: parameter references in expected responses. */
    private static final java.util.regex.Pattern PARAM_PATTERN =
            java.util.regex.Pattern.compile("\\bbdqffdq:[A-Za-z]*[Pp]arameter\\b");

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
                        + "Convention: select Type first, pick Information Elements (ActedUpon/Consulted), "
                        + "then labels are suggested automatically. Fields marked <b>*</b> are required.</html>");
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

        // Multi-IE fields
        ieAddCombo = new JComboBox<>();
        ieAddCombo.setEditable(true);
        ieAddCombo.setToolTipText("Select or type the information element to add");
        actedUponRadio = new JRadioButton("ActedUpon", true);
        actedUponRadio.setToolTipText("The test acts directly on this element");
        consultedRadio = new JRadioButton("Consulted");
        consultedRadio.setToolTipText("The test consults this element without directly modifying it");
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(actedUponRadio);
        roleGroup.add(consultedRadio);
        addIeButton = new JButton("Add");
        addIeButton.setToolTipText("Add the selected term to the appropriate list");
        addIeButton.addActionListener(e -> addIeToList());

        actedUponListModel = new DefaultListModel<>();
        actedUponList = new JList<>(actedUponListModel);
        actedUponList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        actedUponList.setVisibleRowCount(3);
        consultedListModel = new DefaultListModel<>();
        consultedList = new JList<>(consultedListModel);
        consultedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        consultedList.setVisibleRowCount(3);

        removeActedUponButton = new JButton("Remove");
        removeActedUponButton.setFont(removeActedUponButton.getFont().deriveFont(11.0f));
        removeActedUponButton.addActionListener(e -> removeSelectedIes(actedUponList, actedUponListModel));
        removeConsultedButton = new JButton("Remove");
        removeConsultedButton.setFont(removeConsultedButton.getFont().deriveFont(11.0f));
        removeConsultedButton.addActionListener(e -> removeSelectedIes(consultedList, consultedListModel));

        labelField = new JTextField(30);
        labelField.setToolTipText("Machine-readable label: TESTTYPE_IE_EVALUATION (auto-suggested; required)");
        suggestLabelsButton = new JButton("Suggest");
        suggestLabelsButton.setToolTipText("Generate/regenerate label and preferred label from current type and IEs");
        suggestLabelsButton.addActionListener(e -> forceApplySuggestions());

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

        // ---- Row layout ----
        int row = 0;

        // Row 0: compact – Type + Resource type on one row
        addRow(testDetailsPanel, "Type *:", buildTypeResPanel(), row++);

        // Row 1: Multi-IE panel (spans full width, with some height)
        testDetailsPanel.add(new JLabel("<html>Info elements *:</html>"), labelConstraints(row));
        GridBagConstraints iefc = fieldConstraints(row++);
        iefc.fill = GridBagConstraints.BOTH;
        iefc.weighty = 0.25;
        testDetailsPanel.add(buildIePanel(), iefc);

        // Row 2: Label + Suggest button
        addRow(testDetailsPanel, "Label *:", buildLabelPanel(), row++);

        // Row 3: Preferred label
        addRow(testDetailsPanel, "Preferred label *:", prefLabelField, row++);

        // Row 4: compact – Dimension + Criterion/Enhancement on one row
        addRow(testDetailsPanel, "Dimension *:", buildDimCritPanel(), row++);

        // Row 5: Use-case reference
        addRow(testDetailsPanel, "Use-case reference *:", useCaseRefCombo, row++);

        // Row 6: IE quick-insert helpers
        GridBagConstraints lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Insert into condition:"), lc);
        ieInsertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        GridBagConstraints fc = fieldConstraints(row++);
        testDetailsPanel.add(ieInsertPanel, fc);

        // Row 7: IF condition
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

        // Row 8: Comment template
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Comment template:"), lc);
        fc = fieldConstraints(row++);
        testDetailsPanel.add(responseCommentField, fc);

        // Row 9: Clauses list
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Clauses:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        testDetailsPanel.add(new JScrollPane(responseClauseList), fc);

        // Row 10: Clause control buttons
        JPanel clauseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        addElseButton = new JButton("Add ELSE");
        addElseButton.setToolTipText("Add an ELSE (fallback) clause");
        addElseButton.addActionListener(e -> addExpectedResponseClause(true));
        JButton addOtherwiseButton = new JButton("Add otherwise →");
        addOtherwiseButton.setToolTipText("Quick-add the type-appropriate else clause (e.g. otherwise NOT_COMPLIANT)");
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

        // Row 11: Expected response preview
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Expected response:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.6;
        testDetailsPanel.add(new JScrollPane(expectedResponseArea), fc);

        // Row 12: Notes
        lc = labelConstraints(row);
        testDetailsPanel.add(new JLabel("Notes:"), lc);
        fc = fieldConstraints(row++);
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.4;
        testDetailsPanel.add(new JScrollPane(notesArea), fc);

        // Row 13: compact – both checkboxes on one row
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        checkboxPanel.add(hasSourceAuthorityCheck);
        checkboxPanel.add(hasParametersCheck);
        fc = fieldConstraints(row++);
        testDetailsPanel.add(checkboxPanel, fc);

        // Row 14: Save button
        saveDraftButton = new JButton("Save draft");
        saveDraftButton.setEnabled(false); // disabled until a label is entered
        saveDraftButton.addActionListener(e -> saveCurrentDraft());
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = row;
        bc.gridx = 1;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 0, 0, 0);
        testDetailsPanel.add(saveDraftButton, bc);

        // ---- Wire up listeners ----

        // Type combo: refresh criterion picklist + response values + suggestions
        typeCombo.addActionListener(e -> {
            refreshCriterionPicklistByType();
            applySuggestions();
        });

        // IE add combo: trigger suggestions on every keystroke in the editor
        if (ieAddCombo.getEditor() != null && ieAddCombo.getEditor().getEditorComponent() instanceof JTextField) {
            JTextField editorField = (JTextField) ieAddCombo.getEditor().getEditorComponent();
            editorField.getDocument().addDocumentListener(makeDocumentListener(this::applySuggestions));
        }
        // Also on action (Enter / selection)
        ieAddCombo.addActionListener(e -> applySuggestions());

        // Dimension / criterion combos: trigger suggestions
        dimensionCombo.addActionListener(e -> applySuggestions());
        criterionCombo.addActionListener(e -> applySuggestions());

        // Track manual label edits to suppress further auto-suggestions
        labelField.getDocument().addDocumentListener(makeDocumentListener(this::onLabelManualEdit));
        prefLabelField.getDocument().addDocumentListener(makeDocumentListener(this::onPrefLabelManualEdit));

        // Track label changes to enable/disable save button
        labelField.getDocument().addDocumentListener(makeDocumentListener(this::updateSaveButtonState));

        return testDetailsPanel;
    }

    // -----------------------------------------------------------------------
    // Compound panels (for compact rows)
    // -----------------------------------------------------------------------

    /** Builds [typeCombo | "Resource type:" | resourceTypeCombo] for a compact row. */
    private JPanel buildTypeResPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridx = 0;
        p.add(typeCombo, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.insets = new Insets(0, 10, 0, 4);
        gc.gridx = 1;
        p.add(new JLabel("Resource type:"), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridx = 2;
        p.add(resourceTypeCombo, gc);
        return p;
    }

    /** Builds the multi-IE management panel with ActedUpon and Consulted lists. */
    private JPanel buildIePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        // Top: add-row (combo + role radios + Add button)
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        addRow.add(new JLabel("Term:"));
        ieAddCombo.setPreferredSize(new java.awt.Dimension(220, ieAddCombo.getPreferredSize().height));
        addRow.add(ieAddCombo);
        addRow.add(actedUponRadio);
        addRow.add(consultedRadio);
        addRow.add(addIeButton);
        panel.add(addRow, BorderLayout.NORTH);

        // Center: two lists side by side
        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 6, 0));

        JPanel actedPanel = new JPanel(new BorderLayout(2, 2));
        actedPanel.setBorder(BorderFactory.createTitledBorder("Acted Upon"));
        actedPanel.add(new JScrollPane(actedUponList), BorderLayout.CENTER);
        JPanel actedBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actedBtns.add(removeActedUponButton);
        actedPanel.add(actedBtns, BorderLayout.SOUTH);
        listsPanel.add(actedPanel);

        JPanel consultedPanel = new JPanel(new BorderLayout(2, 2));
        consultedPanel.setBorder(BorderFactory.createTitledBorder("Consulted"));
        consultedPanel.add(new JScrollPane(consultedList), BorderLayout.CENTER);
        JPanel consultedBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        consultedBtns.add(removeConsultedButton);
        consultedPanel.add(consultedBtns, BorderLayout.SOUTH);
        listsPanel.add(consultedPanel);

        panel.add(listsPanel, BorderLayout.CENTER);
        return panel;
    }

    /** Builds [labelField | "Suggest" button] for the label row. */
    private JPanel buildLabelPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.add(labelField, BorderLayout.CENTER);
        p.add(suggestLabelsButton, BorderLayout.EAST);
        return p;
    }

    /** Builds [dimensionCombo | "Criterion/Enhancement:" | criterionCombo] for a compact row. */
    private JPanel buildDimCritPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridx = 0;
        p.add(dimensionCombo, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.insets = new Insets(0, 10, 0, 4);
        gc.gridx = 1;
        p.add(new JLabel("Criterion/Enhancement:"), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridx = 2;
        p.add(criterionCombo, gc);
        return p;
    }

    /** Creates a simple DocumentListener that calls the given runnable on any change. */
    private static javax.swing.event.DocumentListener makeDocumentListener(Runnable action) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        };
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

        // Load multi-IE lists
        actedUponListModel.clear();
        for (String ie : draft.getActedUponElements()) {
            actedUponListModel.addElement(ie);
        }
        // Legacy single-field backward compat: if no actedUpon list but informationElement is set, add it
        if (actedUponListModel.isEmpty() && draft.getInformationElement() != null
                && !draft.getInformationElement().trim().isEmpty()) {
            actedUponListModel.addElement(draft.getInformationElement().trim());
        }
        consultedListModel.clear();
        for (String ie : draft.getConsultedElements()) {
            consultedListModel.addElement(ie);
        }

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
        updateSaveButtonState();
    }

    private void saveCurrentDraft() {
        TestDraft draft = draftList.getSelectedValue();
        if (draft == null) {
            return;
        }
        draft.setType((TestType) typeCombo.getSelectedItem());
        draft.setResourceType((ResourceType) resourceTypeCombo.getSelectedItem());

        // Save multi-IE lists
        draft.setActedUponElements(listModelToList(actedUponListModel));
        draft.setConsultedElements(listModelToList(consultedListModel));
        // Keep legacy field in sync with the first acted-upon element
        draft.setInformationElement(actedUponListModel.isEmpty()
                ? "" : actedUponListModel.get(0));

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
        actedUponListModel.clear();
        consultedListModel.clear();
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
        updateSaveButtonState();
    }

    private void refreshPicklists() {
        // Populate ie add combo with use-case IEs first, then full vocabulary
        List<String> infoTerms = new ArrayList<>();
        for (InformationElementRef ref : state.getInformationElements()) {
            if (ref.getQname() != null && !ref.getQname().trim().isEmpty()) {
                infoTerms.add(ref.getQname().trim());
            }
        }
        // Also add full vocabulary terms (deduplicated)
        LinkedHashSet<String> allTerms = new LinkedHashSet<>(infoTerms);
        allTerms.addAll(vocabularyService.getInformationElementTerms());
        resetComboItems(ieAddCombo, new ArrayList<>(allTerms));
        ieAddCombo.setSelectedItem("");

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
    // Multi-IE management
    // -----------------------------------------------------------------------

    /** Adds the term currently in ieAddCombo to the appropriate list (ActedUpon or Consulted). */
    private void addIeToList() {
        String term = getSelectedComboText(ieAddCombo);
        if (term.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select or type an information element term.",
                    "No term",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (actedUponRadio.isSelected()) {
            if (!containsItem(actedUponListModel, term)) {
                actedUponListModel.addElement(term);
            }
        } else {
            if (!containsItem(consultedListModel, term)) {
                consultedListModel.addElement(term);
            }
        }
        // Trigger label suggestion after adding an IE
        applySuggestions();
    }

    /** Removes the selected items from the given list. */
    private void removeSelectedIes(JList<String> list, DefaultListModel<String> model) {
        List<String> selected = list.getSelectedValuesList();
        for (String s : selected) {
            model.removeElement(s);
        }
        // Refresh label suggestion (first IE may have changed)
        applySuggestions();
    }

    private boolean containsItem(DefaultListModel<String> model, String item) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    private List<String> listModelToList(DefaultListModel<String> model) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            result.add(model.get(i));
        }
        return result;
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
        updateSaveButtonState();
    }

    /**
     * Forces label and prefLabel to be regenerated regardless of the user-override flags.
     * Called when the user explicitly clicks the Suggest button.
     */
    private void forceApplySuggestions() {
        TestDraft scratch = buildScratchDraft();
        scratch.setLabelUserOverridden(false);
        scratch.setPrefLabelUserOverridden(false);
        String suggestedLabel = labelSuggestionService.suggestLabel(scratch);
        String suggestedPrefLabel = labelSuggestionService.suggestPrefLabel(scratch);

        // Update the real draft's override flags
        TestDraft current = draftList.getSelectedValue();
        if (current != null) {
            current.setLabelUserOverridden(false);
            current.setPrefLabelUserOverridden(false);
        }

        updatingLabel = true;
        if (suggestedLabel != null) {
            labelField.setText(suggestedLabel);
        }
        if (suggestedPrefLabel != null) {
            prefLabelField.setText(suggestedPrefLabel);
        }
        updatingLabel = false;
        updateSaveButtonState();
    }

    /** Builds a scratch TestDraft from the current form state for suggestion purposes. */
    private TestDraft buildScratchDraft() {
        TestDraft scratch = new TestDraft();
        scratch.setType((TestType) typeCombo.getSelectedItem());
        // Use the actedUpon list as the primary IEs for label generation
        for (int i = 0; i < actedUponListModel.size(); i++) {
            scratch.addActedUponElement(actedUponListModel.get(i));
        }
        // If empty, also try the currently typed term in the add combo
        if (scratch.getActedUponElements().isEmpty()) {
            String ie = getSelectedComboText(ieAddCombo);
            if (!ie.isEmpty()) {
                scratch.addActedUponElement(ie);
            }
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

    /** Updates the Save button enabled state based on whether a label has been entered. */
    private void updateSaveButtonState() {
        if (saveDraftButton != null) {
            boolean hasLabel = labelField != null && !labelField.getText().trim().isEmpty();
            boolean draftSelected = draftList != null && draftList.getSelectedValue() != null;
            saveDraftButton.setEnabled(hasLabel && draftSelected);
        }
    }

    // -----------------------------------------------------------------------
    // Auto-detection of source authority / parameter flags from expected response
    // -----------------------------------------------------------------------

    /**
     * Scans the expected-response text and automatically sets the source-authority
     * and parameters checkboxes when relevant tokens are detected.
     *
     * <p>Users may still override the checkboxes after auto-detection.</p>
     */
    private void autoDetectSourceAuthorityAndParameters() {
        String response = expectedResponseArea.getText();
        if (response == null || response.isEmpty()) {
            return;
        }
        // sourceAuthority token → source authority flag
        if (response.contains("sourceAuthority")) {
            hasSourceAuthorityCheck.setSelected(true);
        }
        // bdqffdq:Parameter or any "...Parameter" token → parameters flag
        if (response.contains("bdqffdq:Parameter")
                || response.contains("Parameter")
                || PARAM_PATTERN.matcher(response).find()) {
            hasParametersCheck.setSelected(true);
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
            b.setFont(b.getFont().deriveFont(IE_BUTTON_FONT_SIZE));
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
            b.setFont(b.getFont().deriveFont(IE_BUTTON_FONT_SIZE));
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
        autoDetectSourceAuthorityAndParameters();
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
        autoDetectSourceAuthorityAndParameters();
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
        ieAddCombo.setEnabled(enabled);
        actedUponRadio.setEnabled(enabled);
        consultedRadio.setEnabled(enabled);
        addIeButton.setEnabled(enabled);
        actedUponList.setEnabled(enabled);
        consultedList.setEnabled(enabled);
        removeActedUponButton.setEnabled(enabled);
        removeConsultedButton.setEnabled(enabled);
        labelField.setEnabled(enabled);
        suggestLabelsButton.setEnabled(enabled);
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
        if (enabled) {
            updateSaveButtonState(); // re-evaluate based on label content
        } else {
            saveDraftButton.setEnabled(false);
        }
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
