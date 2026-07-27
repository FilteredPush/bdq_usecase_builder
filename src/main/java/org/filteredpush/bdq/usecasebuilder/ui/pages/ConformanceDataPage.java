package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.ConformanceCsvService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wizard page for conformance CSV row authoring.
 */
public class ConformanceDataPage extends WizardPage {

    private final ConformanceCsvService conformanceCsvService = new ConformanceCsvService();
    private final DefaultListModel<TestDraft> testListModel = new DefaultListModel<>();
    private JList<TestDraft> testList;
    private ConformanceTableModel tableModel;
    private TestDraft currentDraft;

    public ConformanceDataPage(ProjectState state) {
        super(state);
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return "Conformance CSV Data";
    }

    @Override
    public void onEnter() {
        testListModel.clear();
        for (TestDraft draft : state.getNewTestDrafts()) {
            testListModel.addElement(draft);
        }
        if (!testListModel.isEmpty()) {
            testList.setSelectedIndex(0);
            loadDraft(testListModel.get(0));
        } else {
            loadDraft(null);
        }
    }

    @Override
    public void onLeave() {
        saveCurrentDraft();
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return new ArrayList<>();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Build conformance CSV starter rows.</b><br>"
                        + "Rows are generated from expected-response clauses and can be edited/add/remove before export.<br>"
                        + "Export writes per-test CSV files and a combined CSV artifact.</html>");
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(intro, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(8, 0));
        testList = new JList<>(testListModel);
        testList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDraft(testList.getSelectedValue());
            }
        });
        content.add(new JScrollPane(testList), BorderLayout.WEST);

        tableModel = new ConformanceTableModel();
        JTable table = new JTable(tableModel);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton regenerate = new JButton("Regenerate from clauses");
        regenerate.addActionListener(e -> regenerateRowsFromClauses());
        JButton addRow = new JButton("Add edge-case row");
        addRow.addActionListener(e -> tableModel.addRow(buildBlankRow()));
        JButton removeRow = new JButton("Remove selected row");
        removeRow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRow(row);
            }
        });
        buttons.add(regenerate);
        buttons.add(addRow);
        buttons.add(removeRow);
        add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private void loadDraft(TestDraft draft) {
        saveCurrentDraft();
        currentDraft = draft;
        if (draft == null) {
            tableModel.load(new ArrayList<>(), List.of("Label", "Response.status", "Response.result", "Response.comment"));
            return;
        }
        if (draft.getConformanceRows().isEmpty()) {
            draft.setConformanceRows(conformanceCsvService.generateStarterRows(draft));
        }
        tableModel.load(new ArrayList<>(draft.getConformanceRows()),
                conformanceCsvService.buildColumns(state, draft));
    }

    private void saveCurrentDraft() {
        if (currentDraft == null) {
            return;
        }
        currentDraft.setConformanceRows(tableModel.getRows());
    }

    private void regenerateRowsFromClauses() {
        if (currentDraft == null) {
            return;
        }
        List<ConformanceRow> rows = conformanceCsvService.generateStarterRows(currentDraft);
        currentDraft.setConformanceRows(rows);
        tableModel.load(new ArrayList<>(rows),
                conformanceCsvService.buildColumns(state, currentDraft));
    }

    private ConformanceRow buildBlankRow() {
        ConformanceRow row = new ConformanceRow();
        for (String column : tableModel.getColumns()) {
            row.put(column, "");
        }
        row.put("Label", "Edge case");
        return row;
    }

    private static final class ConformanceTableModel extends AbstractTableModel {
        private final List<ConformanceRow> rows = new ArrayList<>();
        private final List<String> columns = new ArrayList<>();

        void load(List<ConformanceRow> data, List<String> columnNames) {
            rows.clear();
            rows.addAll(data);
            columns.clear();
            columns.addAll(columnNames);
            for (ConformanceRow row : rows) {
                for (String column : columns) {
                    row.getValues().putIfAbsent(column, "");
                }
            }
            fireTableStructureChanged();
        }

        void addRow(ConformanceRow row) {
            for (String column : columns) {
                row.getValues().putIfAbsent(column, "");
            }
            rows.add(row);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void removeRow(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        List<ConformanceRow> getRows() {
            return new ArrayList<>(rows);
        }

        List<String> getColumns() {
            return columns;
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.size(); }
        @Override public String getColumnName(int col) { return columns.get(col); }
        @Override public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ConformanceRow row = rows.get(rowIndex);
            String key = columns.get(columnIndex);
            return row.getValues().getOrDefault(key, "");
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ConformanceRow row = rows.get(rowIndex);
            String key = columns.get(columnIndex);
            row.put(key, value != null ? value.toString() : "");
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
