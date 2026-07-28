package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.filteredpush.bdq.usecasebuilder.catalog.TestCatalogService;
import org.filteredpush.bdq.usecasebuilder.model.AuthorityDefault;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exports a {@link ProjectState} as RDF/Turtle conforming to the bdqffdq
 * ontology.
 */
public class TurtleExportService {

    private static final Logger logger = LoggerFactory.getLogger(TurtleExportService.class);

    private static final String URN_UUID_PREFIX = "urn:uuid:";

    /** Namespace for bdqdim (data quality dimension) terms. */
    static final String BDQDIM_NS = "https://rs.tdwg.org/bdqdim/terms/";

    /** Namespace for bdqcrit (data quality criterion) terms. */
    static final String BDQCRIT_NS = "https://rs.tdwg.org/bdqcrit/terms/";

    /** Namespace for bdqenh (data quality enhancement) terms. */
    static final String BDQENH_NS = "https://rs.tdwg.org/bdqenh/terms/";

    /** Namespace for default information-element fallback. */
    static final String DWC_NS = "http://rs.tdwg.org/dwc/terms/";
    /**
     * Splits plain-text fitness requirements into list items when no explicit
     * {@code <li>} tags are present.
     */
    private static final Pattern FITNESS_SPLIT_PATTERN =
            Pattern.compile("(?:\\n+|\\s*[;•]+\\s*|\\s+-\\s+)");
    private static final Pattern LI_TAG_PATTERN =
            Pattern.compile("(?is)<li[^>]*>(.*?)</li>");
    private static final int MAX_SPECIFICATION_EXAMPLES = 2;

    private static final Set<String> ALLOWED_BDQ_PROPERTIES = Set.of(
            BdqFfdq.hasUseCase.getURI(),
            BdqFfdq.includedInPolicy.getURI(),
            BdqFfdq.hasFitnessRequirements.getURI(),
            BdqFfdq.hasActedUponInformationElement.getURI(),
            BdqFfdq.hasConsultedInformationElement.getURI(),
            BdqFfdq.composedOf.getURI(),
            BdqFfdq.hasDataQualityDimension.getURI(),
            BdqFfdq.hasCriterion.getURI(),
            BdqFfdq.hasEnhancement.getURI(),
            BdqFfdq.forValidation.getURI(),
            BdqFfdq.forIssue.getURI(),
            BdqFfdq.forMeasurement.getURI(),
            BdqFfdq.forAmendment.getURI(),
            BdqFfdq.hasSpecification.getURI(),
            BdqFfdq.hasExpectedResponse.getURI(),
            BdqFfdq.hasAuthoritiesDefaults.getURI()
    );

    private static final Set<String> ALLOWED_BDQ_CLASSES = Set.of(
            BdqFfdq.UseCase.getURI(),
            BdqFfdq.Policy.getURI(),
            BdqFfdq.DataQualityNeed.getURI(),
            BdqFfdq.Validation.getURI(),
            BdqFfdq.Measure.getURI(),
            BdqFfdq.Amendment.getURI(),
            BdqFfdq.Issue.getURI(),
            BdqFfdq.ValidationMethod.getURI(),
            BdqFfdq.IssueMethod.getURI(),
            BdqFfdq.MeasurementMethod.getURI(),
            BdqFfdq.AmendmentMethod.getURI(),
            BdqFfdq.Specification.getURI(),
            BdqFfdq.InformationElement.getURI(),
            BdqFfdq.ActedUpon.getURI(),
            BdqFfdq.Consulted.getURI()
    );

    public File exportMinimal(ProjectState state, File outputDir) throws IOException {
        return export(state, outputDir, false, null);
    }

    public File exportWithExisting(ProjectState state, File outputDir,
            TestCatalogService catalogService) throws IOException {
        return export(state, outputDir, true, catalogService);
    }

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

