package org.filteredpush.bdq.usecasebuilder.ui;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;

import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all pages in the Swing wizard.
 *
 * <p>Each concrete page extends this class and overrides:</p>
 * <ul>
 *   <li>{@link #getPageTitle()} – the page heading shown in the wizard header</li>
 *   <li>{@link #onEnter()} – called every time the wizard navigates to this page,
 *       so the page can refresh its fields from the current project state</li>
 *   <li>{@link #onLeave()} – called before navigating away; the page should write
 *       its field values back into the project state</li>
 *   <li>{@link #validate()} – returns a list of validation error messages
 *       (empty list = page is valid)</li>
 * </ul>
 */
public abstract class WizardPage extends JPanel {

    /** The shared project state that all pages read from and write to. */
    protected final ProjectState state;

    /**
     * Creates a new wizard page backed by the given project state.
     *
     * @param state the shared project state; must not be {@code null}
     */
    protected WizardPage(ProjectState state) {
        this.state = state;
    }

    /**
     * Returns the display title for this page, shown in the wizard header.
     *
     * @return non-null, non-empty title string
     */
    public abstract String getPageTitle();

    /**
     * Called each time the wizard navigates to this page.
     * Implementations should populate UI fields from the current project state.
     */
    public abstract void onEnter();

    /**
     * Called just before the wizard navigates away from this page.
     * Implementations should write field values back to the project state.
     */
    public abstract void onLeave();

    /**
     * Validates the fields on this page.
     *
     * @return a list of human-readable error messages; an empty list means
     *         the page is valid and navigation may proceed
     */
    public List<String> validatePage() {
        return Collections.emptyList();
    }
}
