package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.GapAnalysisService;
import org.filteredpush.bdq.usecasebuilder.service.InformationElementTermService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wizard page for requirement-to-test gap analysis.
 *
 * <p>Phase 3 redesign: two-pane model with explicit Add/Remove/Bulk buttons,
 * search/filter for tests, color-coded coverage indicators, and row-level +
 * summary coverage counts.</p>
 *
 * <p>Left pane: requirements / information elements (the matrix rows).</p>
 * <p>Right pane: linked existing tests and linked new tests for the selected
 * requirement.</p>
 */
public class GapAnalysisPage extends WizardPage {

    private final GapAnalysisService gapAnalysisService = new GapAnalysisService();
    private final TestCatalogService catalogService;
    private CoverageTableModel tableModel;
    private TableRowSorter<CoverageTableModel> tableSorter;
    private JTable matrixTable;
    private JTextField tableSearchField;

    // Available tests (with filter)
    private DefaultListModel<String> existingTestsModel;
    private DefaultListModel<String> newTestsModel;
    private JList<String> existingTestsList;
    private JList<String> newTestsList;
    private JTextField existingSearchField;
    private JTextField newSearchField;

    // Mapped (linked) tests
    private DefaultListModel<String> mappedExistingTestsModel;
    private DefaultListModel<String> mappedNewTestsModel;
    private JList<String> mappedExistingTestsList;
    private JList<String> mappedNewTestsList;

    private JTextArea rationaleArea;
    private JTextArea notesArea;
    private JLabel coverageSummaryLabel;

    private final Map<String, String> existingOptionToIri = new LinkedHashMap<>();
    private final Map<String, String> iriToExistingOption = new LinkedHashMap<>();
    private final Map<String, String> newOptionToLabel = new LinkedHashMap<>();
    private final Map<String, TestCatalogEntry> catalogEntriesByIri = new LinkedHashMap<>();

    // All available options (unfiltered) for search
    private final List<String> allExistingOptions = new ArrayList<>();
    private final List<String> allNewOptions = new ArrayList<>();

    public GapAnalysisPage(ProjectState state, TestCatalogService catalogService) {
        super(state);
        this.catalogService = catalogService;
        for (TestCatalogEntry entry : catalogService.getEntries()) {
            catalogEntriesByIri.put(entry.getIri(), entry);
        }
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return "Gap Analysis Matrix";
    }

    @Override
    public void onEnter() {
        markVisited();
        List<RequirementCoverage> rows = gapAnalysisService.buildRows(state);
        tableModel.load(rows);
        state.setRequirementCoverageRows(rows);
        refreshTestOptionsForRow(getSelectedRow());
        refreshMappedLists(getSelectedRow());
        refreshCoverageSummary();
    }

    @Override
    public void onLeave() {
        state.setRequirementCoverageRows(tableModel.getRows());
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return new ArrayList<>();
    }

    @Override
    public CompletionStatus getCompletionStatus() {
        if (!isVisited()) {
            return CompletionStatus.NOT_STARTED;
        }
        List<RequirementCoverage> rows = tableModel.getRows();
        if (rows.isEmpty()) {
            return CompletionStatus.IN_PROGRESS;
        }
        int gaps = 0;
        for (RequirementCoverage row : rows) {
            if (row.computeStatus() == RequirementCoverage.CoverageStatus.GAP) {
                gaps++;
            }
        }
        if (gaps == 0) {
            return CompletionStatus.READY;
        }
        if (gaps < rows.size()) {
            return CompletionStatus.IN_PROGRESS;
        }
        return CompletionStatus.NEEDS_ATTENTION;
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Requirement coverage matrix (Gap Analysis).</b><br>"
                + "Select a requirement row on the left, then use the right panes to "
                + "<b>Add Existing Test</b>, <b>Add New Draft Test</b>, or <b>Remove Link</b>.<br>"
                + "Coverage status auto-updates: <font color='green'>Covered</font> | "
                + "<font color='orange'>Partially Covered</font> | <font color='red'>Gap</font>. "
                + "Counts shown at top.</html>");
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(intro, BorderLayout.NORTH);

        // Main split: left = matrix table, right = assignment panes
        JPanel mainSplit = new JPanel(new GridLayout(1, 2, 8, 0));

        // Left: matrix table
        mainSplit.add(buildMatrixPanel());

        // Right: assignment panes
        mainSplit.add(buildAssignmentPanel());

        add(mainSplit, BorderLayout.CENTER);

