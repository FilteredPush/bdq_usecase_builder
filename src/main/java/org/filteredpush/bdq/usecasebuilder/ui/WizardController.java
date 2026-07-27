package org.filteredpush.bdq.usecasebuilder.ui;

import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.util.List;

/**
 * Central controller that manages wizard page state and navigation.
 *
 * <p>Phase 3: The controller supports cyclical (non-linear) navigation.
 * Users can jump to any previously-visited page and navigate backward/forward
 * freely without losing state. Each page exposes a completion status via
 * {@link WizardPage#getCompletionStatus()} used by the navigation sidebar.</p>
 *
 * <p>The controller holds the ordered list of pages, keeps track of the
 * current page index, and exposes navigation methods ({@link #goNext()},
 * {@link #goBack()}, {@link #jumpToPage(int)}, {@link #finish()},
 * {@link #cancel()}) that the {@link WizardFrame} delegates button clicks
 * to.</p>
 *
 * <p>Before moving to the next page the controller calls
 * {@link WizardPage#validatePage()}; if any errors are found, a dialog is
 * shown and navigation is blocked. Going backward or jumping does not
 * require validation.</p>
 */
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    private final WizardFrame frame;
    private final ProjectState state;
    private final List<WizardPage> pages;
    private int currentIndex = 0;

    /**
     * Creates the controller.
     *
     * @param frame  the wizard frame this controller drives
     * @param state  the shared project state
     * @param pages  the ordered list of wizard pages
     */
    public WizardController(WizardFrame frame, ProjectState state, List<WizardPage> pages) {
        this.frame = frame;
        this.state = state;
        this.pages = pages;
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /** Returns the zero-based index of the currently displayed page. */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /** Returns the total number of pages in the wizard. */
    public int getPageCount() {
        return pages.size();
    }

    /** Returns {@code true} if there is a page before the current one. */
    public boolean canGoBack() {
        return currentIndex > 0;
    }

    /** Returns {@code true} if there is a page after the current one. */
    public boolean canGoNext() {
        return currentIndex < pages.size() - 1;
    }

    /** Returns {@code true} when the current page is the last page. */
    public boolean isLastPage() {
        return currentIndex == pages.size() - 1;
    }

    /**
     * Attempts to navigate to the next page.
     *
     * <p>Calls {@link WizardPage#validatePage()} on the current page first; if any
     * errors are returned, shows them in a dialog and does not advance.</p>
     */
    public void goNext() {
        WizardPage current = pages.get(currentIndex);
        List<String> errors = current.validatePage();
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return;
        }
        current.onLeave();
        currentIndex++;
        WizardPage next = pages.get(currentIndex);
        next.markVisited();
        next.onEnter();
        frame.showPage(currentIndex);
        frame.updateNavigationButtons(canGoBack(), canGoNext(), isLastPage());
    }

    /**
     * Navigates to the previous page without validation.
     */
    public void goBack() {
        if (!canGoBack()) {
            return;
        }
        pages.get(currentIndex).onLeave();
        currentIndex--;
        WizardPage prev = pages.get(currentIndex);
        prev.markVisited();
        prev.onEnter();
        frame.showPage(currentIndex);
        frame.updateNavigationButtons(canGoBack(), canGoNext(), isLastPage());
    }

    /**
     * Jumps directly to the page at the given index without validation.
     *
     * <p>The current page's {@link WizardPage#onLeave()} is called first so
     * that any in-progress edits are saved to state before navigating away.
     * Then the target page's {@link WizardPage#onEnter()} is called.</p>
     *
     * @param targetIndex zero-based index of the target page
     */
    public void jumpToPage(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= pages.size() || targetIndex == currentIndex) {
            return;
        }
        pages.get(currentIndex).onLeave();
        currentIndex = targetIndex;
        WizardPage target = pages.get(currentIndex);
        target.markVisited();
        target.onEnter();
        frame.showPage(currentIndex);
        frame.updateNavigationButtons(canGoBack(), canGoNext(), isLastPage());
    }

    /**
     * Triggers export of the project and closes the wizard.
     *
     * <p>Called when the user clicks Finish on the last page.</p>
     */
    public void finish() {
        WizardPage current = pages.get(currentIndex);
        List<String> errors = current.validatePage();
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return;
        }
        current.onLeave();

        ExportService exportService = new ExportService();
        try {
            String result = exportService.export(state);
            JOptionPane.showMessageDialog(frame,
                    result,
                    "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
            logger.info("Export complete");
        } catch (Exception e) {
            logger.error("Export failed", e);
            JOptionPane.showMessageDialog(frame,
                    "Export failed: " + e.getMessage(),
                    "Export error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        frame.close();
    }

    /**
     * Cancels the wizard, asking for confirmation, and closes the window.
     */
    public void cancel() {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Cancel the wizard? Any unsaved work will be lost.",
                "Cancel",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            frame.close();
        }
    }

    // -----------------------------------------------------------------------
    // Startup
    // -----------------------------------------------------------------------

    /**
     * Shows the first page of the wizard.
     *
     * <p>Should be called once after the frame has been built and made
     * visible.</p>
     */
    public void start() {
        currentIndex = 0;
        WizardPage first = pages.get(currentIndex);
        first.markVisited();
        first.onEnter();
        frame.showPage(currentIndex);
        frame.updateNavigationButtons(canGoBack(), canGoNext(), isLastPage());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void showValidationErrors(List<String> errors) {
        StringBuilder sb = new StringBuilder("Please fix the following before continuing:\n\n");
        for (String error : errors) {
            sb.append("• ").append(error).append('\n');
        }
        JOptionPane.showMessageDialog(frame,
                sb.toString(),
                "Validation",
                JOptionPane.WARNING_MESSAGE);
    }
}
