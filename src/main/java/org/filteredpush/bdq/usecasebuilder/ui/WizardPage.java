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
 *   <li>{@link #validatePage()} – returns a list of validation error messages
 *       (empty list = page is valid)</li>
 * </ul>
 *
 * <p>Phase 3 additions:</p>
 * <ul>
 *   <li>{@link #getCompletionStatus()} – indicates the page's current completion
 *       state for display in the navigation sidebar.</li>
 *   <li>{@link #getPhaseLabel()} – short phase label (e.g., "A", "B") for the
 *       sidebar.</li>
 * </ul>
 */
public abstract class WizardPage extends JPanel {

    /** Completion status values for the navigation sidebar. */
    public enum CompletionStatus {
        /** The page has not been visited yet. */
        NOT_STARTED("Not started", new java.awt.Color(0xAAAAAA)),
        /** The page has been visited but may have missing optional data. */
        IN_PROGRESS("In progress", new java.awt.Color(0xF5A623)),
        /** The page has required data filled in. */
        READY("Ready", new java.awt.Color(0x4CAF50)),
        /** The page has issues that require attention. */
        NEEDS_ATTENTION("Needs attention", new java.awt.Color(0xE53935));

        private final String displayName;
        private final java.awt.Color color;

        CompletionStatus(String displayName, java.awt.Color color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public java.awt.Color getColor() {
            return color;
        }
    }

    /** The shared project state that all pages read from and write to. */
    protected final ProjectState state;

    /** Whether this page has been entered at least once. */
    private boolean visited = false;

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
     * Returns a short phase label for the sidebar (e.g., "A", "B", "1").
     * Defaults to an empty string; subclasses may override.
     */
    public String getPhaseLabel() {
        return "";
    }

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

    /**
     * Returns the completion status of this page for the navigation sidebar.
     *
     * <p>The default implementation returns {@link CompletionStatus#NOT_STARTED}
     * if not visited, or {@link CompletionStatus#IN_PROGRESS} if visited.
     * Subclasses should override to provide more meaningful status.</p>
     */
    public CompletionStatus getCompletionStatus() {
        return visited ? CompletionStatus.IN_PROGRESS : CompletionStatus.NOT_STARTED;
    }

    /**
     * Marks this page as visited. Called internally when onEnter() is invoked
     * through the framework. Subclasses should call {@code super.markVisited()} if
     * they override {@link #onEnter()}.
     */
    protected void markVisited() {
        this.visited = true;
    }

    /** Returns {@code true} if this page has been visited at least once. */
    public boolean isVisited() {
        return visited;
    }
}