    Model buildModel(ProjectState state, boolean includeExisting,
            TestCatalogService catalogService) {
        Model model = ModelFactory.createDefaultModel();

        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("bdqffdq", BdqFfdq.NS);
        model.setNsPrefix("bdqtest", "https://rs.tdwg.org/bdqtest/terms/");
        model.setNsPrefix("bdqdim", BDQDIM_NS);
        model.setNsPrefix("bdqcrit", BDQCRIT_NS);
        model.setNsPrefix("bdqenh", BDQENH_NS);
        model.setNsPrefix("dwc", DWC_NS);

        UseCaseDraft uc = state.getUseCaseDraft();

        String ucIri = stableUrnUuid("usecase:" + normalizeSeed(uc.getName()));
        Resource ucRes = model.createResource(ucIri);
        addBdqType(ucRes, BdqFfdq.UseCase);
        if (!isBlank(uc.getName())) {
            ucRes.addProperty(RDFS.label, uc.getName().trim());
            ucRes.addProperty(SKOS.prefLabel, uc.getName().trim());
        }
        if (!isBlank(uc.getDescription())) {
            ucRes.addProperty(RDFS.comment, uc.getDescription().trim());
            ucRes.addProperty(SKOS.definition, uc.getDescription().trim());
        }
        String fitnessRequirements = normalizeFitnessRequirements(uc.getFitnessRequirementsText());
        if (!isBlank(fitnessRequirements)) {
            addBdqLiteral(ucRes, BdqFfdq.hasFitnessRequirements, fitnessRequirements);
        }
        if (!isBlank(uc.getScopeNote())) {
            ucRes.addProperty(SKOS.scopeNote, uc.getScopeNote().trim());
        }

        String policyIri = stableUrnUuid("policy:" + ucIri);
        Resource policyRes = model.createResource(policyIri);
        addBdqType(policyRes, BdqFfdq.Policy);
        addBdqResource(policyRes, BdqFfdq.hasUseCase, ucRes);

        for (TestDraft td : state.getNewTestDrafts()) {
            Resource testRes = buildTestResource(model, td);
            addBdqResource(policyRes, BdqFfdq.includedInPolicy, testRes);
        }

        if (!state.getSelectedExistingTestIris().isEmpty()) {
            for (String iri : state.getSelectedExistingTestIris()) {
                Resource existingTest = model.createResource(iri);
                if (includeExisting && catalogService != null) {
                    catalogService.getEntries().stream()
                            .filter(e -> iri.equals(e.getIri()))
                            .findFirst()
                            .ifPresent(entry -> {
                                if (!isBlank(entry.getLabel())) {
                                    existingTest.addProperty(RDFS.label, entry.getLabel().trim());
                                }
                                if (!isBlank(entry.getType())) {
                                    addBdqType(existingTest, testTypeResource(entry.getType()));
                                }
                            });
                } else if (includeExisting) {
                    addBdqType(existingTest, BdqFfdq.DataQualityNeed);
                }
                addBdqResource(policyRes, BdqFfdq.includedInPolicy, existingTest);
            }
        }

        return model;
    }

