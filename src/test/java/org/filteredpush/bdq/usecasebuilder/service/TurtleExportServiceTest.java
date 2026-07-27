package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.filteredpush.bdq.usecasebuilder.model.AuthorityDefault;
import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ParameterDefinition;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurtleExportServiceTest {

    private TurtleExportService service;

    @BeforeEach
    void setUp() {
        service = new TurtleExportService();
    }

    @Test
    void allUrnUuidIdentifiersAreValidUuids() {
        Model model = service.buildModel(buildSampleState(), false, null);

        List<String> urnUuids = new ArrayList<>();
        model.listStatements().forEachRemaining(stmt -> {
            Resource subject = stmt.getSubject();
            if (subject.isURIResource() && subject.getURI().startsWith("urn:uuid:")) {
                urnUuids.add(subject.getURI());
            }
            RDFNode object = stmt.getObject();
            if (object.isURIResource() && object.asResource().getURI().startsWith("urn:uuid:")) {
                urnUuids.add(object.asResource().getURI());
            }
        });

        assertFalse(urnUuids.isEmpty(), "Expected exporter to emit URN UUID identifiers");
        for (String iri : urnUuids) {
            assertTrue(TurtleExportService.isValidUrnUuid(iri), "Invalid urn:uuid emitted: " + iri);
        }
    }

    @Test
    void exporterOnlyEmitsWhitelistedBdqPredicates() {
        Model model = service.buildModel(buildSampleState(), false, null);

        Set<String> allowed = Set.of(
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

        model.listStatements().forEachRemaining(stmt -> {
            String predicateUri = stmt.getPredicate().getURI();
            if (predicateUri.startsWith(BdqFfdq.NS)) {
                assertTrue(allowed.contains(predicateUri), "Unexpected bdqffdq predicate: " + predicateUri);
            }
        });

        assertFalse(model.contains(null,
                model.createProperty(BdqFfdq.NS + "hasFitnessForUsePurpose"),
                (RDFNode) null));
        assertFalse(model.contains(null,
                model.createProperty(BdqFfdq.NS + "includesInPolicy"),
                (RDFNode) null));
        assertFalse(model.contains(null,
                model.createProperty(BdqFfdq.NS + "hasDimension"),
                (RDFNode) null));
        assertFalse(model.contains(null,
                model.createProperty(BdqFfdq.NS + "hasInformationElement"),
                (RDFNode) null));
    }

    @Test
    void needMethodSpecificationChainExistsForEachTestType() {
        assertNeedMethodSpecificationChain(TestType.VALIDATION, BdqFfdq.Validation,
                BdqFfdq.ValidationMethod, BdqFfdq.forValidation);
        assertNeedMethodSpecificationChain(TestType.ISSUE, BdqFfdq.Issue,
                BdqFfdq.IssueMethod, BdqFfdq.forIssue);
        assertNeedMethodSpecificationChain(TestType.MEASURE, BdqFfdq.Measure,
                BdqFfdq.MeasurementMethod, BdqFfdq.forMeasurement);
        assertNeedMethodSpecificationChain(TestType.AMENDMENT, BdqFfdq.Amendment,
                BdqFfdq.AmendmentMethod, BdqFfdq.forAmendment);
    }

    @Test
    void useCaseAndPolicyUseCorrectPredicates() {
        Model model = service.buildModel(buildSampleState(), false, null);

        Resource useCase = model.listSubjectsWithProperty(RDF.type, BdqFfdq.UseCase).next();
        Resource policy = model.listSubjectsWithProperty(RDF.type, BdqFfdq.Policy).next();

        assertTrue(model.contains(policy, BdqFfdq.hasUseCase, useCase));
        assertTrue(model.contains(policy, BdqFfdq.includedInPolicy, (RDFNode) null));
        assertTrue(model.contains(useCase, BdqFfdq.hasFitnessRequirements, (RDFNode) null));

        assertFalse(model.contains(policy,
                model.createProperty(BdqFfdq.NS + "includesInPolicy"),
                (RDFNode) null));
    }

    @Test
    void informationElementNodesUseComposedOfAndSupportMultipleTerms() {
        ProjectState state = buildSampleState();
        TestDraft draft = state.getNewTestDrafts().get(0);
        draft.addActedUponElement("dwc:kingdom");
        draft.addConsultedElement("dwc:taxonRank");

        Model model = service.buildModel(state, false, null);

        Resource testRes = model.listSubjectsWithProperty(RDF.type, BdqFfdq.Validation).next();
        Resource acted = model.listObjectsOfProperty(testRes, BdqFfdq.hasActedUponInformationElement)
                .next().asResource();
        Resource consulted = model.listObjectsOfProperty(testRes, BdqFfdq.hasConsultedInformationElement)
                .next().asResource();

        List<Statement> actedComposed = model.listStatements(acted, BdqFfdq.composedOf, (RDFNode) null).toList();
        List<Statement> consultedComposed = model.listStatements(consulted, BdqFfdq.composedOf, (RDFNode) null).toList();

        assertEquals(2, actedComposed.size(), "ActedUpon node should include both terms via composedOf");
        assertEquals(1, consultedComposed.size(), "Consulted node should include one term via composedOf");
    }

    @Test
    void hasFitnessRequirementsIsSingleLineUlLiOnly() {
        ProjectState state = buildSampleState();
        state.getUseCaseDraft().setFitnessRequirementsText(
                "Data are fit for use for HasSpecies if they have identifications to known species.\n"
                        + "\n<li>Taxon is known to an authority</li>\n"
                        + "<li>Taxon Rank is species or better</li>\n");

        Model model = service.buildModel(state, false, null);
        Resource useCase = model.listSubjectsWithProperty(RDF.type, BdqFfdq.UseCase).next();
        String value = model.getProperty(useCase, BdqFfdq.hasFitnessRequirements).getObject().asLiteral().getString();

        assertTrue(value.startsWith("Data are fit for use for HasSpecies if they have identifications to known species."),
                "Leading prose should be preserved as plain text");
        assertTrue(value.contains("<ul>") && value.contains("<li>"));
        assertFalse(value.contains("\n") || value.contains("\r"), "hasFitnessRequirements must be single-line");
        assertFalse(value.matches("(?is).*</?(?!ul\\b|li\\b)[a-z][^>]*>.*"),
                "Only ul/li tags are allowed: " + value);
    }

    @Test
    void authoritiesDefaultsIsOmittedWhenEmptyAndPresentWhenProvided() {
        ProjectState state = buildSampleState();
        Model model = service.buildModel(state, false, null);
        assertFalse(model.contains(null, BdqFfdq.hasAuthoritiesDefaults, (RDFNode) null));

        AuthorityDefault authority = new AuthorityDefault();
        authority.setIdentifier("gbif");
        authority.setAuthorityUri("https://api.gbif.org/v1/species/match");
        state.getNewTestDrafts().get(0).setAuthorityDefaults(List.of(authority));

        Model updated = service.buildModel(state, false, null);
        assertTrue(updated.contains(null, BdqFfdq.hasAuthoritiesDefaults, (RDFNode) null));
    }

    @Test
    void exportedTurtleFixtureUsesCorrectedGraphShape(@TempDir Path tempDir) throws Exception {
        File ttlFile = service.exportMinimal(buildSampleState(), tempDir.toFile());
        String ttl = Files.readString(ttlFile.toPath());

        assertTrue(ttl.contains("bdqffdq:hasFitnessRequirements"));
        assertTrue(ttl.contains("bdqffdq:includedInPolicy"));
        assertTrue(ttl.contains("bdqffdq:hasDataQualityDimension"));
        assertTrue(ttl.contains("bdqffdq:hasActedUponInformationElement"));
        assertTrue(ttl.contains("bdqffdq:forValidation"));
        assertTrue(ttl.contains("bdqffdq:hasSpecification"));
        assertTrue(ttl.contains("bdqffdq:hasExpectedResponse"));

        assertFalse(ttl.contains("hasFitnessForUsePurpose"));
        assertFalse(ttl.contains("includesInPolicy"));
        assertFalse(ttl.contains("hasDimension"));
        assertFalse(ttl.contains("hasInformationElement"));
    }

    @Test
    void specificationIncludesDescriptionExamplesAndMethodLabels() {
        ProjectState state = buildSampleState();
        TestDraft draft = state.getNewTestDrafts().get(0);
        draft.setExpectedResponse(
                "COMPLIANT if dwc:month is in range; NOT_COMPLIANT if dwc:month is ambiguous");
        draft.setExpectedResponseClauses(List.of());
        draft.setParameterDefaults("{bdqval:sourceAuthority = \"DCMI Type Vocabulary\"}");

        AuthorityDefault authority = new AuthorityDefault();
        authority.setIdentifier("bdqval:sourceAuthority");
        authority.setAuthorityUri("http://purl.org/dc/terms/DCMIType");
        draft.setAuthorityDefaults(List.of(authority));

        ParameterDefinition parameter = new ParameterDefinition();
        parameter.setName("bdqval:sourceAuthority");
        parameter.setDatatype("string");
        parameter.setNotes("DCMI Type Vocabulary List of Terms");
        draft.setParameterDefinitions(List.of(parameter));

        ConformanceRow row1 = new ConformanceRow();
        row1.put("dwc:month", "10");
        row1.put("Response.status", "RUN_HAS_RESULT");
        row1.put("Response.result", "COMPLIANT");
        row1.put("Response.comment", "dwc:month is in range");
        ConformanceRow row2 = new ConformanceRow();
        row2.put("dwc:month", "v");
        row2.put("Response.status", "RUN_HAS_RESULT");
        row2.put("Response.result", "NOT_COMPLIANT");
        row2.put("Response.comment", "dwc:month is ambiguous as \"v\" or \"5\"");
        ConformanceRow prereq = new ConformanceRow();
        prereq.put("dwc:month", "");
        prereq.put("Response.status", "INTERNAL_PREREQUISITES_NOT_MET");
        prereq.put("Response.result", "");
        prereq.put("Response.comment", "missing value");
        draft.setConformanceRows(List.of(prereq, row1, row2));

        Model model = service.buildModel(state, false, null);

        Resource method = model.listSubjectsWithProperty(BdqFfdq.hasSpecification).next();
        Resource spec = model.listObjectsOfProperty(method, BdqFfdq.hasSpecification).next().asResource();

        assertEquals("Specification for: " + draft.getLabel(),
                model.getProperty(spec, RDFS.label).getObject().asLiteral().getString());
        assertTrue(model.contains(method, RDFS.label, (RDFNode) null));
        assertTrue(model.contains(method, SKOS.prefLabel, (RDFNode) null));

        String description = model.getProperty(spec, DCTerms.description).getObject().asLiteral().getString();
        assertTrue(description.contains(draft.getExpectedResponse()));
        assertTrue(description.contains("bdqval:sourceAuthority"));
        assertTrue(description.contains("DCMI Type Vocabulary List of Terms"));

        List<RDFNode> examples = model.listObjectsOfProperty(spec, SKOS.example).toList();
        assertEquals(2, examples.size(), "Only rows with Response.status=RUN_HAS_RESULT should become examples");
        List<String> exampleTexts = examples.stream().map(x -> x.asLiteral().getString()).toList();
        assertTrue(exampleTexts.stream().anyMatch(x -> x.contains("dwc:month=\"10\"")));
        assertTrue(exampleTexts.stream().anyMatch(x -> x.contains("dwc:month is in range")));
        assertTrue(exampleTexts.stream().anyMatch(x -> x.contains("dwc:month=\"v\"")));
        assertTrue(exampleTexts.stream().anyMatch(x -> x.contains("dwc:month is ambiguous as \\\"v\\\" or \\\"5\\\"")));
        assertFalse(examples.stream().anyMatch(x -> x.asLiteral().getString().contains("PREREQUISITES_NOT_MET")));
    }

    private void assertNeedMethodSpecificationChain(
            TestType type,
            Resource needClass,
            Resource methodClass,
            Property forProperty) {
        ProjectState state = buildSampleState();
        TestDraft draft = state.getNewTestDrafts().get(0);
        draft.setType(type);
        draft.setLabel(type.name() + "_TEST");

        Model model = service.buildModel(state, false, null);

        Resource need = model.listSubjectsWithProperty(RDF.type, needClass).next();
        assertFalse(model.contains(need, BdqFfdq.hasSpecification, (RDFNode) null),
                "Need must not link directly to Specification");

        Resource method = model.listSubjectsWithProperty(RDF.type, methodClass).next();
        assertTrue(model.contains(method, forProperty, need), "Method must point to need via forX property");

        Resource spec = model.listObjectsOfProperty(method, BdqFfdq.hasSpecification).next().asResource();
        assertNotNull(spec);
        assertTrue(model.contains(spec, RDF.type, BdqFfdq.Specification));
        assertTrue(model.contains(spec, BdqFfdq.hasExpectedResponse, (RDFNode) null));
    }

    private ProjectState buildSampleState() {
        ProjectState state = new ProjectState();
        state.getUseCaseDraft().setName("Test Use Case");
        state.getUseCaseDraft().setDescription("A test description");
        state.getUseCaseDraft().setFitnessRequirementsText("- dwc:scientificName should be present");
        state.addInformationElement(new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));

        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_SCIENTIFICNAME_NOTEMPTY");
        draft.setPrefLabel("Scientific name not empty");
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setDimension("Completeness");
        draft.setCriterionOrEnhancement("NotEmpty");
        draft.setExpectedResponse("COMPLIANT if dwc:scientificName is not empty");

        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setCondition("dwc:scientificName is not empty");
        clause.setStatus("RUN_HAS_RESULT");
        clause.setResult("COMPLIANT");
        draft.setExpectedResponseClauses(List.of(clause));

        state.addNewTestDraft(draft);
        return state;
    }
}
