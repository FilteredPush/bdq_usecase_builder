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
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wizard page for requirement-to-test gap analysis.
 */
public class GapAnalysisPage extends WizardPage {

    private final GapAnalysisService gapAnalysisService = new GapAnalysisService();
    private final TestCatalogService catalogService;
    private CoverageTableModel tableModel;
    private JTable matrixTable;
    private DefaultListModel<String> existingTestsModel;
    private DefaultListModel<String> newTestsModel;
    private DefaultListModel<String> mappedExistingTestsModel;
    private DefaultListModel<String> mappedNewTestsModel;
    private JList<String> existingTestsList;
    private JList<String> newTestsList;
    private JList<String> mappedExistingTestsList;
    private JList<String> mappedNewTestsList;
    private JTextArea rationaleArea;
    private JTextArea notesArea;
    private JLabel coverageSummaryLabel;
    private final Map<String, String> existingOptionToIri = new LinkedHashMap<>();
    private final Map<String, String> iriToExistingOption = new LinkedHashMap<>();
    private final Map<String, String> newOptionToLabel = new LinkedHashMap<>();
    private final Map<String, TestCatalogEntry> catalogEntriesByIri = new LinkedHashMap<>();

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

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel intro = new JLabel(
                "<html><b>Analyze requirement coverage.</b><br>"
                        + "Map requirement statements and information elements to existing/new tests.<br>"
                        + "Coverage status is auto-derived as Covered, Partially Covered, or Gap.</html>");
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(intro, BorderLayout.NORTH);

