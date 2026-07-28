package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.service.VocabularyService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wizard page 3 – Identify information elements.
 *
 * <p>Presents a table in which the user can add, edit, and remove Darwin Core
 * (or other vocabulary) information elements and assign each the role
 * {@code ActedUpon} or {@code Consulted}.</p>
 *
 * <p>The term combo box provides live autocomplete: as the user types, the
 * dropdown is filtered to matching terms. A "Browse…" button opens a
 * vocabulary tree dialog grouped by namespace prefix (dwc:, ac:, etc.).</p>
 */
public class InformationElementsPage extends WizardPage {

    private IETableModel tableModel;
    private JTable table;
    private JComboBox<String> termCombo;
    private JButton addButton;
    private final ValidationService validationService = new ValidationService();
    private final VocabularyService vocabularyService;
    /** Full sorted list of terms, used for autocomplete filtering. */
    private List<String> allTerms = new ArrayList<>();
    private boolean updatingCombo = false;
    private static final int MAX_AUTOCOMPLETE_RESULTS = 50;

    /**
     * Creates the information elements page.
     *
     * @param state              shared project state
     * @param vocabularyService  service providing available IE vocabulary terms
     */
    public InformationElementsPage(ProjectState state, VocabularyService vocabularyService) {
        super(state);
        this.vocabularyService = vocabularyService;
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
        refreshTermPicklist();
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
                        + "What: terms your tests inspect or use as context.<br>"
                        + "Why: these terms anchor scope and traceability for test definitions.<br>"
                        + "Convention: use qualified names (e.g. <tt>dwc:scientificName</tt>, "
                        + "<tt>ac:accessURI</tt>) and assign each as <b>ActedUpon</b> or "
                        + "<b>Consulted</b>. Type to autocomplete or click <b>Browse…</b> "
                        + "to explore vocabularies by namespace.</html>");
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

        // Buttons row
        JPanel buttons = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        termCombo = new JComboBox<>();
        termCombo.setEditable(true);
        termCombo.setPrototypeDisplayValue("dwc:coordinateUncertaintyInMeters");
        termCombo.setToolTipText("Type to autocomplete; pick from Darwin Core, Audiovisual Core, or custom vocabularies");
        gc.weightx = 1.0;
        gc.gridx = 1;
        gc.gridy = 0;

        buttons.add(new JLabel("Term:"), buildLabelConstraint());
        buttons.add(termCombo, gc);

        addButton = new JButton("Add");
        addButton.setEnabled(false); // disabled until a term is entered
        addButton.addActionListener(e -> addSelectedTerm());
        gc.weightx = 0;
        gc.gridx = 2;
        buttons.add(addButton, gc);

