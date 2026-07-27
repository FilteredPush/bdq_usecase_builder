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
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window for the BDQ Use Case Builder Swing wizard.
 *
 * <p>The frame is divided into three vertical sections:</p>
 * <ol>
 *   <li><strong>Header</strong> – shows the current page title.</li>
 *   <li><strong>Content area</strong> – uses a {@link CardLayout} to display
 *       the current wizard page.</li>
 *   <li><strong>Navigation bar</strong> – Back / Next / Finish / Cancel
 *       buttons delegating to the {@link WizardController}.</li>
 * </ol>
 *
 * <p>Build and display the wizard with:</p>
 * <pre>
 *   SwingUtilities.invokeLater(() -> {
 *       WizardFrame frame = new WizardFrame();
 *       frame.setVisible(true);
 *   });
 * </pre>
 */
public class WizardFrame extends JFrame {

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    private final JLabel headerLabel;
    private final JPanel cardPanel;
    private final CardLayout cardLayout;

    private JButton backButton;
    private JButton nextButton;
    private JButton finishButton;
    private JButton cancelButton;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private WizardController controller;

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
        super("BDQ Use Case Builder Wizard");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(900, 700));
        setLayout(new BorderLayout());

        // Header
        headerLabel = new JLabel("", SwingConstants.LEFT);
        headerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBackground(new Color(0x2C5F8A));
        headerLabel.setOpaque(true);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(headerLabel, BorderLayout.NORTH);

        // Card panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(cardPanel, BorderLayout.CENTER);

        // Navigation bar
        add(buildNavBar(), BorderLayout.SOUTH);

        // Wire pages + controller
        wireController();

        pack();
        setLocationRelativeTo(null);
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

        List<WizardPage> pages = new ArrayList<>();
        pages.add(new WelcomePage(state));
        pages.add(new UseCasePage(state));
        pages.add(new InformationElementsPage(state, vocabularyService));
        pages.add(new ExistingTestsPage(state, catalogService));
        pages.add(new NewTestPage(state, vocabularyService, catalogService));
        pages.add(new ParameterDefaultsPage(state));
        pages.add(new GapAnalysisPage(state));
        pages.add(new ConformanceDataPage(state));
        pages.add(new ReviewExportPage(state));

        for (int i = 0; i < pages.size(); i++) {
            cardPanel.add(pages.get(i), String.valueOf(i));
        }

        controller = new WizardController(this, state, pages);

        backButton.addActionListener(e -> controller.goBack());
        nextButton.addActionListener(e -> controller.goNext());
        finishButton.addActionListener(e -> controller.finish());
        cancelButton.addActionListener(e -> controller.cancel());
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
     * Switches the card panel to the page at the given index and updates the
     * header label.
     *
     * @param index zero-based page index
     */
    public void showPage(int index) {
        cardLayout.show(cardPanel, String.valueOf(index));
        WizardPage page = (WizardPage) cardPanel.getComponent(index);
        headerLabel.setText("Step " + (index + 1) + " of " + cardPanel.getComponentCount()
                + "  –  " + page.getPageTitle());
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