        tableModel = new CoverageTableModel();
        matrixTable = new JTable(tableModel);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matrixTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowDetails();
            }
        });
        add(new JScrollPane(matrixTable), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        JPanel mappingPanel = new JPanel(new java.awt.GridLayout(1, 2, 6, 6));
        mappingPanel.add(buildExistingTestMappingPanel());
        mappingPanel.add(buildNewTestMappingPanel());
        south.add(mappingPanel, BorderLayout.CENTER);

        JPanel rationalePanel = new JPanel(new BorderLayout(4, 4));
        rationalePanel.setBorder(BorderFactory.createTitledBorder("Partial coverage rationale / notes"));
        rationaleArea = new JTextArea(3, 40);
        notesArea = new JTextArea(2, 40);
        rationalePanel.add(new JLabel("Rationale:"), BorderLayout.NORTH);
        rationalePanel.add(new JScrollPane(rationaleArea), BorderLayout.CENTER);
        JPanel notesWrap = new JPanel(new BorderLayout());
        notesWrap.add(new JLabel("Notes:"), BorderLayout.NORTH);
        notesWrap.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        rationalePanel.add(notesWrap, BorderLayout.SOUTH);
        south.add(rationalePanel, BorderLayout.SOUTH);

        coverageSummaryLabel = new JLabel("Coverage: 0/0 covered");
        south.add(coverageSummaryLabel, BorderLayout.NORTH);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildExistingTestMappingPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Existing tests by acted-upon information element"));
        existingTestsModel = new DefaultListModel<>();
        existingTestsList = new JList<>(existingTestsModel);
        existingTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(existingTestsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton link = new JButton("Link selected");
        link.addActionListener(e -> linkSelectedExistingTests());
        buttons.add(link);
        panel.add(buttons, BorderLayout.SOUTH);
        JPanel mappedPanel = new JPanel(new BorderLayout(4, 4));
        mappedPanel.setBorder(BorderFactory.createTitledBorder("Mapped existing tests"));
        mappedExistingTestsModel = new DefaultListModel<>();
        mappedExistingTestsList = new JList<>(mappedExistingTestsModel);
        mappedExistingTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mappedPanel.add(new JScrollPane(mappedExistingTestsList), BorderLayout.CENTER);
        JButton unlinkMapped = new JButton("Remove mapped");
        unlinkMapped.addActionListener(e -> unlinkMappedExistingTests());
        mappedPanel.add(unlinkMapped, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout(4, 4));
        wrap.add(panel, BorderLayout.CENTER);
        wrap.add(mappedPanel, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildNewTestMappingPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("New tests by acted-upon information element"));
        newTestsModel = new DefaultListModel<>();
        newTestsList = new JList<>(newTestsModel);
        newTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(newTestsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton link = new JButton("Link selected");
        link.addActionListener(e -> linkSelectedNewTests());
        buttons.add(link);
        panel.add(buttons, BorderLayout.SOUTH);
        JPanel mappedPanel = new JPanel(new BorderLayout(4, 4));
        mappedPanel.setBorder(BorderFactory.createTitledBorder("Mapped new tests"));
        mappedNewTestsModel = new DefaultListModel<>();
        mappedNewTestsList = new JList<>(mappedNewTestsModel);
        mappedNewTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mappedPanel.add(new JScrollPane(mappedNewTestsList), BorderLayout.CENTER);
        JButton unlinkMapped = new JButton("Remove mapped");
        unlinkMapped.addActionListener(e -> unlinkMappedNewTests());
        mappedPanel.add(unlinkMapped, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout(4, 4));
        wrap.add(panel, BorderLayout.CENTER);
        wrap.add(mappedPanel, BorderLayout.SOUTH);
        return wrap;
    }

    private void refreshTestOptionsForRow(RequirementCoverage row) {
        existingOptionToIri.clear();
        iriToExistingOption.clear();
        newOptionToLabel.clear();
        existingTestsModel.clear();
        Set<String> rowTerms = termsForRow(row);
        for (String iri : state.getSelectedExistingTestIris()) {
            TestCatalogEntry entry = findCatalogEntry(iri);
            if (entry == null) {
                continue;
            }
            Set<String> testTerms = InformationElementTermService.extractQualifiedTerms(
                    entry.getLabel(), entry.getPrefLabel());
            if (!rowTerms.isEmpty() && !InformationElementTermService.matchesAnySelectedTerm(testTerms, rowTerms)) {
                continue;
            }
            String option = displayNameForEntry(entry);
            if (existingOptionToIri.containsKey(option)) {
                option = option + " <" + iri + ">";
            }
            existingTestsModel.addElement(option);
            existingOptionToIri.put(option, iri);
            iriToExistingOption.put(iri, option);
        }
        newTestsModel.clear();
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
            newTestsModel.addElement(option);
            newOptionToLabel.put(option, label);
        }
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
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshMappedLists(row);
        refreshCoverageSummary();
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
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshMappedLists(row);
        refreshCoverageSummary();
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
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshMappedLists(row);
        refreshCoverageSummary();
    }

    private void unlinkMappedNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedNewTests().removeAll(mappedNewTestsList.getSelectedValuesList());
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshMappedLists(row);
        refreshCoverageSummary();
    }

    private void saveDetails(RequirementCoverage row) {
        row.setPartialCoverageRationale(rationaleArea.getText().trim());
        row.setNotes(notesArea.getText().trim());
    }

    private void refreshCoverageSummary() {
        List<RequirementCoverage> rows = tableModel.getRows();
        int covered = gapAnalysisService.countCovered(rows);
        coverageSummaryLabel.setText("Coverage: " + covered + "/" + rows.size() + " covered");
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

    private Set<String> termsForRow(RequirementCoverage row) {
        Set<String> terms = new HashSet<>();
        if (row == null || row.getInformationElements() == null) {
            return terms;
        }
        String[] tokens = row.getInformationElements().split(",");
        for (String token : tokens) {
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

    private String formatExistingTestLabels(List<String> iris) {
        List<String> labels = new ArrayList<>();
        for (String iri : iris) {
            String option = iriToExistingOption.get(iri);
            if (option != null) {
                labels.add(option);
                continue;
            }
            TestCatalogEntry entry = findCatalogEntry(iri);
            labels.add(entry != null ? displayNameForEntry(entry) : iri);
        }
        return String.join("; ", labels);
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

    private final class CoverageTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {
                "Requirement ID/summary",
                "Information Element(s)",
                "Existing Tests mapped",
                "New Tests drafted",
                "Coverage status",
                "Notes/rationale"
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
                    return formatExistingTestLabels(row.getLinkedExistingTests());
                case 3:
                    return String.join("; ", row.getLinkedNewTests());
                case 4:
                    return row.computeStatus().getDisplayName();
                case 5:
                    return nvl(row.getPartialCoverageRationale());
                default:
                    return "";
            }
        }

        private String nvl(String value) {
            return value != null ? value : "";
        }
    }
}
