package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Loads RDF/OWL content into an Apache Jena {@link Model} from HTTP/HTTPS IRIs
 * and from local file paths.
 *
 * <p>On startup the loader always attempts to fetch the configured bdqffdq
 * ontology and bdqtest RDF from their known IRI locations (which may be remote
 * URLs or local file paths).  Additional sources, such as user-supplied local
 * files, can be injected via {@link #loadAll(List)}.</p>
 *
 * <p>Any source that cannot be read (network error, unparseable format, …) is
 * logged as a warning and silently skipped so that the application can continue
 * with whatever data was successfully loaded.</p>
 */
public class RdfLoader {

    private static final Logger logger = LoggerFactory.getLogger(RdfLoader.class);

    private final Configuration configuration;

    /**
     * Creates a new loader backed by the given configuration.
     *
     * @param configuration application configuration (must not be {@code null})
     */
    public RdfLoader(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Loads all RDF sources into a single merged model.
     *
     * <p>Sources are loaded in this order:</p>
     * <ol>
     *   <li>bdqffdq ontology (from {@link Configuration#getBdqFfdqOntologyIri()})</li>
     *   <li>bdqtest RDF (from {@link Configuration#getBdqTestRdfIri()})</li>
     *   <li>Any extra IRIs listed in the configuration</li>
     *   <li>The user-supplied {@code additionalSources}</li>
     * </ol>
     *
     * @param additionalSources list of file paths or IRIs supplied by the user;
     *                          may be {@code null} or empty
     * @return merged Jena {@link Model} containing all successfully loaded triples
     */
    public Model loadAll(List<String> additionalSources) {
        Model model = ModelFactory.createDefaultModel();

        // Core vocabulary: bdqffdq ontology
        String bdqFfdqIri = configuration.getBdqFfdqOntologyIri();
        if (bdqFfdqIri != null && !bdqFfdqIri.isEmpty()) {
            loadIntoModel(model, bdqFfdqIri);
        }

        // Core vocabulary: bdqtest RDF
        String bdqTestIri = configuration.getBdqTestRdfIri();
        if (bdqTestIri != null && !bdqTestIri.isEmpty()) {
            loadIntoModel(model, bdqTestIri);
        }

        // Optional extra IRIs from the configuration file
        for (String iri : configuration.getExtraRdfIris()) {
            loadIntoModel(model, iri);
        }

        // User-supplied additional sources (typically local files)
        if (additionalSources != null) {
            for (String source : additionalSources) {
                loadIntoModel(model, source);
            }
        }

        logger.info("Total triples loaded: {}", model.size());
        return model;
    }

    /**
     * Loads a single RDF source into the given model.
     *
     * <p>The source may be any form accepted by Jena's
     * {@link RDFDataMgr#read(Model, String)}: an HTTP/HTTPS URL, a
     * {@code file:} URI, or a relative file path.</p>
     *
     * @param model  the model to merge loaded triples into
     * @param source the file path or IRI to load
     */
    public void loadIntoModel(Model model, String source) {
        try {
            logger.info("Loading RDF from: {}", source);
            RDFDataMgr.read(model, source);
            logger.debug("Successfully loaded: {}", source);
        } catch (RiotException e) {
            logger.warn("Could not parse RDF from '{}': {}", source, e.getMessage());
        } catch (Exception e) {
            logger.warn("Could not load RDF from '{}': {}", source, e.getMessage());
        }
    }

    /**
     * Convenience factory: creates and returns a new, empty Jena {@link Model}.
     *
     * @return an empty default model
     */
    public Model createEmptyModel() {
        return ModelFactory.createDefaultModel();
    }
}
