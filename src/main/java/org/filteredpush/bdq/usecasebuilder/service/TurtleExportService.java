package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Exports a {@link ProjectState} as RDF/Turtle conforming to the bdqffdq
 * ontology.
 *
 * <p>Two export modes are supported:</p>
 * <ul>
 *   <li><b>Minimal</b> – exports only the newly authored Use Case and its
 *       newly authored tests plus required supporting nodes.</li>
 *   <li><b>Include existing</b> – additionally includes stubs for the
 *       existing BDQ tests that were selected/referenced in the project.</li>
 * </ul>
 *
 * <p>The output file is named {@code usecase_new.ttl} (minimal) or
 * {@code usecase_with_existing.ttl} (include-existing mode) and is written to
 * the configured output directory.</p>
 *
 * <p>Dimension, criterion, and enhancement values are exported as typed
 * resources using their respective namespaces:
 * {@code bdqdim:}, {@code bdqcrit:}, and {@code bdqenh:}.</p>
 */
public class TurtleExportService {

    private static final Logger logger = LoggerFactory.getLogger(TurtleExportService.class);

    /** Base namespace for newly authored resources (urn-based by default). */
    private static final String DEFAULT_BASE = "urn:uuid:";

    /** Namespace for bdqdim (data quality dimension) terms. */
    static final String BDQDIM_NS = "https://rs.tdwg.org/bdqdim/terms/";

    /** Namespace for bdqcrit (data quality criterion) terms. */
    static final String BDQCRIT_NS = "https://rs.tdwg.org/bdqcrit/terms/";

    /** Namespace for bdqenh (data quality enhancement) terms. */
    static final String BDQENH_NS = "https://rs.tdwg.org/bdqenh/terms/";

    /** Prefix declarations emitted at the top of the Turtle file. */
    private static final String PREFIXES =
            "@prefix rdf:      <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
            + "@prefix rdfs:     <http://www.w3.org/2000/01/rdf-schema#> .\n"
            + "@prefix owl:      <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix skos:     <http://www.w3.org/2004/02/skos/core#> .\n"
            + "@prefix dcterms:  <http://purl.org/dc/terms/> .\n"
            + "@prefix bdqffdq:  <https://rs.tdwg.org/bdqffdq/terms/> .\n"
            + "@prefix bdqtest:  <https://rs.tdwg.org/bdqtest/terms/> .\n"
            + "@prefix dwc:      <http://rs.tdwg.org/dwc/terms/> .\n";

    /**
     * Exports Turtle in minimal mode (new use case + new tests only).
     *
     * @param state     project state to export
     * @param outputDir directory to write the file into
     * @return the written file
     * @throws IOException on write failure
     */
    public File exportMinimal(ProjectState state, File outputDir) throws IOException {
        return export(state, outputDir, false, null);
    }

    /**
     * Exports Turtle in include-existing mode (new use case + new tests +
     * selected existing test stubs).
     *
     * @param state          project state to export
     * @param outputDir      directory to write the file into
     * @param catalogService catalog service used to look up existing test labels
     * @return the written file
     * @throws IOException on write failure
     */
    public File exportWithExisting(ProjectState state, File outputDir,
            TestCatalogService catalogService) throws IOException {
        return export(state, outputDir, true, catalogService);
    }

    // -----------------------------------------------------------------------
    // Internal implementation
    // -----------------------------------------------------------------------

    private File export(ProjectState state, File outputDir, boolean includeExisting,
            TestCatalogService catalogService) throws IOException {
        if (state == null) {
            throw new IllegalArgumentException("ProjectState must not be null");
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }

        String fileName = includeExisting ? "usecase_with_existing.ttl" : "usecase_new.ttl";
        File outFile = new File(outputDir, fileName);

        Model model = buildModel(state, includeExisting, catalogService);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            model.write(fos, "TURTLE");
        }

