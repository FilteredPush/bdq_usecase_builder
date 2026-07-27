package org.filteredpush.bdq.usecasebuilder.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.filteredpush.bdq.usecasebuilder.model.AuthorityDefault;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
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
                "<p>First requirement</p>\nSecond requirement <b>with formatting</b>\n<li>Third</li>");

        Model model = service.buildModel(state, false, null);
        Resource useCase = model.listSubjectsWithProperty(RDF.type, BdqFfdq.UseCase).next();
        String value = model.getProperty(useCase, BdqFfdq.hasFitnessRequirements).getObject().asLiteral().getString();

        assertTrue(value.contains("<ul>") && value.contains("<li>"));
        assertFalse(value.contains("\n") || value.contains("\r"), "hasFitnessRequirements must be single-line");
        assertFalse(value.matches("(?is).*</?(?!ul|li)[a-z][^>]*>.*"),
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
