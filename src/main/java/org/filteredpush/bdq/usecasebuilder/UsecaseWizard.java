package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

/**
 * Interactive console wizard for creating a BDQ use case.
 *
 * <p>The wizard presents a simple menu-driven interface that guides the user
 * through three steps:</p>
 * <ol>
 *   <li><strong>Create use case</strong> – the user provides a label, an IRI
 *       (or accepts an auto-generated {@code urn:uuid:…}), and an optional
 *       description.</li>
 *   <li><strong>Add tests</strong> – the wizard lists all
 *       {@code bdqffdq:DataQualityNeed} instances found in the loaded model
 *       and lets the user select one or more to associate with the use case
 *       via {@code bdqffdq:Policy} resources.</li>
 *   <li><strong>Review &amp; finish</strong> – a summary is printed and the
 *       completed {@link UsecaseModel} is returned to the caller for
 *       serialization.</li>
 * </ol>
 *
 * <p>The wizard can be cancelled at any time by selecting option 4 (Quit).
 * In that case {@link #run()} returns {@code null}.</p>
 */
public class UsecaseWizard {

    private static final Logger logger = LoggerFactory.getLogger(UsecaseWizard.class);

    private final Model sourceModel;
    private final Configuration configuration;
    private final Scanner scanner;

    /**
     * Creates a wizard that reads user input from {@link System#in}.
     *
     * @param sourceModel   the merged RDF model loaded from all configured sources
     * @param configuration application configuration
     */
    public UsecaseWizard(Model sourceModel, Configuration configuration) {
        this(sourceModel, configuration, System.in);
    }

    /**
     * Creates a wizard that reads user input from the given {@link InputStream}.
     * Useful for unit testing.
     *
     * @param sourceModel   the merged RDF model loaded from all configured sources
     * @param configuration application configuration
     * @param inputStream   source of user input
     */
    public UsecaseWizard(Model sourceModel, Configuration configuration, InputStream inputStream) {
        this.sourceModel = sourceModel;
        this.configuration = configuration;
        this.scanner = new Scanner(inputStream);
    }

    /**
     * Runs the interactive wizard.
     *
     * @return the completed {@link UsecaseModel}, or {@code null} if the user
     *         cancelled without creating a use case
     */
    public UsecaseModel run() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  BDQ Use Case Builder Wizard");
        System.out.println("========================================");
        System.out.println();

        UsecaseModel usecaseModel = null;
        boolean running = true;

        while (running) {
            printMenu(usecaseModel);
            String choice = promptInput("Enter choice: ").trim();

            switch (choice) {
                case "1":
                    usecaseModel = createUseCase();
                    break;
                case "2":
                    if (usecaseModel == null) {
                        System.out.println("Please create a use case first (option 1).");
                    } else {
                        addTestsToUseCase(usecaseModel);
                    }
                    break;
                case "3":
                    if (usecaseModel == null) {
                        System.out.println("Please create a use case first (option 1).");
                    } else {
                        showSummary(usecaseModel);
                        running = false;
                    }
                    break;
                case "4":
                    running = false;
                    usecaseModel = null;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, 3, or 4.");
            }
        }

