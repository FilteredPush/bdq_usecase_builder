package org.filteredpush.bdq.usecasebuilder.ui;

import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.service.VocabularyService;
import org.filteredpush.bdq.usecasebuilder.ui.pages.ExistingTestsPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.GapAnalysisPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.InformationElementsPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.NewTestPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.ParameterDefaultsPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.ConformanceDataPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.ReviewExportPage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.UseCasePage;
import org.filteredpush.bdq.usecasebuilder.ui.pages.WelcomePage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Main window for the BDQ Use Case Builder Swing wizard.
 *
 * <p>Phase 3 layout:</p>
 * <ul>
 *   <li><strong>Header</strong> – shows the current page title.</li>
 *   <li><strong>Phase sidebar</strong> (left) – lists all phases with their
 *       completion status indicator; clicking a phase name jumps to it.</li>
 *   <li><strong>Content area</strong> (center) – uses a {@link CardLayout} to
 *       display the current wizard page.</li>
 *   <li><strong>Navigation bar</strong> (bottom) – Back / Next / Finish /
 *       Cancel buttons delegating to the {@link WizardController}.</li>
 * </ul>
 *
 * <p>Window size (default 1400×900) and position are persisted via
 * {@link Preferences} so they are restored on next launch.</p>
 */
public class WizardFrame extends JFrame {

    // -----------------------------------------------------------------------
    // Persistence key prefix
    // -----------------------------------------------------------------------

    private static final String PREFS_NODE = "org/filteredpush/bdq/usecasebuilder";
    private static final String PREF_WIDTH  = "windowWidth";
    private static final String PREF_HEIGHT = "windowHeight";
    private static final String PREF_X      = "windowX";
    private static final String PREF_Y      = "windowY";

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    private final JLabel headerLabel;
    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private JPanel sidebarPanel;

    private JButton backButton;
    private JButton nextButton;
    private JButton finishButton;
    private JButton cancelButton;