    private Resource buildTestResource(Model model, TestDraft td) {
        String testSeed = "test:"
                + normalizeSeed(td.getType() != null ? td.getType().name() : "")
                + ":" + normalizeSeed(td.getLabel())
                + ":" + normalizeSeed(td.getPrefLabel())
                + ":" + String.join("|", td.getAllInformationElements());
        Resource testRes = model.createResource(stableUrnUuid(testSeed));

        Resource typeRes = testTypeResource(td.getType() != null ? td.getType().name() : null);
        addBdqType(testRes, typeRes != null ? typeRes : BdqFfdq.DataQualityNeed);

        if (!isBlank(td.getLabel())) {
            testRes.addProperty(RDFS.label, td.getLabel().trim());
        }
        if (!isBlank(td.getPrefLabel())) {
            testRes.addProperty(SKOS.prefLabel, td.getPrefLabel().trim());
        }

        List<String> actedUpon = new ArrayList<>(td.getActedUponElements());
        if (actedUpon.isEmpty() && td.getConsultedElements().isEmpty() && !isBlank(td.getInformationElement())) {
            actedUpon.add(td.getInformationElement().trim());
        }
        if (!actedUpon.isEmpty()) {
            Resource actedNode = buildIeNode(model, actedUpon, InfoElementRole.ACTED_UPON);
            addBdqResource(testRes, BdqFfdq.hasActedUponInformationElement, actedNode);
        }
        if (!td.getConsultedElements().isEmpty()) {
            Resource consultedNode = buildIeNode(model, td.getConsultedElements(), InfoElementRole.CONSULTED);
            addBdqResource(testRes, BdqFfdq.hasConsultedInformationElement, consultedNode);
        }

        if (!isBlank(td.getDimension())) {
            Resource dimRes = resolveVocabTerm(model, td.getDimension(), BDQDIM_NS);
            addBdqResource(testRes, BdqFfdq.hasDataQualityDimension, dimRes);
        }

        if (!isBlank(td.getCriterionOrEnhancement())) {
            boolean isAmendment = td.getType() == TestType.AMENDMENT;
            Resource vocabRes = resolveVocabTerm(model, td.getCriterionOrEnhancement(),
                    isAmendment ? BDQENH_NS : BDQCRIT_NS);
            addBdqResource(testRes,
                    isAmendment ? BdqFfdq.hasEnhancement : BdqFfdq.hasCriterion,
                    vocabRes);
        }

        buildMethodAndSpecification(model, testRes, td);

        if (!isBlank(td.getNotes())) {
            testRes.addProperty(RDFS.comment, td.getNotes().trim());
        }

        return testRes;
    }

    private Resource buildIeNode(Model model, List<String> terms, InfoElementRole role) {
        List<String> cleaned = new ArrayList<>();
        for (String term : terms) {
            if (!isBlank(term)) {
                cleaned.add(term.trim());
            }
        }
        String seed = (role == InfoElementRole.ACTED_UPON ? "acted:" : "consulted:")
                + String.join("|", cleaned);
        Resource ieRes = model.createResource(stableUrnUuid(seed));
        addBdqType(ieRes, BdqFfdq.InformationElement);
        addBdqType(ieRes, role == InfoElementRole.ACTED_UPON ? BdqFfdq.ActedUpon : BdqFfdq.Consulted);
        ieRes.addProperty(RDFS.label, String.join(", ", cleaned));
        for (String term : cleaned) {
            Resource termRes = resolveVocabTerm(model, term, DWC_NS);
            addBdqResource(ieRes, BdqFfdq.composedOf, termRes);
        }
        return ieRes;
    }

    private void buildMethodAndSpecification(Model model, Resource testRes, TestDraft td) {
        String expectedResponse = buildSpecificationText(td.getExpectedResponseClauses());
        if (isBlank(expectedResponse)) {
            expectedResponse = isBlank(td.getExpectedResponse()) ? null : td.getExpectedResponse().trim();
        }
        if (isBlank(expectedResponse)) {
            return;
        }

        String specSeed = "spec:" + expectedResponse + ":" + normalizeSeed(td.getLabel());
        Resource specRes = model.createResource(stableUrnUuid(specSeed));
        addBdqType(specRes, BdqFfdq.Specification);
        specRes.addProperty(RDFS.label, specificationLabel(td));
        addBdqLiteral(specRes, BdqFfdq.hasExpectedResponse, expectedResponse);

        String authoritiesDefaults = buildAuthoritiesDefaultsText(td);
        if (!isBlank(authoritiesDefaults)) {
            addBdqLiteral(specRes, BdqFfdq.hasAuthoritiesDefaults, authoritiesDefaults);
        }
        String description = buildSpecificationDescription(td, expectedResponse, authoritiesDefaults);
        if (!isBlank(description)) {
            specRes.addProperty(DCTerms.description, description);
        }
        for (String example : buildSpecificationExamples(td, MAX_SPECIFICATION_EXAMPLES)) {
            specRes.addProperty(SKOS.example, example);
        }

        Resource methodRes = model.createResource(stableUrnUuid("method:" + specSeed));
        addBdqType(methodRes, methodTypeResource(td.getType()));
        String methodLabel = methodLabel(td);
        methodRes.addProperty(RDFS.label, methodLabel);
        methodRes.addProperty(SKOS.prefLabel, methodLabel);
        addBdqResource(methodRes, forNeedProperty(td.getType()), testRes);
        addBdqResource(methodRes, BdqFfdq.hasSpecification, specRes);
    }

