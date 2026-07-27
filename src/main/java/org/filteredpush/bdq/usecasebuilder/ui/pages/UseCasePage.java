package org.filteredpush.bdq.usecasebuilder.ui.pages;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.filteredpush.bdq.usecasebuilder.service.ValidationService;
import org.filteredpush.bdq.usecasebuilder.ui.WizardPage;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Wizard page 2 – Define the use case.
 *
 * <p>Collects the use case name, a description, fitness-for-use requirements
 * narrative, and an optional scope note.  The fitness requirements are stored
 * as an introductory plain-text line followed by a {@code <ul><li>…</li></ul>}
 * block (no {@code <p>} tags) so that they round-trip correctly when exported
 * to RDF and then re-loaded.</p>
 */
public class UseCasePage extends WizardPage {

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextArea fitnessLeadArea;
    private JTextArea fitnessPropertiesArea;
    private JTextField scopeNoteField;
    private boolean updatingFitnessLeadTemplate;
    private boolean fitnessLeadUserEdited;

    private final ValidationService validationService = new ValidationService();

    /**
     * Creates the use case definition page.
     *
     * @param state shared project state
     */
    public UseCasePage(ProjectState state) {
        super(state);
        buildUi();
    }

    // -----------------------------------------------------------------------
    // WizardPage contract
    // -----------------------------------------------------------------------

    @Override
    public String getPageTitle() {
        return "Define Use Case";
    }

    @Override
    public void onEnter() {
        UseCaseDraft draft = state.getUseCaseDraft();
        nameField.setText(nvl(draft.getName()));
        descriptionArea.setText(nvl(draft.getDescription()));
        loadFitnessClauses(nvl(draft.getFitnessRequirementsText()));
        scopeNoteField.setText(nvl(draft.getScopeNote()));
        ensureFitnessTemplate();
    }

    @Override
    public void onLeave() {
        UseCaseDraft draft = state.getUseCaseDraft();
        draft.setName(nameField.getText().trim());
        draft.setDescription(descriptionArea.getText().trim());
        draft.setFitnessRequirementsText(buildFitnessRequirementsText());
        String sn = scopeNoteField.getText().trim();
        draft.setScopeNote(sn.isEmpty() ? null : sn);
    }