    private List<JButton> sidebarButtons = new ArrayList<>();

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private WizardController controller;
    private List<WizardPage> pages;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates and wires the wizard frame.
     *
     * <p>The frame is not visible until {@link #setVisible(boolean)} is
     * called.</p>
     */
    public WizardFrame() {
        super("BDQ Use Case Builder – Guided Authoring Workbench");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Restore persisted window size/position
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        int w = prefs.getInt(PREF_WIDTH,  1400);
        int h = prefs.getInt(PREF_HEIGHT, 900);
        int x = prefs.getInt(PREF_X, -1);
        int y = prefs.getInt(PREF_Y, -1);

        setPreferredSize(new Dimension(w, h));
        setMinimumSize(new Dimension(1100, 750));
        setLayout(new BorderLayout());

        // Header
        headerLabel = new JLabel("", SwingConstants.LEFT);
        headerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBackground(new Color(0x2C5F8A));
        headerLabel.setOpaque(true);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(headerLabel, BorderLayout.NORTH);

        // Card panel (center)
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Sidebar placeholder – will be populated in wireController()
        sidebarPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        sidebarPanel.setBackground(new Color(0x3D6B8A));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setPreferredSize(new Dimension(190, 0));
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.add(sidebarScroll, BorderLayout.WEST);
        centerArea.add(cardPanel, BorderLayout.CENTER);
        add(centerArea, BorderLayout.CENTER);

        // Navigation bar
        add(buildNavBar(), BorderLayout.SOUTH);

        // Wire pages + controller
        wireController();

        pack();

        // Apply persisted position
        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }

        // Persist window size/position changes
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Preferences p = Preferences.userRoot().node(PREFS_NODE);
                p.putInt(PREF_WIDTH,  getWidth());
                p.putInt(PREF_HEIGHT, getHeight());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                Preferences p = Preferences.userRoot().node(PREFS_NODE);
                p.putInt(PREF_X, getX());
                p.putInt(PREF_Y, getY());
            }
        });
    }

    // -----------------------------------------------------------------------
    // Controller wiring
    // -----------------------------------------------------------------------

    private void wireController() {
        ProjectState state = new ProjectState();
        TestCatalogService catalogService = new TestCatalogService();
        catalogService.loadCatalog();
        VocabularyService vocabularyService = new VocabularyService();
        vocabularyService.load();

        pages = new ArrayList<>();
        pages.add(new WelcomePage(state));
        pages.add(new UseCasePage(state));
        pages.add(new InformationElementsPage(state, vocabularyService));
        pages.add(new ExistingTestsPage(state, catalogService));
        pages.add(new NewTestPage(state, vocabularyService, catalogService));
        pages.add(new ParameterDefaultsPage(state));
        pages.add(new GapAnalysisPage(state, catalogService));
        pages.add(new ConformanceDataPage(state));
        pages.add(new ReviewExportPage(state));

        for (int i = 0; i < pages.size(); i++) {
            cardPanel.add(pages.get(i), String.valueOf(i));
        }

        controller = new WizardController(this, state, pages);

        // Build sidebar buttons
        buildSidebar(pages);

        backButton.addActionListener(e -> controller.goBack());
        nextButton.addActionListener(e -> controller.goNext());
        finishButton.addActionListener(e -> controller.finish());
        cancelButton.addActionListener(e -> controller.cancel());
    }

    /**
     * Builds the phase sidebar with one button per page.
     */
    private void buildSidebar(List<WizardPage> pageList) {
        sidebarPanel.removeAll();
        sidebarButtons.clear();

        for (int i = 0; i < pageList.size(); i++) {
            WizardPage page = pageList.get(i);
            final int idx = i;

            JButton btn = new JButton(buildSidebarText(i, page));
            btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(0x3D6B8A));
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setToolTipText(page.getPageTitle());
            btn.addActionListener(e -> controller.jumpToPage(idx));
            sidebarButtons.add(btn);
            sidebarPanel.add(btn);
        }

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private String buildSidebarText(int index, WizardPage page) {
        String status = page.getCompletionStatus().getDisplayName();
        String title = page.getPageTitle();
        // Shorten for sidebar display
        if (title.length() > 22) {
            title = title.substring(0, 21) + "…";
        }
        return "<html><b>" + (index + 1) + ".</b> " + title
                + "<br><small><font color='#DDDDDD'>" + status + "</font></small></html>";
    }

    // -----------------------------------------------------------------------
    // Navigation bar
    // -----------------------------------------------------------------------

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        cancelButton = new JButton("Cancel");
        backButton = new JButton("< Back");
        nextButton = new JButton("Next >");
        finishButton = new JButton("Finish");

        bar.add(cancelButton);
        bar.add(backButton);
        bar.add(nextButton);
        bar.add(finishButton);

        return bar;
    }

    // -----------------------------------------------------------------------
    // Public API used by WizardController
    // -----------------------------------------------------------------------

    /**
     * Switches the card panel to the page at the given index, updates the
     * header label, and refreshes the sidebar status indicators.
     *
     * @param index zero-based page index
     */
    public void showPage(int index) {
        cardLayout.show(cardPanel, String.valueOf(index));
        WizardPage page = (WizardPage) cardPanel.getComponent(index);
        headerLabel.setText("Step " + (index + 1) + " of " + cardPanel.getComponentCount()
                + "  –  " + page.getPageTitle());
        refreshSidebar(index);
    }

    /**
     * Refreshes sidebar button text/colors to reflect current status.
     *
     * @param currentIndex the currently displayed page index
     */
    public void refreshSidebar(int currentIndex) {
        for (int i = 0; i < sidebarButtons.size() && i < pages.size(); i++) {
            WizardPage page = pages.get(i);
            JButton btn = sidebarButtons.get(i);
            btn.setText(buildSidebarText(i, page));
            boolean isCurrent = (i == currentIndex);
            btn.setBackground(isCurrent ? new Color(0x1A3E5C) : new Color(0x3D6B8A));
            WizardPage.CompletionStatus status = page.getCompletionStatus();
            btn.setForeground(status.getColor());
        }
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    /**
     * Updates the enabled state of navigation buttons.
     *
     * @param canBack  whether Back should be enabled
     * @param canNext  whether Next should be enabled
     * @param lastPage whether this is the last page (Finish shown instead of Next)
     */
    public void updateNavigationButtons(boolean canBack, boolean canNext, boolean lastPage) {
        backButton.setEnabled(canBack);
        nextButton.setEnabled(canNext && !lastPage);
        nextButton.setVisible(!lastPage);
        finishButton.setEnabled(lastPage);
        finishButton.setVisible(lastPage);
    }

    /**
     * Starts the wizard (shows the first page).
     *
     * <p>Call this after the frame has been made visible.</p>
     */
    public void startWizard() {
        controller.start();
    }

    /** Disposes the frame. */
    public void close() {
        dispose();
    }
}