        logger.info("Wrote Turtle export to {}", outFile.getAbsolutePath());
        return outFile;
    }

    /**
     * Builds the Jena {@link Model} representing the project state.
     */
    Model buildModel(ProjectState state, boolean includeExisting,
            TestCatalogService catalogService) {
        Model model = ModelFactory.createDefaultModel();

        // Set namespace prefixes for nicer Turtle output
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("bdqffdq", BdqFfdq.NS);
        model.setNsPrefix("bdqtest", "https://rs.tdwg.org/bdqtest/terms/");
        model.setNsPrefix("bdqdim", BDQDIM_NS);
        model.setNsPrefix("bdqcrit", BDQCRIT_NS);
        model.setNsPrefix("bdqenh", BDQENH_NS);
        model.setNsPrefix("dwc", "http://rs.tdwg.org/dwc/terms/");

        UseCaseDraft uc = state.getUseCaseDraft();

        // --- Use Case resource ---
        String ucIri = sanitizeIri(uc.getName(), "UseCase");
        Resource ucRes = model.createResource(ucIri);
        ucRes.addProperty(RDF.type, BdqFfdq.UseCase);
        if (uc.getName() != null && !uc.getName().isEmpty()) {
            ucRes.addProperty(RDFS.label, uc.getName());
            ucRes.addProperty(SKOS.prefLabel, uc.getName());
        }
        if (uc.getDescription() != null && !uc.getDescription().isEmpty()) {
            ucRes.addProperty(RDFS.comment, uc.getDescription());
        }
        if (uc.getFitnessRequirementsText() != null
                && !uc.getFitnessRequirementsText().isEmpty()) {
            ucRes.addProperty(
                    model.createProperty("http://purl.org/dc/terms/", "description"),
                    uc.getFitnessRequirementsText());
        }

        // --- Policy resource linking UC to tests ---
        String policyIri = ucIri + "_Policy";
        Resource policyRes = model.createResource(policyIri);
        policyRes.addProperty(RDF.type, BdqFfdq.Policy);
        policyRes.addProperty(BdqFfdq.hasUseCase, ucRes);

        // --- New test resources ---
        List<Resource> newTestResources = new ArrayList<>();
        for (TestDraft td : state.getNewTestDrafts()) {
            Resource testRes = buildTestResource(model, td);
            newTestResources.add(testRes);
            policyRes.addProperty(BdqFfdq.includesInPolicy, testRes);
        }

        // --- Existing test stubs (include-existing mode) ---
        if (includeExisting && !state.getSelectedExistingTestIris().isEmpty()) {
            for (String iri : state.getSelectedExistingTestIris()) {
                Resource existingTest = model.createResource(iri);
                // Add a minimal type if we have catalog info
                if (catalogService != null) {
                    catalogService.getEntries().stream()
                            .filter(e -> iri.equals(e.getIri()))
                            .findFirst()
                            .ifPresent(entry -> {
                                if (entry.getLabel() != null) {
                                    existingTest.addProperty(RDFS.label, entry.getLabel());
                                }
                                if (entry.getType() != null) {
                                    existingTest.addProperty(RDF.type,
                                            testTypeResource(entry.getType()));
                                }
                            });
                } else {
                    // Minimal stub
                    existingTest.addProperty(RDF.type, BdqFfdq.DataQualityNeed);
                }
                policyRes.addProperty(BdqFfdq.includesInPolicy, existingTest);
            }
        }

        return model;
    }

    private Resource buildTestResource(Model model, TestDraft td) {
        String iri = DEFAULT_BASE + UUID.nameUUIDFromBytes(
                ("test:" + (td.getLabel() != null ? td.getLabel() : UUID.randomUUID().toString()))
                .getBytes(StandardCharsets.UTF_8));
        Resource testRes = model.createResource(iri);

        // Type
        Resource typeRes = testTypeResource(td.getType() != null
                ? td.getType().name() : null);
        if (typeRes != null) {
            testRes.addProperty(RDF.type, typeRes);
        } else {
            testRes.addProperty(RDF.type, BdqFfdq.DataQualityNeed);
        }

        // Labels
        if (td.getLabel() != null && !td.getLabel().isEmpty()) {
            testRes.addProperty(RDFS.label, td.getLabel());
        }
        if (td.getPrefLabel() != null && !td.getPrefLabel().isEmpty()) {
            testRes.addProperty(SKOS.prefLabel, td.getPrefLabel());
        }

        // Information elements - ActedUpon
        for (String ie : td.getActedUponElements()) {
            Resource ieRes = buildIeResource(model, ie, InfoElementRole.ACTED_UPON);
            testRes.addProperty(BdqFfdq.hasInformationElement, ieRes);
        }
        // Information elements - Consulted
        for (String ie : td.getConsultedElements()) {
            Resource ieRes = buildIeResource(model, ie, InfoElementRole.CONSULTED);
            testRes.addProperty(BdqFfdq.hasInformationElement, ieRes);
        }
        // Legacy single informationElement field (backward compat)
        if (td.getActedUponElements().isEmpty() && td.getConsultedElements().isEmpty()) {
            if (td.getInformationElement() != null && !td.getInformationElement().isEmpty()) {
                Resource ieRes = buildIeResource(model, td.getInformationElement(), InfoElementRole.ACTED_UPON);
                testRes.addProperty(BdqFfdq.hasInformationElement, ieRes);
            }
        }

        // Dimension
        if (td.getDimension() != null && !td.getDimension().isEmpty()) {
            Resource dimRes = resolveVocabTerm(model, td.getDimension(), BDQDIM_NS);
            testRes.addProperty(model.createProperty(BdqFfdq.NS, "hasDimension"), dimRes);
        }

        // Criterion / enhancement
        if (td.getCriterionOrEnhancement() != null
                && !td.getCriterionOrEnhancement().isEmpty()) {
            boolean isAmendment = (td.getType() == TestType.AMENDMENT);
            String predicate = isAmendment ? "hasEnhancement" : "hasCriterion";
            String defaultNs  = isAmendment ? BDQENH_NS : BDQCRIT_NS;
            Resource vocabRes = resolveVocabTerm(model, td.getCriterionOrEnhancement(), defaultNs);
            testRes.addProperty(model.createProperty(BdqFfdq.NS, predicate), vocabRes);
        }

        // Expected response / specification
        buildSpecification(model, testRes, td);

        // Notes
        if (td.getNotes() != null && !td.getNotes().isEmpty()) {
            testRes.addProperty(RDFS.comment, td.getNotes());
        }

        return testRes;
    }

    private Resource buildIeResource(Model model, String qname, InfoElementRole role) {
        // Construct a simple URI for the information element node
        String ieIri = DEFAULT_BASE + "ie_"
                + UUID.nameUUIDFromBytes(("ie:" + qname).getBytes(StandardCharsets.UTF_8));
        Resource ieRes = model.createResource(ieIri);
        ieRes.addProperty(RDF.type,
                model.createResource(BdqFfdq.NS + "InformationElement"));
        ieRes.addProperty(RDFS.label, qname);
        if (role != null) {
            String roleClass = (role == InfoElementRole.ACTED_UPON)
                    ? "ActedUpon" : "Consulted";
            ieRes.addProperty(RDF.type,
                    model.createResource(BdqFfdq.NS + roleClass));
        }
        return ieRes;
    }

    private void buildSpecification(Model model, Resource testRes, TestDraft td) {
        // Build specification text from clauses or fall back to free text
        String specText;
        if (!td.getExpectedResponseClauses().isEmpty()) {
            specText = buildSpecificationText(td.getExpectedResponseClauses());
        } else if (td.getExpectedResponse() != null && !td.getExpectedResponse().isEmpty()) {
            specText = td.getExpectedResponse();
        } else {
            return;
        }

        String specIri = DEFAULT_BASE + "spec_"
                + UUID.nameUUIDFromBytes(("spec:" + specText).getBytes(StandardCharsets.UTF_8));
        Resource specRes = model.createResource(specIri);
        specRes.addProperty(RDF.type,
                model.createResource(BdqFfdq.NS + "Specification"));
        specRes.addProperty(RDFS.label, specText);
        specRes.addProperty(
                model.createProperty(BdqFfdq.NS, "hasExpectedResponse"), specText);
        testRes.addProperty(BdqFfdq.hasSpecification, specRes);
    }

    private String buildSpecificationText(List<ExpectedResponseClause> clauses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(clauses.get(i).toString());
            sb.append('.');
        }
        return sb.toString();
    }

    private Resource testTypeResource(String typeName) {
        if (typeName == null) {
            return null;
        }
        switch (typeName.toUpperCase()) {
            case "VALIDATION": return BdqFfdq.Validation;
            case "MEASURE":    return BdqFfdq.Measure;
            case "AMENDMENT":  return BdqFfdq.Amendment;
            case "ISSUE":      return BdqFfdq.Issue;
            default:           return BdqFfdq.DataQualityNeed;
        }
    }

    private static String sanitizeIri(String name, String fallbackSuffix) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_BASE + UUID.randomUUID() + "_" + fallbackSuffix;
        }
        String safe = name.trim()
                .replaceAll("[^A-Za-z0-9_\\-]", "_")
                .replaceAll("_+", "_");
        return DEFAULT_BASE + safe;
    }

    /**
     * Resolves a vocabulary term string (e.g. {@code "Completeness"},
     * {@code "bdqdim:Completeness"}, or a full URI) to an RDF {@link Resource}
     * using the supplied default namespace as the fallback when the term has no
     * namespace qualifier.
     *
     * <p>Resolution rules (in order):</p>
     * <ol>
     *   <li>If the value contains {@code "://"} it is treated as a full URI.</li>
     *   <li>If the value contains {@code ":"} it is split into prefix + local name;
     *       the prefix is looked up in the model's registered namespace map.</li>
     *   <li>Otherwise the {@code defaultNs} is prepended to the local name.</li>
     * </ol>
     *
     * @param model     the Jena model (used for prefix lookup)
     * @param term      the term string; never {@code null}
     * @param defaultNs the namespace to use when no prefix is present
     * @return an RDF resource for the resolved URI
     */
    static Resource resolveVocabTerm(Model model, String term, String defaultNs) {
        String t = term.trim();
        // Full URI
        if (t.contains("://")) {
            return model.createResource(t);
        }
        // Prefixed name: split on first ':'
        int colon = t.indexOf(':');
        if (colon > 0) {
            String prefix = t.substring(0, colon);
            String localName = t.substring(colon + 1);
            String ns = model.getNsPrefixURI(prefix);
            if (ns != null) {
                return model.createResource(ns + localName);
            }
            // Unknown prefix – fall through to default
        }
        // Local name only: apply default namespace
        return model.createResource(defaultNs + t);
    }
}
