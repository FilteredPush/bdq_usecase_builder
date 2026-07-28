package org.filteredpush.bdq.usecasebuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.filteredpush.bdq.usecasebuilder.ui.WizardFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the BDQ Use Case Builder application.
 *
 * <p>Loads the bdqffdq.owl ontology and bdqtest RDF (from configured IRIs), plus
 * any optional user-specified RDF files. It then presents an interactive wizard
 * that guides the user through creating a new use case, associating tests with
 * it, and writing the resulting RDF serialization to a local output file.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   java -jar bdq-usecase-builder.jar [OPTIONS]
 *
 *   Options:
 *     --cli                   Run the wizard on the command line, without launching the Swing UI 
 *     -c, --config &lt;file&gt;     Path to configuration file (default: config.properties)
 *     -i, --input &lt;file|IRI&gt;  Additional RDF inputs (may be repeated)
 *     -o, --output &lt;file&gt;     Output file path (default: usecase-output.ttl)
 *     -f, --format &lt;fmt&gt;      Output format: TURTLE, RDF/XML, JSON-LD, N-TRIPLES
 *     -h, --help              Print help and exit
 * </pre>
 */
public class BdqUsecaseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BdqUsecaseBuilder.class);

    /** Default output file name when none is provided on the command line. */
    public static final String DEFAULT_OUTPUT_FILE = "usecase-output.ttl";

    /** Default RDF output format when none is provided on the command line. */
    public static final String DEFAULT_OUTPUT_FORMAT = "TURTLE";

    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("bdq-usecase-builder", options, true);
                return;
            }

            // by default launch the Swing wizard, unless cli is specified.
            if (!cmd.hasOption("cli")) {
                SwingUtilities.invokeLater(() -> {
                    WizardFrame wizardFrame = new WizardFrame();
                    wizardFrame.setVisible(true);
                    wizardFrame.startWizard();
                });
                return;
            }

            // Load configuration
            String configPath = cmd.getOptionValue("config", "config.properties");
            Configuration configuration = new Configuration(configPath);

            // Collect any additional user-specified input RDF files / IRIs
            List<String> inputSources = new ArrayList<>();
            if (cmd.hasOption("input")) {
                for (String input : cmd.getOptionValues("input")) {
                    inputSources.add(input);
                }
            }

            // Output destination
            String outputFile = cmd.getOptionValue("output", DEFAULT_OUTPUT_FILE);
            String outputFormat = cmd.getOptionValue("format", DEFAULT_OUTPUT_FORMAT);

            // Load all RDF sources into a single merged model
            RdfLoader loader = new RdfLoader(configuration);
            Model model = loader.loadAll(inputSources);
            logger.info("Loaded {} triples from all sources", model.size());

            // Present the interactive wizard
            UsecaseWizard wizard = new UsecaseWizard(model, configuration);
            UsecaseModel usecaseModel = wizard.run();

            if (usecaseModel != null) {
                // Write the resulting use case model to the output file
                RdfWriter writer = new RdfWriter();
                writer.write(usecaseModel.toModel(), outputFile, outputFormat);
                System.out.println("Use case written to: " + outputFile);
                logger.info("Use case written to {}", outputFile);
            } else {
                System.out.println("Wizard cancelled. No output written.");
            }

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("bdq-usecase-builder", options, true);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Application error", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Builds the set of command-line options accepted by the application.
     *
     * @return configured {@link Options} instance
     */
    static Options buildOptions() {
        Options options = new Options();

        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg()
                .argName("file")
                .desc("Path to configuration file (default: config.properties)")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .argName("file|IRI")
                .desc("Additional RDF input file or IRI to load (may be specified multiple times)")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("file")
                .desc("Output file path (default: " + DEFAULT_OUTPUT_FILE + ")")
                .build());

        options.addOption(Option.builder("f")
                .longOpt("format")
                .hasArg()
                .argName("format")
                .desc("Output RDF format: TURTLE, RDF/XML, JSON-LD, N-TRIPLES (default: " + DEFAULT_OUTPUT_FORMAT + ")")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Print this help message and exit")
                .build());

        options.addOption(Option.builder()
                .longOpt("cli")
                .desc("Launch the wizard on the command line, without the Swing UI")
                .build());

        return options;
    }
}