    private String buildSpecificationDescription(
            TestDraft td,
            String expectedResponse,
            String authoritiesDefaults) {
        List<String> parts = new ArrayList<>();
        if (!isBlank(expectedResponse)) {
            parts.add(expectedResponse.trim());
        }
        if (!isBlank(authoritiesDefaults)) {
            parts.add(authoritiesDefaults.trim());
        }
        String parameters = buildParametersText(td);
        if (!isBlank(parameters)) {
            parts.add(parameters.trim());
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private String buildParametersText(TestDraft td) {
        List<String> chunks = new ArrayList<>();
        if (!isBlank(td.getParameterDefaults())) {
            chunks.add(td.getParameterDefaults().trim());
        }
        td.getParameterDefinitions().forEach(parameter -> {
            if (parameter == null) {
                return;
            }
            List<String> parts = new ArrayList<>();
            if (!isBlank(parameter.getName())) {
                parts.add(parameter.getName().trim());
            }
            if (!isBlank(parameter.getDatatype())) {
                parts.add("datatype=" + parameter.getDatatype().trim());
            }
            if (!isBlank(parameter.getDefaultAuthorityIdentifier())) {
                parts.add("defaultAuthority=" + parameter.getDefaultAuthorityIdentifier().trim());
            }
            if (!isBlank(parameter.getNotes())) {
                parts.add(parameter.getNotes().trim());
            }
            if (!parts.isEmpty()) {
                chunks.add(String.join(" ", parts));
            }
        });
        return chunks.isEmpty() ? null : String.join(" ; ", chunks);
    }

    /**
     * Builds up to {@code maxExamples} SKOS examples from conformance rows.
     *
     * <p>Only rows with {@code Response.status=RUN_HAS_RESULT} are included.
     * Each example is serialized as:
     * {@code input1="value1", input2="value2": Response.status=..., Response.result=..., Response.comment="..."}.
     * Rows are skipped when they have no non-response input values after filtering.</p>
     */
    private List<String> buildSpecificationExamples(TestDraft td, int maxExamples) {
        List<String> examples = new ArrayList<>();
        for (var row : td.getConformanceRows()) {
            if (row == null || row.getValues() == null) {
                continue;
            }
            Map<String, String> values = row.getValues();
            String status = values.get("Response.status");
            if (!"RUN_HAS_RESULT".equals(collapseWhitespace(status))) {
                continue;
            }

            List<String> inputs = new ArrayList<>();
            values.forEach((key, value) -> {
                if (isBlank(value)
                        || "Label".equals(key)
                        || "Response.status".equals(key)
                        || "Response.result".equals(key)
                        || "Response.comment".equals(key)) {
                    return;
                }
                StringBuilder input = new StringBuilder();
                input.append(key).append("=\"")
                        .append(escapeQuotes(collapseWhitespace(value)))
                        .append('"');
                inputs.add(input.toString());
            });
            if (inputs.isEmpty()) {
                continue;
            }

            StringBuilder sb = new StringBuilder(String.join(", ", inputs))
                    .append(": Response.status=RUN_HAS_RESULT");
            if (!isBlank(values.get("Response.result"))) {
                sb.append(", Response.result=")
                        .append(collapseWhitespace(values.get("Response.result")));
            }
            if (!isBlank(values.get("Response.comment"))) {
                sb.append(", Response.comment=\"")
                        .append(escapeQuotes(collapseWhitespace(values.get("Response.comment"))))
                        .append("\"");
            }
            examples.add(sb.toString());
            if (examples.size() >= maxExamples) {
                break;
            }
        }
        return examples;
    }

    private String buildAuthoritiesDefaultsText(TestDraft td) {
        List<String> chunks = new ArrayList<>();
        for (AuthorityDefault authority : td.getAuthorityDefaults()) {
            if (authority == null) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            if (!isBlank(authority.getIdentifier())) {
                parts.add(authority.getIdentifier().trim());
            }
            if (!isBlank(authority.getAuthorityUri())) {
                parts.add(authority.getAuthorityUri().trim());
            }
            if (!isBlank(authority.getApiLabel())) {
                parts.add("apiLabel=" + authority.getApiLabel().trim());
            }
            if (!isBlank(authority.getApiEndpoint())) {
                parts.add("apiEndpoint=" + authority.getApiEndpoint().trim());
            }
            if (!isBlank(authority.getRegexPattern())) {
                parts.add("regex=" + authority.getRegexPattern().trim());
            }
            if (!parts.isEmpty()) {
                chunks.add(String.join(" | ", parts));
            }
        }
        if (!chunks.isEmpty()) {
            return String.join(" ; ", chunks);
        }
        return isBlank(td.getParameterDefaults()) ? null : td.getParameterDefaults().trim();
    }

    private static String normalizeFitnessRequirements(String raw) {
        if (isBlank(raw)) {
            return null;
        }

        String source = raw.replace("\r", "\n");
        List<String> items = new ArrayList<>();
        String leadingText = "";
        int firstLiStart = -1;

        Matcher liMatcher = LI_TAG_PATTERN.matcher(source);
        while (liMatcher.find()) {
            if (firstLiStart < 0) {
                firstLiStart = liMatcher.start();
            }
            String text = stripTags(liMatcher.group(1));
            if (!isBlank(text)) {
                items.add(text);
            }
        }

        if (items.isEmpty()) {
            String noTags = stripTags(source);
            // Split plain text into list items by newlines, bullets, semicolons, or hyphens.
            for (String part : FITNESS_SPLIT_PATTERN.split(noTags, -1)) {
                String cleaned = collapseWhitespace(part);
                if (!isBlank(cleaned)) {
                    items.add(cleaned);
                }
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        if (firstLiStart >= 0) {
            leadingText = stripTags(source.substring(0, firstLiStart));
        }

        StringBuilder sb = new StringBuilder();
        if (!isBlank(leadingText)) {
            sb.append(collapseWhitespace(leadingText));
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append("<ul>");
        for (String item : items) {
            sb.append("<li>").append(collapseWhitespace(item)).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString().replace("\n", " ").replace("\r", "").trim();
    }

    private String testDisplayLabel(TestDraft td) {
        if (!isBlank(td.getLabel())) {
            return td.getLabel().trim();
        }
        if (!isBlank(td.getPrefLabel())) {
            return td.getPrefLabel().trim();
        }
        return "Unlabeled test";
    }

    private static String stripTags(String value) {
        if (value == null) {
            return "";
        }
        return collapseWhitespace(value.replaceAll("(?is)<[^>]+>", " "));
    }

    private String buildSpecificationText(List<ExpectedResponseClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ExpectedResponseClause clause : clauses) {
            if (clause == null) {
                continue;
            }
            String clauseText = clause.toString();
            if (isBlank(clauseText)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(clauseText.trim());
            sb.append('.');
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Resource methodTypeResource(TestType type) {
        if (type == null) {
            return BdqFfdq.DataQualityMethod;
        }
        switch (type) {
            case VALIDATION:
                return BdqFfdq.ValidationMethod;
            case ISSUE:
                return BdqFfdq.IssueMethod;
            case AMENDMENT:
                return BdqFfdq.AmendmentMethod;
            case MEASURE:
            default:
                return BdqFfdq.MeasurementMethod;
        }
    }

    private Property forNeedProperty(TestType type) {
        if (type == null) {
            return BdqFfdq.forValidation;
        }
        switch (type) {
            case VALIDATION:
                return BdqFfdq.forValidation;
            case ISSUE:
                return BdqFfdq.forIssue;
            case AMENDMENT:
                return BdqFfdq.forAmendment;
            case MEASURE:
            default:
                return BdqFfdq.forMeasurement;
        }
    }

    private Resource testTypeResource(String typeName) {
        if (typeName == null) {
            return null;
        }
        switch (typeName.toUpperCase()) {
            case "VALIDATION":
                return BdqFfdq.Validation;
            case "MEASURE":
                return BdqFfdq.Measure;
            case "AMENDMENT":
                return BdqFfdq.Amendment;
            case "ISSUE":
                return BdqFfdq.Issue;
            default:
                return BdqFfdq.DataQualityNeed;
        }
    }

    private void addBdqType(Resource subject, Resource bdqClass) {
        requireAllowedClass(bdqClass);
        subject.addProperty(RDF.type, bdqClass);
    }

    private void addBdqResource(Resource subject, Property property, Resource object) {
        requireAllowedProperty(property);
        subject.addProperty(property, object);
    }

    private void addBdqLiteral(Resource subject, Property property, String value) {
        requireAllowedProperty(property);
        subject.addProperty(property, value);
    }

    private static void requireAllowedProperty(Property property) {
        if (property == null || !ALLOWED_BDQ_PROPERTIES.contains(property.getURI())) {
            throw new IllegalArgumentException("Disallowed bdqffdq property in export: "
                    + (property == null ? "null" : property.getURI()));
        }
    }

    private static void requireAllowedClass(Resource resource) {
        if (resource == null || !ALLOWED_BDQ_CLASSES.contains(resource.getURI())) {
            throw new IllegalArgumentException("Disallowed bdqffdq class in export: "
                    + (resource == null ? "null" : resource.getURI()));
        }
    }

    private static String stableUrnUuid(String seed) {
        UUID uuid = UUID.nameUUIDFromBytes(normalizeSeed(seed).getBytes(StandardCharsets.UTF_8));
        return URN_UUID_PREFIX + uuid;
    }

    static boolean isValidUrnUuid(String iri) {
        if (iri == null || !iri.startsWith(URN_UUID_PREFIX)) {
            return false;
        }
        try {
            UUID.fromString(iri.substring(URN_UUID_PREFIX.length()));
            return true;
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid urn:uuid identifier encountered: {}", iri);
            return false;
        }
    }

    private static String normalizeSeed(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String collapseWhitespace(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String escapeQuotes(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String specificationLabel(TestDraft td) {
        return "Specification for: " + testDisplayLabel(td);
    }

    private String methodLabel(TestDraft td) {
        return "Method for: " + testDisplayLabel(td);
    }

    /**
     * Resolves a vocabulary term string (e.g. {@code "Completeness"},
     * {@code "bdqdim:Completeness"}, or a full URI) to an RDF {@link Resource}.
     */
    static Resource resolveVocabTerm(Model model, String term, String defaultNs) {
        String t = term.trim();
        if (t.contains("://")) {
            return model.createResource(t);
        }
        int colon = t.indexOf(':');
        if (colon > 0) {
            String prefix = t.substring(0, colon);
            String localName = t.substring(colon + 1);
            String ns = model.getNsPrefixURI(prefix);
            if (ns != null) {
                return model.createResource(ns + localName);
            }
        }
        return model.createResource(defaultNs + t);
    }
}