        // Bottom: coverage summary + rationale
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Requirements / Information Elements"));

        // Search field for the matrix table
        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchPanel.add(new JLabel("Filter: "), BorderLayout.WEST);
        tableSearchField = new JTextField();
        tableSearchField.setToolTipText("Filter requirements by text");
        tableSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyTableFilter(); }
        });
        searchPanel.add(tableSearchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        tableModel = new CoverageTableModel();
        matrixTable = new JTable(tableModel);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matrixTable.setRowHeight(22);
        matrixTable.setDefaultRenderer(Object.class, new CoverageStatusCellRenderer());

        tableSorter = new TableRowSorter<>(tableModel);
        matrixTable.setRowSorter(tableSorter);

        matrixTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowDetails();
            }
        });

        // Column widths
        matrixTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        matrixTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        matrixTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        matrixTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        panel.add(new JScrollPane(matrixTable), BorderLayout.CENTER);

        coverageSummaryLabel = new JLabel("Coverage: 0/0 covered");
        coverageSummaryLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(coverageSummaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildAssignmentPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));

        // Top: Existing tests assignment
        panel.add(buildExistingTestsPane());

        // Bottom: New tests assignment
        panel.add(buildNewTestsPane());

        return panel;
    }

    private JPanel buildExistingTestsPane() {
        JPanel outer = new JPanel(new GridLayout(1, 2, 4, 0));
        outer.setBorder(BorderFactory.createTitledBorder("Existing Tests"));

        // Available
        JPanel availPanel = new JPanel(new BorderLayout(4, 4));
        availPanel.setBorder(BorderFactory.createTitledBorder("Available"));
        JPanel existingSearchPanel = new JPanel(new BorderLayout(4, 0));
        existingSearchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        existingSearchField = new JTextField();
        existingSearchField.setToolTipText("Search available existing tests");
        existingSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filterExistingTests(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filterExistingTests(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filterExistingTests(); }
        });
        existingSearchPanel.add(existingSearchField, BorderLayout.CENTER);
        availPanel.add(existingSearchPanel, BorderLayout.NORTH);
        existingTestsModel = new DefaultListModel<>();
        existingTestsList = new JList<>(existingTestsModel);
        existingTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availPanel.add(new JScrollPane(existingTestsList), BorderLayout.CENTER);
        JPanel existingBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        JButton addExistingBtn = new JButton("Add Existing Test →");
        addExistingBtn.setToolTipText("Link selected existing tests to this requirement");
        addExistingBtn.addActionListener(e -> linkSelectedExistingTests());
        JButton addAllExistingBtn = new JButton("Add All →");
        addAllExistingBtn.setToolTipText("Link all visible existing tests to this requirement");
        addAllExistingBtn.addActionListener(e -> linkAllExistingTests());
        existingBtns.add(addExistingBtn);
        existingBtns.add(addAllExistingBtn);
        availPanel.add(existingBtns, BorderLayout.SOUTH);
        outer.add(availPanel);

        // Linked
        JPanel linkedPanel = new JPanel(new BorderLayout(4, 4));
        linkedPanel.setBorder(BorderFactory.createTitledBorder("Linked"));
        mappedExistingTestsModel = new DefaultListModel<>();
        mappedExistingTestsList = new JList<>(mappedExistingTestsModel);
        mappedExistingTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        linkedPanel.add(new JScrollPane(mappedExistingTestsList), BorderLayout.CENTER);
        JPanel unlinkExistingBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        JButton removeExistingBtn = new JButton("← Remove Link");
        removeExistingBtn.setToolTipText("Unlink selected existing tests from this requirement");
        removeExistingBtn.addActionListener(e -> unlinkMappedExistingTests());
        JButton removeAllExistingBtn = new JButton("← Remove All");
        removeAllExistingBtn.setToolTipText("Remove all existing test links from this requirement");
        removeAllExistingBtn.addActionListener(e -> unlinkAllExistingTests());
        unlinkExistingBtns.add(removeExistingBtn);
        unlinkExistingBtns.add(removeAllExistingBtn);
        linkedPanel.add(unlinkExistingBtns, BorderLayout.SOUTH);
        outer.add(linkedPanel);

        return outer;
    }

    private JPanel buildNewTestsPane() {
        JPanel outer = new JPanel(new GridLayout(1, 2, 4, 0));
        outer.setBorder(BorderFactory.createTitledBorder("New Draft Tests"));

        // Available
        JPanel availPanel = new JPanel(new BorderLayout(4, 4));
        availPanel.setBorder(BorderFactory.createTitledBorder("Available"));
        JPanel newSearchPanel = new JPanel(new BorderLayout(4, 0));
        newSearchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        newSearchField = new JTextField();
        newSearchField.setToolTipText("Search available new draft tests");
        newSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filterNewTests(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filterNewTests(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filterNewTests(); }
        });
        newSearchPanel.add(newSearchField, BorderLayout.CENTER);
        availPanel.add(newSearchPanel, BorderLayout.NORTH);
        newTestsModel = new DefaultListModel<>();
        newTestsList = new JList<>(newTestsModel);
        newTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availPanel.add(new JScrollPane(newTestsList), BorderLayout.CENTER);
        JPanel newBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        JButton addNewBtn = new JButton("Add New Draft Test →");
        addNewBtn.setToolTipText("Link selected new draft tests to this requirement");
        addNewBtn.addActionListener(e -> linkSelectedNewTests());
        JButton addAllNewBtn = new JButton("Add All →");
        addAllNewBtn.setToolTipText("Link all visible new draft tests to this requirement");
        addAllNewBtn.addActionListener(e -> linkAllNewTests());
        newBtns.add(addNewBtn);
        newBtns.add(addAllNewBtn);
        availPanel.add(newBtns, BorderLayout.SOUTH);
        outer.add(availPanel);

        // Linked
        JPanel linkedPanel = new JPanel(new BorderLayout(4, 4));
        linkedPanel.setBorder(BorderFactory.createTitledBorder("Linked"));
        mappedNewTestsModel = new DefaultListModel<>();
        mappedNewTestsList = new JList<>(mappedNewTestsModel);
        mappedNewTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        linkedPanel.add(new JScrollPane(mappedNewTestsList), BorderLayout.CENTER);
        JPanel unlinkNewBtns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        JButton removeNewBtn = new JButton("← Remove Link");
        removeNewBtn.setToolTipText("Unlink selected new draft tests from this requirement");
        removeNewBtn.addActionListener(e -> unlinkMappedNewTests());
        JButton removeAllNewBtn = new JButton("← Remove All");
        removeAllNewBtn.setToolTipText("Remove all new draft test links from this requirement");
        removeAllNewBtn.addActionListener(e -> unlinkAllNewTests());
        unlinkNewBtns.add(removeNewBtn);
        unlinkNewBtns.add(removeAllNewBtn);
        linkedPanel.add(unlinkNewBtns, BorderLayout.SOUTH);
        outer.add(linkedPanel);

        return outer;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Notes for selected requirement"));
        rationaleArea = new JTextArea(2, 40);
        rationaleArea.setLineWrap(true);
        rationaleArea.setWrapStyleWord(true);
        notesArea = new JTextArea(2, 40);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JPanel labels = new JPanel(new GridLayout(1, 2, 8, 0));
        labels.add(new JLabel("Partial coverage rationale:"));
        labels.add(new JLabel("Notes:"));
        panel.add(labels, BorderLayout.NORTH);
        JPanel areas = new JPanel(new GridLayout(1, 2, 8, 0));
        areas.add(new JScrollPane(rationaleArea));
        areas.add(new JScrollPane(notesArea));
        panel.add(areas, BorderLayout.CENTER);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Table filter
    // -----------------------------------------------------------------------

    private void applyTableFilter() {
        String text = tableSearchField.getText().trim();
        if (text.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            try {
                tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            } catch (java.util.regex.PatternSyntaxException e) {
                // ignore invalid regex
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test option filtering (search)
    // -----------------------------------------------------------------------

    private void filterExistingTests() {
        String query = existingSearchField.getText().trim().toLowerCase(Locale.ROOT);
        existingTestsModel.clear();
        for (String opt : allExistingOptions) {
            if (query.isEmpty() || opt.toLowerCase(Locale.ROOT).contains(query)) {
                existingTestsModel.addElement(opt);
            }
        }
    }

    private void filterNewTests() {
        String query = newSearchField.getText().trim().toLowerCase(Locale.ROOT);
        newTestsModel.clear();
        for (String opt : allNewOptions) {
            if (query.isEmpty() || opt.toLowerCase(Locale.ROOT).contains(query)) {
                newTestsModel.addElement(opt);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Row detail loading
    // -----------------------------------------------------------------------

    private void refreshTestOptionsForRow(RequirementCoverage row) {
        existingOptionToIri.clear();
        iriToExistingOption.clear();
        newOptionToLabel.clear();
        allExistingOptions.clear();
        allNewOptions.clear();

        Set<String> rowTerms = termsForRow(row);

        for (String iri : state.getSelectedExistingTestIris()) {
            TestCatalogEntry entry = findCatalogEntry(iri);
            if (entry == null) {
                continue;
            }
            Set<String> testTerms = InformationElementTermService.extractQualifiedTerms(
                    entry.getLabel(), entry.getPrefLabel());
            if (!rowTerms.isEmpty()
                    && !InformationElementTermService.matchesAnySelectedTerm(testTerms, rowTerms)) {
                continue;
            }
            String option = displayNameForEntry(entry);
            if (existingOptionToIri.containsKey(option)) {
                option = option + " <" + iri + ">";
            }
            existingOptionToIri.put(option, iri);
            iriToExistingOption.put(iri, option);
            allExistingOptions.add(option);
        }

        for (TestDraft draft : state.getNewTestDrafts()) {
            String label = draft.getLabel() != null && !draft.getLabel().trim().isEmpty()
                    ? draft.getLabel().trim() : draft.toString();
            String infoElement = draft.getInformationElement() != null
                    ? draft.getInformationElement().trim() : "";
            String normalizedInfoElement = infoElement.toLowerCase(Locale.ROOT);
            if (!rowTerms.isEmpty() && !rowTerms.contains(normalizedInfoElement)) {
                continue;
            }
            String option = label + (infoElement.isEmpty() ? "" : " [" + infoElement + "]");
            newOptionToLabel.put(option, label);
            allNewOptions.add(option);
        }

        // Populate filtered lists
        existingTestsModel.clear();
        allExistingOptions.forEach(existingTestsModel::addElement);
        newTestsModel.clear();
        allNewOptions.forEach(newTestsModel::addElement);
    }

    private void loadSelectedRowDetails() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            rationaleArea.setText("");
            notesArea.setText("");
            refreshTestOptionsForRow(null);
            refreshMappedLists(null);
            return;
        }
        rationaleArea.setText(row.getPartialCoverageRationale() != null
                ? row.getPartialCoverageRationale() : "");
        notesArea.setText(row.getNotes() != null ? row.getNotes() : "");
        refreshTestOptionsForRow(row);
        refreshMappedLists(row);
    }

    private RequirementCoverage getSelectedRow() {
        int row = matrixTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = matrixTable.convertRowIndexToModel(row);
        return tableModel.getRow(modelRow);
    }

    // -----------------------------------------------------------------------
    // Link / unlink operations
    // -----------------------------------------------------------------------

    private void linkSelectedExistingTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String selected : existingTestsList.getSelectedValuesList()) {
            String iri = existingOptionToIri.get(selected);
            if (iri != null && !row.getLinkedExistingTests().contains(iri)) {
                row.getLinkedExistingTests().add(iri);
            }
        }
        afterLinkChange(row);
    }

    private void linkAllExistingTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String opt : allExistingOptions) {
            String iri = existingOptionToIri.get(opt);
            if (iri != null && !row.getLinkedExistingTests().contains(iri)) {
                row.getLinkedExistingTests().add(iri);
            }
        }
        afterLinkChange(row);
    }

    private void unlinkMappedExistingTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String selected : mappedExistingTestsList.getSelectedValuesList()) {
            String iri = resolveMappedExistingSelectionToIri(selected, row);
            if (iri != null) {
                row.getLinkedExistingTests().remove(iri);
            }
        }
        afterLinkChange(row);
    }

    private void unlinkAllExistingTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedExistingTests().clear();
        afterLinkChange(row);
    }

    private void linkSelectedNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String selected : newTestsList.getSelectedValuesList()) {
            String label = newOptionToLabel.getOrDefault(selected, selected);
            if (!row.getLinkedNewTests().contains(label)) {
                row.getLinkedNewTests().add(label);
            }
        }
        afterLinkChange(row);
    }

    private void linkAllNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String opt : allNewOptions) {
            String label = newOptionToLabel.getOrDefault(opt, opt);
            if (!row.getLinkedNewTests().contains(label)) {
                row.getLinkedNewTests().add(label);
            }
        }
        afterLinkChange(row);
    }

    private void unlinkMappedNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedNewTests().removeAll(mappedNewTestsList.getSelectedValuesList());
        afterLinkChange(row);
    }

    private void unlinkAllNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedNewTests().clear();
        afterLinkChange(row);
    }

    private void afterLinkChange(RequirementCoverage row) {
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshMappedLists(row);
        refreshCoverageSummary();
    }

    private void saveDetails(RequirementCoverage row) {
        row.setPartialCoverageRationale(rationaleArea.getText().trim());
        row.setNotes(notesArea.getText().trim());
    }

    // -----------------------------------------------------------------------
    // Refresh helpers
    // -----------------------------------------------------------------------

    private void refreshCoverageSummary() {
        List<RequirementCoverage> rows = tableModel.getRows();
        int covered = gapAnalysisService.countCovered(rows);
        int gaps = (int) rows.stream()
                .filter(r -> r.computeStatus() == RequirementCoverage.CoverageStatus.GAP)
                .count();
        coverageSummaryLabel.setText("Coverage: " + covered + "/" + rows.size()
                + " covered | " + gaps + " gap(s)");
        if (gaps == 0 && !rows.isEmpty()) {
            coverageSummaryLabel.setForeground(new Color(0x2E7D32));
        } else if (gaps < rows.size()) {
            coverageSummaryLabel.setForeground(new Color(0xE65100));
        } else {
            coverageSummaryLabel.setForeground(Color.RED);
        }
    }

    private void refreshMappedLists(RequirementCoverage row) {
        mappedExistingTestsModel.clear();
        mappedNewTestsModel.clear();
        if (row == null) {
            return;
        }
        for (String iri : row.getLinkedExistingTests()) {
            TestCatalogEntry entry = findCatalogEntry(iri);
            mappedExistingTestsModel.addElement(entry != null ? displayNameForEntry(entry) : iri);
        }
        for (String label : row.getLinkedNewTests()) {
            mappedNewTestsModel.addElement(label);
        }
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------

    private Set<String> termsForRow(RequirementCoverage row) {
        Set<String> terms = new HashSet<>();
        if (row == null || row.getInformationElements() == null) {
            return terms;
        }
        for (String token : row.getInformationElements().split(",")) {
            String term = token.trim();
            if (!term.isEmpty()) {
                terms.add(term.toLowerCase(Locale.ROOT));
            }
        }
        return terms;
    }

    private TestCatalogEntry findCatalogEntry(String iri) {
        return catalogEntriesByIri.get(iri);
    }

    private String displayNameForEntry(TestCatalogEntry entry) {
        return entry.getLabel() != null && !entry.getLabel().trim().isEmpty()
                ? entry.getLabel().trim() : entry.getIri();
    }

    private String resolveMappedExistingSelectionToIri(String selected, RequirementCoverage row) {
        String direct = existingOptionToIri.get(selected);
        if (direct != null) {
            return direct;
        }
        for (String iri : row.getLinkedExistingTests()) {
            if (iri.equals(selected)) {
                return iri;
            }
            TestCatalogEntry entry = findCatalogEntry(iri);
            if (entry != null && displayNameForEntry(entry).equals(selected)) {
                return iri;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private final class CoverageTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {
                "Requirement",
                "Info Elements",
                "Existing Tests",
                "New Tests"
        };

        private final List<RequirementCoverage> rows = new ArrayList<>();

        void load(List<RequirementCoverage> data) {
            rows.clear();
            rows.addAll(data);
            fireTableDataChanged();
        }

        List<RequirementCoverage> getRows() {
            return rows;
        }

        RequirementCoverage getRow(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RequirementCoverage row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.getRequirementId() + " " + nvl(row.getRequirementSummary());
                case 1:
                    return nvl(row.getInformationElements());
                case 2:
                    return row.getLinkedExistingTests().size() + " linked";
                case 3:
                    return row.getLinkedNewTests().size() + " linked";
                default:
                    return "";
            }
        }

        private String nvl(String value) {
            return value != null ? value : "";
        }
    }

    // -----------------------------------------------------------------------
    // Cell renderer: color rows by coverage status
    // -----------------------------------------------------------------------

    private final class CoverageStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                RequirementCoverage rc = tableModel.getRow(modelRow);
                switch (rc.computeStatus()) {
                    case COVERED:
                        c.setBackground(new Color(0xE8F5E9));
                        break;
                    case PARTIALLY_COVERED:
                        c.setBackground(new Color(0xFFF8E1));
                        break;
                    case GAP:
                        c.setBackground(new Color(0xFFEBEE));
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                }
            }
            return c;
        }
    }
}
