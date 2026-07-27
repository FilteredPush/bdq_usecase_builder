package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogEntry;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads BDQ tests and use cases from an RDF document (Turtle, RDF/XML, JSON-LD, …)
 * using Apache Jena.
 *
 * <p>The service recognises resources typed as {@code bdqffdq:UseCase} and
 * common BDQ test types (Validation, Measure, Amendment, Issue) and returns them
 * as lightweight {@link UseCaseDraft} and {@link TestCatalogEntry} objects that
 * can be injected into the running wizard session.</p>
 */
public class RdfProjectLoader {

    private static final Logger logger = LoggerFactory.getLogger(RdfProjectLoader.class);

    private static final String BDQFFDQ_NS  = "https://rs.tdwg.org/bdqffdq/terms/";
    private static final String BDQCORE_NS  = "https://rs.tdwg.org/bdq/terms/";
    private static final String SKOS_NS     = "http://www.w3.org/2004/02/skos/core#";
    private static final String DCTERMS_NS  = "http://purl.org/dc/terms/";

    /**
     * Loads an RDF document from the given file path or URI and returns all
     * recognised use-case resources.
     *
     * @param source file path or URI to load
     * @return list of {@link UseCaseDraft} objects; empty if none found or load fails
     */
    public List<UseCaseDraft> loadUseCases(String source) {
        List<UseCaseDraft> result = new ArrayList<>();
        Model model = loadModel(source);
        if (model == null) {
            return result;
        }
        Property useCaseType = model.createProperty(BDQFFDQ_NS + "UseCase");
        ResIterator it = model.listSubjectsWithProperty(RDF.type, useCaseType);
        while (it.hasNext()) {
            Resource res = it.next();
            UseCaseDraft draft = new UseCaseDraft();
            draft.setName(extractLabel(model, res));
            draft.setDescription(extractDescription(model, res));
            if (draft.getName() == null || draft.getName().trim().isEmpty()) {
                String localName = res.isURIResource() ? res.getLocalName() : res.toString();
                draft.setName(localName);
            }
            result.add(draft);
        }
        // Also look with alternate namespace
        Property useCaseType2 = model.createProperty(BDQCORE_NS + "UseCase");
        ResIterator it2 = model.listSubjectsWithProperty(RDF.type, useCaseType2);
        while (it2.hasNext()) {
            Resource res = it2.next();
            UseCaseDraft draft = new UseCaseDraft();
            draft.setName(extractLabel(model, res));
            draft.setDescription(extractDescription(model, res));
            if (draft.getName() == null || draft.getName().trim().isEmpty()) {
                draft.setName(res.isURIResource() ? res.getLocalName() : res.toString());
            }
            result.add(draft);
        }
        logger.info("Loaded {} use cases from: {}", result.size(), source);
        return result;
    }

    /**
     * Loads an RDF document from the given file path or URI and returns all
     * recognised BDQ test resources as catalog entries.
     *
     * @param source file path or URI to load
     * @return list of {@link TestCatalogEntry} objects; empty if none found or load fails
     */
    public List<TestCatalogEntry> loadTestEntries(String source) {
        List<TestCatalogEntry> result = new ArrayList<>();
        Model model = loadModel(source);
        if (model == null) {
            return result;
        }
        // Known BDQ test type URIs
        String[] testTypeUris = {
            BDQFFDQ_NS + "Validation",
            BDQFFDQ_NS + "Measure",
            BDQFFDQ_NS + "Amendment",
            BDQFFDQ_NS + "Issue",
            BDQCORE_NS + "Validation",
            BDQCORE_NS + "Measure",
            BDQCORE_NS + "Amendment",
            BDQCORE_NS + "Issue"
        };
        for (String typeUri : testTypeUris) {
            ResIterator it = model.listSubjectsWithProperty(RDF.type,
                    model.createResource(typeUri));
            while (it.hasNext()) {
                Resource res = it.next();
                String iri = res.isURIResource() ? res.getURI() : res.getId().getLabelString();
                String label = extractLabel(model, res);
                String prefLabel = extractPrefLabel(model, res);
                String typeName = typeUri.substring(typeUri.lastIndexOf('/') + 1);
                String dimension = extractObjectLiteral(model, res,
                        model.createProperty(BDQFFDQ_NS + "hasDimension"));
                TestCatalogEntry entry = new TestCatalogEntry(
                        iri,
                        label != null ? label : "",
                        prefLabel != null ? prefLabel : "",
                        typeName,
                        dimension != null ? dimension : "");
                result.add(entry);
            }
        }
        logger.info("Loaded {} test entries from: {}", result.size(), source);
        return result;
    }

