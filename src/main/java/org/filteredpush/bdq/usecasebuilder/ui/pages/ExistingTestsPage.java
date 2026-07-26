package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wizard page 4 – Select existing BDQ tests.
 *
 * <p>Displays the local BDQ test catalog in a table with a search/filter text
 * field. The user selects tests by checking the checkbox in the first column.
 * Selections are synchronised with the project state on {@link #onLeave()}.</p>
 */
public class ExistingTestsPage extends WizardPage {

    private final TestCatalogService catalogService;
    private ExistingTestsTableModel tableModel;
    private TableRowSorter<ExistingTestsTableModel> sorter;
    private JTextField searchField;

    /**
     * Creates the existing tests selection page.
     *
     * @param state          shared project state
     * @param catalogService the catalog service (already loaded)
     */
    public ExistingTestsPage(ProjectState state, TestCatalogService catalogService) {
        super(state);
        this.catalogService = catalogService;
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Select Existing Tests";
    }

    @Override
    public void onEnter() {
        Set<String> selected = new HashSet<>(state.getSelectedExistingTestIris());
        tableModel.setSelected(selected);
    }

    @Override
    public void onLeave() {
        state.setSelectedExistingTestIris(tableModel.getSelectedIris());
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        // No hard requirement on the existing tests page
        return new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Select existing BDQ tests</b> to include in your use case.<br>"
                        + "Use the search box to filter by label, type, or dimension. "
                        + "Check the box next to each test you want to include.</html>");
        add(intro, BorderLayout.NORTH);

        // Table
        tableModel = new ExistingTestsTableModel(catalogService.getEntries());
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Column widths
        table.getColumnModel().getColumn(0).setMaxWidth(50);    // checkbox
        table.getColumnModel().getColumn(1).setPreferredWidth(220); // label
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // type
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // dimension
        table.getColumnModel().getColumn(4).setPreferredWidth(280); // prefLabel

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.add(new JLabel("Filter:"));
        searchField = new JTextField(30);
        searchField.setToolTipText("Type to filter tests by label, type, or dimension");
        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
                    @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
                    @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
                });
        searchPanel.add(searchField);

        JLabel countLabel = new JLabel(
                catalogService.getEntries().size() + " tests in catalog");
        searchPanel.add(countLabel);
        add(searchPanel, BorderLayout.SOUTH);
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1, 2, 3, 4));
        }
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private static final class ExistingTestsTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
                {"Select", "Label", "Type", "Dimension", "Preferred Label"};

        private final List<TestCatalogEntry> entries;
        private final boolean[] selected;

        ExistingTestsTableModel(List<TestCatalogEntry> entries) {
            this.entries = new ArrayList<>(entries);
            this.selected = new boolean[entries.size()];
        }

        void setSelected(Set<String> iris) {
            for (int i = 0; i < entries.size(); i++) {
                selected[i] = iris.contains(entries.get(i).getIri());
            }
            fireTableDataChanged();
        }

        List<String> getSelectedIris() {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                if (selected[i]) {
                    result.add(entries.get(i).getIri());
                }
            }
            return result;
        }

        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            TestCatalogEntry e = entries.get(row);
            switch (col) {
                case 0: return selected[row];
                case 1: return e.getLabel();
                case 2: return e.getType();
                case 3: return e.getDimension();
                case 4: return e.getPrefLabel();
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                selected[row] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
