package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page 3 – Identify information elements.
 *
 * <p>Presents a table in which the user can add, edit, and remove Darwin Core
 * (or other vocabulary) information elements and assign each the role
 * {@code ActedUpon} or {@code Consulted}.</p>
 */
public class InformationElementsPage extends WizardPage {

    private IETableModel tableModel;
    private JTable table;
    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the information elements page.
     *
     * @param state shared project state
     */
    public InformationElementsPage(ProjectState state) {
        super(state);
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Information Elements";
    }

    @Override
    public void onEnter() {
        tableModel.load(new ArrayList<>(state.getInformationElements()));
    }

    @Override
    public void onLeave() {
        state.clearInformationElements();
        for (InformationElementRef ref : tableModel.getRows()) {
            state.addInformationElement(ref);
        }
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return validationService.validateInformationElementsPage(state);
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Identify the information elements</b> your use case depends on.<br>"
                        + "For each term (e.g. <tt>dwc:scientificName</tt>), specify whether tests in "
                        + "your use case act upon it or consult it for context.</html>");
        add(intro, BorderLayout.NORTH);

        tableModel = new IETableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);

        // Role column uses a combo box editor
        JComboBox<InfoElementRole> roleCombo = new JComboBox<>(InfoElementRole.values());
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(roleCombo));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");

        addButton.addActionListener(e -> addRow());
        removeButton.addActionListener(e -> removeRow());

        buttons.add(addButton);
        buttons.add(removeButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addRow() {
        String qname = JOptionPane.showInputDialog(this,
                "Enter the qualified name of the term\n(e.g. dwc:scientificName):",
                "Add information element",
                JOptionPane.PLAIN_MESSAGE);
        if (qname == null || qname.trim().isEmpty()) {
            return;
        }
        tableModel.addRow(new InformationElementRef(qname.trim(), InfoElementRole.ACTED_UPON));
    }

    private void removeRow() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            tableModel.removeRow(row);
        }
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private static final class IETableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {"Term (qualified name)", "Role"};
        private final List<InformationElementRef> rows = new ArrayList<>();

        void load(List<InformationElementRef> data) {
            rows.clear();
            rows.addAll(data);
            fireTableDataChanged();
        }

        List<InformationElementRef> getRows() {
            return rows;
        }

        void addRow(InformationElementRef ref) {
            rows.add(ref);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void removeRow(int index) {
            rows.remove(index);
            fireTableRowsDeleted(index, index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 1 ? InfoElementRole.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public Object getValueAt(int row, int col) {
            InformationElementRef ref = rows.get(row);
            return col == 0 ? ref.getQname() : ref.getRole();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            InformationElementRef ref = rows.get(row);
            if (col == 0) {
                ref.setQname((String) value);
            } else {
                ref.setRole((InfoElementRole) value);
            }
            fireTableCellUpdated(row, col);
        }
    }
}