    /**
     * Loads an RDF document and extracts information-element terms from it.
     *
     * <p>Extracts terms typed as {@code rdf:Property}, {@code owl:DatatypeProperty},
     * {@code owl:ObjectProperty}, or {@code owl:AnnotationProperty}.  The namespace
     * prefix declared in the model (or inferred from the terms) is used to form
     * {@code prefix:localName} strings.</p>
     *
     * @param source file path or URI to load
     * @return list of qualified-name strings (e.g. {@code "dwc:scientificName"});
     *         empty if none found or load fails
     */
    public List<String> loadVocabularyTerms(String source) {
        java.util.LinkedHashSet<String> termSet = new java.util.LinkedHashSet<>();
        Model model = loadModel(source);
        if (model == null) {
            return new ArrayList<>();
        }
        String[] propTypeUris = {
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
            "http://www.w3.org/2002/07/owl#DatatypeProperty",
            "http://www.w3.org/2002/07/owl#ObjectProperty",
            "http://www.w3.org/2002/07/owl#AnnotationProperty"
        };
        for (String typeUri : propTypeUris) {
            ResIterator it = model.listSubjectsWithProperty(RDF.type,
                    model.createResource(typeUri));
            while (it.hasNext()) {
                Resource res = it.next();
                if (!res.isURIResource()) {
                    continue;
                }
                String uri = res.getURI();
                String qname = resolveQName(model, uri);
                if (qname != null) {
                    termSet.add(qname);
                }
            }
        }
        List<String> terms = new ArrayList<>(termSet);
        logger.info("Loaded {} vocabulary terms from: {}", terms.size(), source);
        return terms;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Model loadModel(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, source.trim());
            return model;
        } catch (RiotException e) {
            logger.warn("Could not parse RDF from '{}': {}", source, e.getMessage());
        } catch (Exception e) {
            logger.warn("Could not load RDF from '{}': {}", source, e.getMessage());
        }
        return null;
    }

    private String extractLabel(Model model, Resource res) {
        // Try rdfs:label first
        Statement stmt = res.getProperty(RDFS.label);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        // Try skos:prefLabel
        Property prefLabelProp = model.createProperty(SKOS_NS + "prefLabel");
        stmt = res.getProperty(prefLabelProp);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        // Try dcterms:title
        Property titleProp = model.createProperty(DCTERMS_NS + "title");
        stmt = res.getProperty(titleProp);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        return null;
    }

    private String extractPrefLabel(Model model, Resource res) {
        Property prefLabelProp = model.createProperty(SKOS_NS + "prefLabel");
        Statement stmt = res.getProperty(prefLabelProp);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        return null;
    }

    private String extractDescription(Model model, Resource res) {
        Property descProp = model.createProperty(DCTERMS_NS + "description");
        Statement stmt = res.getProperty(descProp);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        // Try rdfs:comment
        stmt = res.getProperty(RDFS.comment);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getLiteral().getString();
        }
        return null;
    }

    private String extractObjectLiteral(Model model, Resource res, Property prop) {
        Statement stmt = res.getProperty(prop);
        if (stmt == null) {
            return null;
        }
        RDFNode obj = stmt.getObject();
        if (obj.isLiteral()) {
            return obj.asLiteral().getString();
        }
        if (obj.isURIResource()) {
            return obj.asResource().getLocalName();
        }
        return null;
    }

    private String resolveQName(Model model, String uri) {
        // Try to use the model's registered prefix declarations
        String qname = model.qnameFor(uri);
        if (qname != null && qname.contains(":")) {
            return qname;
        }
        // Fall back to deriving prefix from the namespace (last segment before /)
        int hash = uri.lastIndexOf('#');
        int slash = uri.lastIndexOf('/');
        int sep = Math.max(hash, slash);
        if (sep <= 0 || sep >= uri.length() - 1) {
            return null;
        }
        String localName = uri.substring(sep + 1);
        if (localName.isEmpty()) {
            return null;
        }
        String ns = uri.substring(0, sep + 1);
        String prefix = model.getNsURIPrefix(ns);
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + ":" + localName;
        }
        return null;
    }
}