        JButton browseButton = new JButton("Browse…");
        browseButton.setToolTipText("Open vocabulary browser grouped by namespace");
        browseButton.addActionListener(e -> openVocabularyBrowser());
        gc.gridx = 3;
        buttons.add(browseButton, gc);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeRow());
        gc.gridx = 4;
        buttons.add(removeButton, gc);

        add(buttons, BorderLayout.SOUTH);

        // Wire autocomplete to the combo editor
        wireAutocomplete();
    }

    private GridBagConstraints buildLabelConstraint() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 4);
        return c;
    }

    /** Wires a DocumentListener to the combo editor that filters the popup list. */
    private void wireAutocomplete() {
        JTextField editor = (JTextField) termCombo.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterCombo(); updateAddButtonState(); }
            @Override public void removeUpdate(DocumentEvent e) { filterCombo(); updateAddButtonState(); }
            @Override public void changedUpdate(DocumentEvent e) { filterCombo(); updateAddButtonState(); }
        });
    }

    /** Enables the Add button only when the term combo editor has a non-empty value. */
    private void updateAddButtonState() {
        if (addButton == null || termCombo == null) {
            return;
        }
        Object item = termCombo.getEditor().getItem();
        addButton.setEnabled(item != null && !item.toString().trim().isEmpty());
    }

    private void filterCombo() {
        if (updatingCombo) {
            return;
        }
        JTextField editor = (JTextField) termCombo.getEditor().getEditorComponent();
        String typed = editor.getText();
        String lower = typed.toLowerCase(Locale.ROOT);

        // Build filtered list
        List<String> filtered = new ArrayList<>();
        for (String term : allTerms) {
            if (term.toLowerCase(Locale.ROOT).contains(lower)) {
                filtered.add(term);
                if (filtered.size() >= MAX_AUTOCOMPLETE_RESULTS) {
                    break;
                }
            }
        }

        updatingCombo = true;
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(filtered.toArray(new String[0]));
        termCombo.setModel(model);
        termCombo.setSelectedItem(typed);
        // Restore caret to end
        SwingUtilities.invokeLater(() -> {
            JTextField ed = (JTextField) termCombo.getEditor().getEditorComponent();
            ed.setText(typed);
            ed.setCaretPosition(typed.length());
        });
        updatingCombo = false;

        // Show popup if there are matches and user is typing
        if (!filtered.isEmpty() && !typed.isEmpty()) {
            termCombo.setPopupVisible(true);
        }
    }

    private void addSelectedTerm() {
        Object selected = termCombo.getEditor().getItem();
        if (selected == null) {
            return;
        }
        String qname = selected.toString().trim();
        if (!qname.isEmpty() && !tableModel.hasTerm(qname)) {
            tableModel.addRow(new InformationElementRef(qname, InfoElementRole.ACTED_UPON));
        }
        // Clear editor after add
        termCombo.getEditor().setItem("");
    }

    private void refreshTermPicklist() {
        allTerms = new ArrayList<>(vocabularyService.getInformationElementTerms());
        updatingCombo = true;
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(allTerms.toArray(new String[0]));
        termCombo.setModel(model);
        termCombo.setSelectedItem("");
        updatingCombo = false;
    }

    private void removeRow() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            tableModel.removeRow(row);
        }
    }

    // -----------------------------------------------------------------------
    // Vocabulary browser dialog
    // -----------------------------------------------------------------------

    private void openVocabularyBrowser() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Vocabulary Browser", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));

        // Build tree grouped by vocabulary prefix
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Vocabularies");
        Map<String, List<String>> allVocabs = vocabularyService.getAllVocabularies();
        for (Map.Entry<String, List<String>> entry : allVocabs.entrySet()) {
            String vocabId = entry.getKey();
            List<String> terms = entry.getValue();
            // Only show information-element vocabularies (not bdq* ontology terms)
            if (vocabId.startsWith("bdq") || terms.isEmpty()) {
                continue;
            }
            DefaultMutableTreeNode vocabNode = new DefaultMutableTreeNode(vocabId);
            for (String term : terms) {
                vocabNode.add(new DefaultMutableTreeNode(term));
            }
            root.add(vocabNode);
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // Filter field
        JTextField filterField = new JTextField();
        filterField.setToolTipText("Filter terms");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTree(tree, root, filterField.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filterTree(tree, root, filterField.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterTree(tree, root, filterField.getText()); }
        });

        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.add(new JLabel("Filter: "), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        dialog.add(filterPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(tree), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel();
        JButton selectButton = new JButton("Add selected");
        selectButton.addActionListener(e -> {
            for (int row : tree.getSelectionRows()) {
                Object node = tree.getPathForRow(row).getLastPathComponent();
                if (node instanceof DefaultMutableTreeNode) {
                    Object val = ((DefaultMutableTreeNode) node).getUserObject();
                    if (val != null) {
                        String qname = val.toString().trim();
                        if (qname.contains(":") && !tableModel.hasTerm(qname)) {
                            tableModel.addRow(new InformationElementRef(qname, InfoElementRole.ACTED_UPON));
                        }
                    }
                }
            }
            // Dialog stays open – user closes it explicitly
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        btnPanel.add(selectButton);
        btnPanel.add(closeButton);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void filterTree(JTree tree, DefaultMutableTreeNode root, String query) {
        if (query == null || query.trim().isEmpty()) {
            tree.setModel(new DefaultTreeModel(root));
            return;
        }
        String lower = query.trim().toLowerCase(Locale.ROOT);
        DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode("Vocabularies");
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode vocabNode = (DefaultMutableTreeNode) root.getChildAt(i);
            DefaultMutableTreeNode filteredVocab = new DefaultMutableTreeNode(vocabNode.getUserObject());
            for (int j = 0; j < vocabNode.getChildCount(); j++) {
                DefaultMutableTreeNode termNode = (DefaultMutableTreeNode) vocabNode.getChildAt(j);
                String term = termNode.getUserObject().toString().toLowerCase(Locale.ROOT);
                if (term.contains(lower)) {
                    filteredVocab.add(new DefaultMutableTreeNode(termNode.getUserObject()));
                }
            }
            if (filteredVocab.getChildCount() > 0) {
                filteredRoot.add(filteredVocab);
            }
        }
        tree.setModel(new DefaultTreeModel(filteredRoot));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
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

        boolean hasTerm(String qname) {
            for (InformationElementRef row : rows) {
                if (row.getQname() != null && row.getQname().equalsIgnoreCase(qname)) {
                    return true;
                }
            }
            return false;
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