    @Override
    public List<String> validatePage() {
        onLeave();
        return validationService.validateUseCasePage(state.getUseCaseDraft());
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel intro = new JLabel(
                "<html><b>Define your use case.</b> "
                        + "What: the purpose and context for a set of data quality tests.<br>"
                        + "Why: this anchors which information elements and test assertions matter.<br>"
                        + "Convention: keep names concise, describe scope in prose, and express "
                        + "fitness requirements as practical, testable statements.</html>");
        add(intro, BorderLayout.NORTH);

        JLabel introLabel = new JLabel(
                "<html><small>Fields marked * are required.</small></html>");
        introLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        // Form
        JLabel nameLabel = new JLabel("Use case name *:");
        nameField = new JTextField(40);
        nameField.setToolTipText(
                "A short, unique name for the use case (e.g. Spatial quality for specimens)");

        JLabel descLabel = new JLabel("Description:");
        descriptionArea = new JTextArea(4, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setToolTipText("A paragraph describing what the use case is for");

        JLabel fitnessLabel = new JLabel("Fitness-for-use requirements:");
        fitnessLeadArea = new JTextArea(2, 40);
        fitnessLeadArea.setLineWrap(true);
        fitnessLeadArea.setWrapStyleWord(true);
        fitnessLeadArea.setToolTipText(
                "Clause 1 (descriptive): Data are fit for use for [use case name] if they...");
        fitnessPropertiesArea = new JTextArea(5, 40);
        fitnessPropertiesArea.setLineWrap(true);
        fitnessPropertiesArea.setWrapStyleWord(true);
        fitnessPropertiesArea.setToolTipText(
                "One property per line (a bulleted list will be generated in export text)");

        JLabel scopeNoteLabel = new JLabel("Scope note (optional):");
        scopeNoteField = new JTextField(40);
        scopeNoteField.setToolTipText(
                "An optional skos:scopeNote providing additional context for the use case");

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        JScrollPane fitnessLeadScroll = new JScrollPane(fitnessLeadArea);
        JScrollPane fitnessPropsScroll = new JScrollPane(fitnessPropertiesArea);

        // Use GridBag for the form
        JScrollPane formWrapper = buildForm(
                introLabel, nameLabel, nameField,
                descLabel, descScroll,
                fitnessLabel, fitnessLeadScroll, fitnessPropsScroll,
                scopeNoteLabel, scopeNoteField);
        add(formWrapper, BorderLayout.CENTER);

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { ensureFitnessTemplate(); }
        });
        fitnessLeadArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { markFitnessLeadEdited(); }
        });
    }

    private JScrollPane buildForm(JLabel introLabel,
                                   JLabel nameLabel, JTextField nameField,
                                   JLabel descLabel, JScrollPane descScroll,
                                   JLabel fitnessLabel, JScrollPane fitnessLeadScroll,
                                   JScrollPane fitnessPropsScroll,
                                   JLabel scopeNoteLabel, JTextField scopeNoteField) {
        // Use GridBagLayout
        java.awt.Container form = new javax.swing.JPanel(new GridBagLayout());
        form.setBackground(getBackground());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(6, 0, 4, 8);

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(6, 0, 4, 0);
        fc.gridwidth = GridBagConstraints.REMAINDER;

        // Intro
        fc.gridy = 0;
        form.add(introLabel, fc);

        // Name
        lc.gridy = 1;
        form.add(nameLabel, lc);
        fc.gridy = 1;
        fc.gridwidth = GridBagConstraints.REMAINDER;
        form.add(nameField, fc);

        // Description
        lc.gridy = 2;
        form.add(descLabel, lc);
        fc.gridy = 2;
        fc.fill = GridBagConstraints.BOTH;
        fc.weighty = 0.2;
        form.add(descScroll, fc);

        // Fitness
        lc.gridy = 3;
        form.add(fitnessLabel, lc);
        fc.gridy = 3;
        fc.weighty = 0.2;
        form.add(fitnessLeadScroll, fc);

        lc.gridy = 4;
        form.add(new JLabel("Specific properties (one per line):"), lc);
        fc.gridy = 4;
        fc.weighty = 0.5;
        form.add(fitnessPropsScroll, fc);

        // Scope note
        lc.gridy = 5;
        form.add(scopeNoteLabel, lc);
        fc.gridy = 5;
        fc.weighty = 0;
        fc.fill = GridBagConstraints.HORIZONTAL;
        form.add(scopeNoteField, fc);

        return new JScrollPane(form);
    }

    private void ensureFitnessTemplate() {
        String current = fitnessLeadArea.getText().trim();
        if (current.isEmpty() || !fitnessLeadUserEdited) {
            updatingFitnessLeadTemplate = true;
            fitnessLeadArea.setText(defaultFitnessLeadClause());
            updatingFitnessLeadTemplate = false;
        }
    }

    private String defaultFitnessLeadClause() {
        String name = nameField != null ? nameField.getText().trim() : "";
        String bracketedName = name.isEmpty() ? "[use case name]" : name;
        return "Data are fit for use for " + bracketedName + " if they...";
    }

    private void loadFitnessClauses(String text) {
        // Handle both HTML <ul><li> format (from RDF export) and plain-text "- " format
        if (text.contains("<ul>") || text.contains("<li>")) {
            loadFitnessClausesFromHtml(text);
        } else {
            loadFitnessClausesFromPlainText(text);
        }
    }

    private void loadFitnessClausesFromHtml(String html) {
        // Extract lead text: everything before <ul>.
        // Strip any legacy <p> wrapper tags, then unescape HTML entities, then
        // call stripHtmlTags to remove any tag-like content that may have been
        // reintroduced by unescaping (handles data that went through multiple
        // encode/decode cycles in earlier versions).  Both branches apply the
        // same stripHtmlTags(unescapeHtml(...)) pattern for consistency.
        String lead = "";
        String bullets = "";
        int ulStart = html.indexOf("<ul>");
        if (ulStart >= 0) {
            String beforeUl = html.substring(0, ulStart).replaceAll("</?p>", "").trim();
            lead = stripHtmlTags(unescapeHtml(beforeUl)).trim();
            // Extract <li> items
            StringBuilder sb = new StringBuilder();
            int pos = ulStart;
            while (true) {
                int liStart = html.indexOf("<li>", pos);
                if (liStart < 0) break;
                int liEnd = html.indexOf("</li>", liStart);
                if (liEnd < 0) break;
                if (sb.length() > 0) sb.append('\n');
                sb.append(unescapeHtml(html.substring(liStart + 4, liEnd).trim()));
                pos = liEnd + 5;
            }
            bullets = sb.toString();
        } else {
            // No <ul> present: treat entire content as plain lead text, stripping
            // any HTML tags that may have been embedded or reintroduced by unescaping.
            lead = stripHtmlTags(unescapeHtml(html)).trim();
        }
        fitnessLeadArea.setText(lead);
        fitnessPropertiesArea.setText(bullets);
        String leadTrimmed = lead.trim();
        fitnessLeadUserEdited = !leadTrimmed.isEmpty() && !isDefaultFitnessLeadTemplate(leadTrimmed);
    }

    /**
     * Strips HTML tags from a string using a character-level state machine to
     * avoid the limitations of regex-based tag stripping with malformed input.
     */
    private static String stripHtmlTags(String html) {
        StringBuilder out = new StringBuilder(html.length());
        boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char ch = html.charAt(i);
            if (ch == '<') {
                inTag = true;
            } else if (ch == '>') {
                inTag = false;
            } else if (!inTag) {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private void loadFitnessClausesFromPlainText(String text) {
        String[] lines = text.split("\\R");
        StringBuilder lead = new StringBuilder();
        StringBuilder bullets = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("-")) {
                if (bullets.length() > 0) {
                    bullets.append('\n');
                }
                bullets.append(line.substring(1).trim());
            } else if (!line.isEmpty()) {
                if (lead.length() > 0) {
                    lead.append(' ');
                }
                lead.append(line);
            }
        }
        fitnessLeadArea.setText(lead.toString());
        fitnessPropertiesArea.setText(bullets.toString());
        String leadText = lead.toString().trim();
        fitnessLeadUserEdited = !leadText.isEmpty() && !isDefaultFitnessLeadTemplate(leadText);
    }

    private boolean isDefaultFitnessLeadTemplate(String text) {
        return text != null
                && text.startsWith("Data are fit for use for ")
                && text.endsWith(" if they...");
    }

    /**
     * Builds the fitness-for-use requirements text in HTML format:
     * {@code <p>lead</p>\n<ul>\n<li>item1</li>\n...\ n</ul>}.
     * If there are no bullet points, only the lead paragraph is returned.
     * Returns an empty string if both fields are empty.
     */
    private String buildFitnessRequirementsText() {
        String lead = fitnessLeadArea.getText().trim();
        String[] propertyLines = fitnessPropertiesArea.getText().split("\\R");

        // Collect non-empty bullet items
        List<String> items = new java.util.ArrayList<>();
        for (String line : propertyLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }

        if (lead.isEmpty() && items.isEmpty()) {
            return "";
        }

        return buildFitnessHtml(lead, items);
    }

    /**
     * Formats fitness requirements as an HTML fragment with only {@code ul} and
     * {@code li} tags.  The introductory sentence is emitted as plain (HTML-escaped)
     * text on its own line; no {@code <p>} wrapper is added.
     *
     * @param lead  the introductory sentence (may be empty)
     * @param items bullet-point items (may be empty)
     * @return HTML-formatted string using only {@code ul}/{@code li} markup
     */
    private static String buildFitnessHtml(String lead, List<String> items) {
        StringBuilder text = new StringBuilder();
        if (!lead.isEmpty()) {
            text.append(escapeHtml(lead));
        }
        if (!items.isEmpty()) {
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append("<ul>\n");
            for (String item : items) {
                text.append("<li>").append(escapeHtml(item)).append("</li>\n");
            }
            text.append("</ul>");
        }
        return text.toString().trim();
    }

    /** Escapes HTML special characters in user-entered text. */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /** Reverses HTML escaping for the entities produced by {@link #escapeHtml}. */
    private static String unescapeHtml(String text) {
        if (text == null) {
            return "";
        }
        // &amp; must be unescaped last to avoid double-decoding (e.g. &amp;lt; → &lt; → <)
        return text.replace("&#39;", "'")
                   .replace("&quot;", "\"")
                   .replace("&gt;", ">")
                   .replace("&lt;", "<")
                   .replace("&amp;", "&");
    }

    private void markFitnessLeadEdited() {
        if (!updatingFitnessLeadTemplate) {
            fitnessLeadUserEdited = !fitnessLeadArea.getText().trim().isEmpty();
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