        return usecaseModel;
    }

    // -----------------------------------------------------------------------
    // Menu steps
    // -----------------------------------------------------------------------

    private void printMenu(UsecaseModel current) {
        System.out.println();
        System.out.println("--- Menu ---");
        System.out.println("  1. Create use case"
                + (current != null ? "  [current: " + current.getLabel() + "]" : ""));
        System.out.println("  2. Add tests to use case"
                + (current != null ? "  (" + current.getTestIris().size() + " selected)" : ""));
        System.out.println("  3. Review and finish");
        System.out.println("  4. Quit / Cancel");
    }

    private UsecaseModel createUseCase() {
        System.out.println();
        System.out.println("--- Create Use Case ---");

        String label = promptInput("Use case label/name: ").trim();
        if (label.isEmpty()) {
            System.out.println("A label is required. Use case not created.");
            return null;
        }

        String defaultIri = configuration.getUsecaseBaseIri() + UUID.randomUUID();
        System.out.println("Suggested IRI: " + defaultIri);
        String iri = promptInput("Use case IRI (press Enter to accept suggestion): ").trim();
        if (iri.isEmpty()) {
            iri = defaultIri;
        }

        String description = promptInput("Use case description (optional, press Enter to skip): ").trim();

        UsecaseModel model = new UsecaseModel(iri, label, description);
        System.out.println();
        System.out.println("Use case created:");
        System.out.println("  Label : " + label);
        System.out.println("  IRI   : " + iri);
        return model;
    }

    private void addTestsToUseCase(UsecaseModel usecaseModel) {
        System.out.println();
        System.out.println("--- Add Tests ---");

        List<TestEntry> tests = findAvailableTests();
        if (tests.isEmpty()) {
            System.out.println("No tests (bdqffdq:DataQualityNeed subclass instances) found in");
            System.out.println("the loaded RDF data.  Load a bdqtest file with -i <file|IRI>.");
            return;
        }

        System.out.println("Available tests (" + tests.size() + " found):");
        for (int i = 0; i < tests.size(); i++) {
            String marker = usecaseModel.getTestIris().contains(tests.get(i).iri) ? "[X]" : "[ ]";
            System.out.printf("  %s %3d. %s%n", marker, i + 1, tests.get(i).label);
            System.out.printf("           <%s>%n", tests.get(i).iri);
        }

        System.out.println();
        System.out.println("Enter test number(s) to toggle (comma-separated),");
        System.out.println("  'all' to add all tests, or press Enter to leave selection unchanged:");
        String input = promptInput("> ").trim();

        if (input.equalsIgnoreCase("all")) {
            for (TestEntry test : tests) {
                usecaseModel.addTest(test.iri);
            }
            System.out.println("Added all " + tests.size() + " tests.");
        } else if (!input.isEmpty()) {
            int added = 0;
            for (String part : input.split(",")) {
                try {
                    int index = Integer.parseInt(part.trim()) - 1;
                    if (index >= 0 && index < tests.size()) {
                        usecaseModel.addTest(tests.get(index).iri);
                        added++;
                    } else {
                        System.out.println("  Invalid test number: " + (index + 1));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  Not a number: '" + part.trim() + "'");
                }
            }
            System.out.println("Added " + added + " test(s). Total selected: "
                    + usecaseModel.getTestIris().size());
        }
    }

    private void showSummary(UsecaseModel usecaseModel) {
        System.out.println();
        System.out.println("--- Summary ---");
        System.out.println("  Label       : " + usecaseModel.getLabel());
        System.out.println("  IRI         : " + usecaseModel.getIri());
        System.out.println("  Description : " + usecaseModel.getDescription());
        System.out.println("  Tests (" + usecaseModel.getTestIris().size() + "):");
        for (String testIri : usecaseModel.getTestIris()) {
            System.out.println("    - " + testIri);
        }
    }

    // -----------------------------------------------------------------------
    // Test discovery
    // -----------------------------------------------------------------------

    /**
     * Finds all {@code bdqffdq:DataQualityNeed} instances in the loaded model.
     *
     * <p>First tries a SPARQL query that traverses the class hierarchy
     * ({@code rdfs:subClassOf*}); if that produces no results (e.g. because the
     * ontology is not loaded), it falls back to a direct type-matching scan for
     * the four concrete bdqffdq test types.</p>
     *
     * @return sorted list of {@link TestEntry} objects; never {@code null}
     */
    public List<TestEntry> findAvailableTests() {
        List<TestEntry> tests = findTestsBySparql();
        if (tests.isEmpty()) {
            tests = findTestsByType();
        }
        return tests;
    }

    private List<TestEntry> findTestsBySparql() {
        List<TestEntry> tests = new ArrayList<>();
        String sparql = "PREFIX bdqffdq: <" + BdqFfdq.NS + "> "
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                + "SELECT DISTINCT ?test ?label WHERE { "
                + "  ?test a ?type . "
                + "  ?type rdfs:subClassOf* bdqffdq:DataQualityNeed . "
                + "  OPTIONAL { ?test rdfs:label ?label } "
                + "} ORDER BY ?label";
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, sourceModel)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                Resource testRes = sol.getResource("test");
                if (testRes == null || testRes.getURI() == null) {
                    continue;
                }
                String iri = testRes.getURI();
                String label = sol.contains("label") ? sol.getLiteral("label").getString() : iri;
                tests.add(new TestEntry(iri, label));
            }
        } catch (Exception e) {
            logger.debug("SPARQL test discovery failed ({}); using fallback", e.getMessage());
        }
        return tests;
    }

    private List<TestEntry> findTestsByType() {
        List<TestEntry> tests = new ArrayList<>();
        Set<Resource> testTypes = new HashSet<>(Arrays.asList(
                BdqFfdq.Validation,
                BdqFfdq.Measure,
                BdqFfdq.Amendment,
                BdqFfdq.Issue));

        for (Resource testType : testTypes) {
            ResIterator it = sourceModel.listSubjectsWithProperty(RDF.type, testType);
            while (it.hasNext()) {
                Resource resource = it.next();
                String iri = resource.getURI();
                if (iri == null) {
                    continue; // skip blank nodes
                }
                Statement labelStmt = resource.getProperty(RDFS.label);
                String label = labelStmt != null ? labelStmt.getString() : iri;
                tests.add(new TestEntry(iri, label));
            }
        }
        tests.sort(Comparator.comparing(t -> t.label));
        return tests;
    }

    // -----------------------------------------------------------------------
    // I/O helper
    // -----------------------------------------------------------------------

    private String promptInput(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return "";
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    /**
     * Lightweight holder for a test IRI and its human-readable label, used
     * when presenting the test selection list to the user.
     */
    public static class TestEntry {
        /** The IRI of the test resource. */
        public final String iri;
        /** The human-readable label of the test. */
        public final String label;

        public TestEntry(String iri, String label) {
            this.iri = iri;
            this.label = label;
        }
    }
}
