package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.GapAnalysisService;
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
import java.util.List;

/**
 * Wizard page for requirement-to-test gap analysis.
 */
public class GapAnalysisPage extends WizardPage {

    private final GapAnalysisService gapAnalysisService = new GapAnalysisService();
    private CoverageTableModel tableModel;
    private JTable matrixTable;
    private DefaultListModel<String> existingTestsModel;
    private DefaultListModel<String> newTestsModel;
    private JList<String> existingTestsList;
    private JList<String> newTestsList;
    private JTextArea rationaleArea;
    private JTextArea notesArea;
    private JLabel coverageSummaryLabel;

    public GapAnalysisPage(ProjectState state) {
        super(state);
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
        refreshTestLists();
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
        panel.setBorder(BorderFactory.createTitledBorder("Link existing tests"));
        existingTestsModel = new DefaultListModel<>();
        existingTestsList = new JList<>(existingTestsModel);
        existingTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(existingTestsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton link = new JButton("Link selected");
        link.addActionListener(e -> linkSelectedExistingTests());
        JButton unlink = new JButton("Unlink selected");
        unlink.addActionListener(e -> unlinkSelectedExistingTests());
        buttons.add(link);
        buttons.add(unlink);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildNewTestMappingPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Link new tests"));
        newTestsModel = new DefaultListModel<>();
        newTestsList = new JList<>(newTestsModel);
        newTestsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(newTestsList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton link = new JButton("Link selected");
        link.addActionListener(e -> linkSelectedNewTests());
        JButton unlink = new JButton("Unlink selected");
        unlink.addActionListener(e -> unlinkSelectedNewTests());
        buttons.add(link);
        buttons.add(unlink);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshTestLists() {
        existingTestsModel.clear();
        for (String iri : state.getSelectedExistingTestIris()) {
            existingTestsModel.addElement(iri);
        }
        newTestsModel.clear();
        for (TestDraft draft : state.getNewTestDrafts()) {
            if (draft.getLabel() != null && !draft.getLabel().trim().isEmpty()) {
                newTestsModel.addElement(draft.getLabel().trim());
            } else {
                newTestsModel.addElement(draft.toString());
            }
        }
    }

    private void loadSelectedRowDetails() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            rationaleArea.setText("");
            notesArea.setText("");
            return;
        }
        rationaleArea.setText(row.getPartialCoverageRationale() != null
                ? row.getPartialCoverageRationale() : "");
        notesArea.setText(row.getNotes() != null ? row.getNotes() : "");
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
            if (!row.getLinkedExistingTests().contains(selected)) {
                row.getLinkedExistingTests().add(selected);
            }
        }
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshCoverageSummary();
    }

    private void unlinkSelectedExistingTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedExistingTests().removeAll(existingTestsList.getSelectedValuesList());
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshCoverageSummary();
    }

    private void linkSelectedNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        for (String selected : newTestsList.getSelectedValuesList()) {
            if (!row.getLinkedNewTests().contains(selected)) {
                row.getLinkedNewTests().add(selected);
            }
        }
        saveDetails(row);
        tableModel.fireTableDataChanged();
        refreshCoverageSummary();
    }

    private void unlinkSelectedNewTests() {
        RequirementCoverage row = getSelectedRow();
        if (row == null) {
            return;
        }
        row.getLinkedNewTests().removeAll(newTestsList.getSelectedValuesList());
        saveDetails(row);
        tableModel.fireTableDataChanged();
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

    private static final class CoverageTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
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
                    return String.join("; ", row.getLinkedExistingTests());
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
