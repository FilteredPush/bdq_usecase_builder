package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.AuthorityDefault;
import org.filteredpush.bdq.usecasebuilder.model.AuthorityPatternType;
import org.filteredpush.bdq.usecasebuilder.model.ParameterDefinition;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page for structured authority/default and parameter authoring.
 */
public class ParameterDefaultsPage extends WizardPage {

    private final DefaultListModel<TestDraft> listModel = new DefaultListModel<>();
    private final ValidationService validationService = new ValidationService();

    private JList<TestDraft> draftList;
    private AuthorityTableModel authorityTableModel;
    private ParameterTableModel parameterTableModel;
    private TestDraft currentDraft;

    public ParameterDefaultsPage(ProjectState state) {
        super(state);
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return "Authorities & Parameters";
    }

    @Override
    public void onEnter() {
        listModel.clear();
        for (TestDraft draft : state.getNewTestDrafts()) {
            listModel.addElement(draft);
        }
        if (!listModel.isEmpty()) {
            draftList.setSelectedIndex(0);
            loadDraft(listModel.get(0));
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
        saveCurrentDraft();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            errors.addAll(validationService.validateAuthorityAndParameters(listModel.get(i)));
        }
        return errors;
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Define source authority defaults and parameters.</b><br>"
                        + "What: structured authority conventions and parameter definitions per test.<br>"
                        + "Why: authority/parameter modeling drives reproducible execution behavior.<br>"
                        + "Convention: choose URI-only, URI+API, or regex-based authorities; "
                        + "parameter names must be unique per test.</html>");
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        add(intro, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(8, 0));
        draftList = new JList<>(listModel);
        draftList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        draftList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDraft(draftList.getSelectedValue());
            }
        });
        content.add(new JScrollPane(draftList), BorderLayout.WEST);

        JPanel editors = new JPanel(new GridLayout(2, 1, 0, 8));
        editors.add(buildAuthorityPanel());
        editors.add(buildParameterPanel());
        content.add(editors, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildAuthorityPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Authorities defaults"));
        authorityTableModel = new AuthorityTableModel();
        JTable table = new JTable(authorityTableModel);
        JComboBox<AuthorityPatternType> patternTypeCombo = new JComboBox<>(AuthorityPatternType.values());
        table.getColumnModel().getColumn(1).setCellEditor(new javax.swing.DefaultCellEditor(patternTypeCombo));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Add authority");
        add.addActionListener(e -> authorityTableModel.addRow(new AuthorityDefault()));
        JButton remove = new JButton("Remove selected authority");
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                authorityTableModel.removeRow(row);
            }
        });
        buttons.add(add);
        buttons.add(remove);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildParameterPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        parameterTableModel = new ParameterTableModel();
        JTable table = new JTable(parameterTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Add parameter");
        add.addActionListener(e -> parameterTableModel.addRow(new ParameterDefinition()));
        JButton remove = new JButton("Remove selected parameter");
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                parameterTableModel.removeRow(row);
            }
        });
        buttons.add(add);
        buttons.add(remove);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void loadDraft(TestDraft draft) {
        saveCurrentDraft();
        currentDraft = draft;
        if (draft == null) {
            authorityTableModel.load(new ArrayList<>());
            parameterTableModel.load(new ArrayList<>());
            return;
        }
        authorityTableModel.load(new ArrayList<>(draft.getAuthorityDefaults()));
        parameterTableModel.load(new ArrayList<>(draft.getParameterDefinitions()));
    }

    private void saveCurrentDraft() {
        if (currentDraft == null) {
            return;
        }
        currentDraft.setAuthorityDefaults(authorityTableModel.getRows());
        currentDraft.setParameterDefinitions(parameterTableModel.getRows());
        currentDraft.setParameterDefaults(buildParameterDefaultsText(currentDraft));
        List<String> errors = validationService.validateAuthorityAndParameters(currentDraft);
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    String.join("\n", errors),
                    "Authority/parameter validation",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private String buildParameterDefaultsText(TestDraft draft) {
        StringBuilder sb = new StringBuilder();
        for (AuthorityDefault authority : draft.getAuthorityDefaults()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("authority[").append(nvl(authority.getIdentifier())).append("] ");
            sb.append("type=").append(authority.getPatternType() != null
                    ? authority.getPatternType().name() : "");
            if (authority.getAuthorityUri() != null && !authority.getAuthorityUri().trim().isEmpty()) {
                sb.append(" uri=").append(authority.getAuthorityUri().trim());
            }
            if (authority.getApiLabel() != null && !authority.getApiLabel().trim().isEmpty()) {
                sb.append(" apiLabel=").append(authority.getApiLabel().trim());
            }
            if (authority.getApiEndpoint() != null && !authority.getApiEndpoint().trim().isEmpty()) {
                sb.append(" apiEndpoint=").append(authority.getApiEndpoint().trim());
            }
            if (authority.getRegexPattern() != null && !authority.getRegexPattern().trim().isEmpty()) {
                sb.append(" regex=").append(authority.getRegexPattern().trim());
            }
        }
        for (ParameterDefinition parameter : draft.getParameterDefinitions()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("parameter[").append(nvl(parameter.getName())).append("] ");
            if (parameter.getDatatype() != null && !parameter.getDatatype().trim().isEmpty()) {
                sb.append("datatype=").append(parameter.getDatatype().trim()).append(" ");
            }
            if (parameter.getDefaultAuthorityIdentifier() != null
                    && !parameter.getDefaultAuthorityIdentifier().trim().isEmpty()) {
                sb.append("defaultAuthority=").append(parameter.getDefaultAuthorityIdentifier().trim()).append(" ");
            }
            if (parameter.getNotes() != null && !parameter.getNotes().trim().isEmpty()) {
                sb.append("notes=").append(parameter.getNotes().trim());
            }
        }
        return sb.toString().trim();
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private static final class AuthorityTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Identifier", "Pattern Type", "Authority URI", "API Label", "API Endpoint", "Regex Pattern"
        };
        private final List<AuthorityDefault> rows = new ArrayList<>();

        void load(List<AuthorityDefault> data) {
            rows.clear();
            rows.addAll(data);
            fireTableDataChanged();
        }

        void addRow(AuthorityDefault authority) {
            rows.add(authority);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void removeRow(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        List<AuthorityDefault> getRows() {
            return new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 1 ? AuthorityPatternType.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            AuthorityDefault authority = rows.get(row);
            switch (col) {
                case 0:
                    return authority.getIdentifier();
                case 1:
                    return authority.getPatternType();
                case 2:
                    return authority.getAuthorityUri();
                case 3:
                    return authority.getApiLabel();
                case 4:
                    return authority.getApiEndpoint();
                case 5:
                    return authority.getRegexPattern();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            AuthorityDefault authority = rows.get(row);
            switch (col) {
                case 0:
                    authority.setIdentifier(value != null ? value.toString() : null);
                    break;
                case 1:
                    authority.setPatternType((AuthorityPatternType) value);
                    break;
                case 2:
                    authority.setAuthorityUri(value != null ? value.toString() : null);
                    break;
                case 3:
                    authority.setApiLabel(value != null ? value.toString() : null);
                    break;
                case 4:
                    authority.setApiEndpoint(value != null ? value.toString() : null);
                    break;
                case 5:
                    authority.setRegexPattern(value != null ? value.toString() : null);
                    break;
                default:
                    break;
            }
            fireTableCellUpdated(row, col);
        }
    }

    private static final class ParameterTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Name", "Datatype", "Default authority", "Notes"};
        private final List<ParameterDefinition> rows = new ArrayList<>();

        void load(List<ParameterDefinition> data) {
            rows.clear();
            rows.addAll(data);
            fireTableDataChanged();
        }

        void addRow(ParameterDefinition row) {
            rows.add(row);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void removeRow(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        List<ParameterDefinition> getRows() {
            return new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public Object getValueAt(int row, int col) {
            ParameterDefinition parameter = rows.get(row);
            switch (col) {
                case 0:
                    return parameter.getName();
                case 1:
                    return parameter.getDatatype();
                case 2:
                    return parameter.getDefaultAuthorityIdentifier();
                case 3:
                    return parameter.getNotes();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            ParameterDefinition parameter = rows.get(row);
            switch (col) {
                case 0:
                    parameter.setName(value != null ? value.toString() : null);
                    break;
                case 1:
                    parameter.setDatatype(value != null ? value.toString() : null);
                    break;
                case 2:
                    parameter.setDefaultAuthorityIdentifier(value != null ? value.toString() : null);
                    break;
                case 3:
                    parameter.setNotes(value != null ? value.toString() : null);
                    break;
                default:
                    break;
            }
            fireTableCellUpdated(row, col);
        }
    }
}
