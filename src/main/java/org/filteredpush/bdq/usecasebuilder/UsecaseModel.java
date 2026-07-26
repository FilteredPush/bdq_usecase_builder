package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.filteredpush.bdq.usecasebuilder.vocab.BdqFfdq;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds the data for a BDQ use case gathered during the wizard session and
 * provides a method to convert that data into an Apache Jena {@link Model}
 * suitable for RDF serialization.
 *
 * <p>The resulting RDF follows the bdqffdq ontology pattern:</p>
 * <pre>
 *   &lt;usecase-IRI&gt; a bdqffdq:UseCase ;
 *       rdfs:label "…" ;
 *       rdfs:comment "…" .
 *
 *   &lt;urn:uuid:…&gt; a bdqffdq:Policy ;
 *       bdqffdq:hasUseCase &lt;usecase-IRI&gt; ;
 *       bdqffdq:includesInPolicy &lt;test-IRI&gt; .
 * </pre>
 *
 * <p>One {@code bdqffdq:Policy} resource is created for each test added to
 * the use case, as required by the bdqffdq ontology.</p>
 */
public class UsecaseModel {

    private String iri;
    private String label;
    private String description;
    private final List<String> testIris = new ArrayList<>();

    /** Creates an empty UsecaseModel (setters must be called before {@link #toModel()}). */
    public UsecaseModel() {
    }

    /**
     * Creates a UsecaseModel with the core metadata pre-populated.
     *
     * @param iri         the IRI of the use case
     * @param label       human-readable label
     * @param description free-text description
     */
    public UsecaseModel(String iri, String label, String description) {
        this.iri = iri;
        this.label = label;
        this.description = description;
    }

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    /**
     * Adds a test (data quality need) to this use case by its IRI.
     * Duplicate IRIs and {@code null}/empty values are silently ignored.
     *
     * @param testIri the IRI of the test ({@code bdqffdq:DataQualityNeed} subclass)
     */
    public void addTest(String testIri) {
        if (testIri != null && !testIri.isEmpty() && !testIris.contains(testIri)) {
            testIris.add(testIri);
        }
    }

    // -----------------------------------------------------------------------
    // RDF conversion
    // -----------------------------------------------------------------------

    /**
     * Converts this use case model to a Jena {@link Model}.
     *
     * <p>The model includes:</p>
     * <ul>
     *   <li>The use case resource typed as {@code bdqffdq:UseCase} with its
     *       label and description.</li>
     *   <li>One {@code bdqffdq:Policy} resource per associated test, linking
     *       the use case to that test via {@code bdqffdq:hasUseCase} and
     *       {@code bdqffdq:includesInPolicy}.</li>
     * </ul>
     *
     * @return a Jena Model ready for serialization
     */
    public Model toModel() {
        Model model = ModelFactory.createDefaultModel();

        // Register standard namespace prefixes for readable Turtle output
        model.setNsPrefix(BdqFfdq.PREFIX, BdqFfdq.NS);
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("skos", SKOS.getURI());

        // Create the use case resource
        Resource useCaseResource = model.createResource(iri);
        useCaseResource.addProperty(RDF.type, BdqFfdq.UseCase);
        if (label != null && !label.isEmpty()) {
            useCaseResource.addProperty(RDFS.label, label);
        }
        if (description != null && !description.isEmpty()) {
            useCaseResource.addProperty(RDFS.comment, description);
        }

        // Create one Policy per test, linking use case to test
        for (String testIri : testIris) {
            String policyIri = "urn:uuid:" + UUID.randomUUID();
            Resource policyResource = model.createResource(policyIri);
            policyResource.addProperty(RDF.type, BdqFfdq.Policy);
            policyResource.addProperty(BdqFfdq.hasUseCase, useCaseResource);
            policyResource.addProperty(BdqFfdq.includesInPolicy,
                    model.createResource(testIri));
        }

        return model;
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    /** Returns the IRI of the use case. */
    public String getIri() {
        return iri;
    }

    /** Sets the IRI of the use case. */
    public void setIri(String iri) {
        this.iri = iri;
    }

    /** Returns the human-readable label of the use case. */
    public String getLabel() {
        return label;
    }

    /** Sets the human-readable label. */
    public void setLabel(String label) {
        this.label = label;
    }

    /** Returns the free-text description of the use case. */
    public String getDescription() {
        return description;
    }

    /** Sets the free-text description. */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns a defensive copy of the list of test IRIs associated with this
     * use case.
     *
     * @return mutable copy; never {@code null}
     */
    public List<String> getTestIris() {
        return new ArrayList<>(testIris);
    }
}
