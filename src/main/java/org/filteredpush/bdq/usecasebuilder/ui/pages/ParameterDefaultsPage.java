package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page for test parameter/default authoring.
 */
public class ParameterDefaultsPage extends WizardPage {

    private final DefaultListModel<TestDraft> listModel = new DefaultListModel<>();
    private JList<TestDraft> draftList;
    private JTextArea parameterDefaultsArea;

    public ParameterDefaultsPage(ProjectState state) {
        super(state);
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return "Parameters & Defaults";
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
            parameterDefaultsArea.setText("");
        }
    }

    @Override
    public void onLeave() {
        saveCurrentDraft();
    }

    @Override
    public List<String> validatePage() {
        saveCurrentDraft();
        return new ArrayList<>();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Define parameters/defaults separately from expected response clauses.</b><br>"
                        + "What: parameter names, accepted ranges, and default values used by each new test.<br>"
                        + "Why: this captures test inputs independently from response.result behavior.<br>"
                        + "Convention: one parameter per line (e.g. <tt>countryCode=US</tt>).</html>");
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

        parameterDefaultsArea = new JTextArea(18, 48);
        parameterDefaultsArea.setLineWrap(true);
        parameterDefaultsArea.setWrapStyleWord(true);
        parameterDefaultsArea.setBorder(BorderFactory.createTitledBorder("Parameters/defaults"));
        content.add(new JScrollPane(parameterDefaultsArea), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private void loadDraft(TestDraft draft) {
        if (draft == null) {
            parameterDefaultsArea.setText("");
            return;
        }
        parameterDefaultsArea.setText(draft.getParameterDefaults() != null
                ? draft.getParameterDefaults() : "");
    }

    private void saveCurrentDraft() {
        TestDraft selected = draftList.getSelectedValue();
        if (selected != null) {
            selected.setParameterDefaults(parameterDefaultsArea.getText().trim());
            int idx = draftList.getSelectedIndex();
            if (idx >= 0) {
                listModel.set(idx, selected);
            }
        }
    }
}
