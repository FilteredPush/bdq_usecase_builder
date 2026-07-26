package org.filteredpush.bdq.usecasebuilder.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary constants for the <em>bdqffdq</em> ontology.
 *
 * <p>The bdqffdq ontology (Biodiversity Data Quality Fitness-for-Use Framework
 * Ontology) defines the conceptual framework used to describe data quality
 * use cases, tests (data quality needs), methods, and implementations.</p>
 *
 * @see <a href="https://rs.tdwg.org/bdqffdq/terms/">bdqffdq vocabulary</a>
 * @see <a href="https://github.com/tdwg/bdq">TDWG BDQ repository</a>
 */
public final class BdqFfdq {

    /** The namespace URI for the bdqffdq vocabulary. */
    public static final String NS = "https://rs.tdwg.org/bdqffdq/terms/";

    /** Conventional namespace prefix for bdqffdq. */
    public static final String PREFIX = "bdqffdq";

    private BdqFfdq() {
        // Utility class; do not instantiate.
    }

    // -----------------------------------------------------------------------
    // Classes
    // -----------------------------------------------------------------------

    /**
     * {@code bdqffdq:UseCase} – a data quality use case that groups a set of
     * data quality needs (tests) needed for a particular purpose.
     */
    public static final Resource UseCase =
            ResourceFactory.createResource(NS + "UseCase");

    /**
     * {@code bdqffdq:Policy} – a policy that links a use case to the data
     * quality needs included in that use case.
     */
    public static final Resource Policy =
            ResourceFactory.createResource(NS + "Policy");

    /**
     * {@code bdqffdq:DataQualityNeed} – the abstract superclass for all data
     * quality test types (Validation, Measure, Amendment, Issue).
     */
    public static final Resource DataQualityNeed =
            ResourceFactory.createResource(NS + "DataQualityNeed");

    /**
     * {@code bdqffdq:Validation} – a data quality need that evaluates whether
     * a record meets a criterion for fitness for use.
     */
    public static final Resource Validation =
            ResourceFactory.createResource(NS + "Validation");

    /**
     * {@code bdqffdq:Measure} – a data quality need that measures an aspect of
     * the quality of a record.
     */
    public static final Resource Measure =
            ResourceFactory.createResource(NS + "Measure");

    /**
     * {@code bdqffdq:Amendment} – a data quality need that proposes a change
     * to improve the quality of a record.
     */
    public static final Resource Amendment =
            ResourceFactory.createResource(NS + "Amendment");

    /**
     * {@code bdqffdq:Issue} – a data quality need that flags a potential
     * problem in a record that may warrant human review.
     */
    public static final Resource Issue =
            ResourceFactory.createResource(NS + "Issue");

    /**
     * {@code bdqffdq:DataQualityReport} – a report that aggregates the results
     * of running data quality tests.
     */
    public static final Resource DataQualityReport =
            ResourceFactory.createResource(NS + "DataQualityReport");

    /**
     * {@code bdqffdq:DataQualityMethod} – a method that implements a data
     * quality need.
     */
    public static final Resource DataQualityMethod =
            ResourceFactory.createResource(NS + "DataQualityMethod");

    /**
     * {@code bdqffdq:Implementation} – a concrete implementation of a data
     * quality method.
     */
    public static final Resource Implementation =
            ResourceFactory.createResource(NS + "Implementation");

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    /**
     * {@code bdqffdq:hasUseCase} – links a policy to the use case it belongs
     * to.
     */
    public static final Property hasUseCase =
            ResourceFactory.createProperty(NS + "hasUseCase");

    /**
     * {@code bdqffdq:includesInPolicy} – links a policy to each data quality
     * need (test) it includes.
     */
    public static final Property includesInPolicy =
            ResourceFactory.createProperty(NS + "includesInPolicy");

    /**
     * {@code bdqffdq:hasSpecification} – links a data quality need to its
     * formal specification.
     */
    public static final Property hasSpecification =
            ResourceFactory.createProperty(NS + "hasSpecification");

    /**
     * {@code bdqffdq:hasInformationElement} – links a test to the information
     * element(s) it operates on.
     */
    public static final Property hasInformationElement =
            ResourceFactory.createProperty(NS + "hasInformationElement");

    /**
     * {@code bdqffdq:implementedBy} – links an implementation to the method it
     * implements.
     */
    public static final Property implementedBy =
            ResourceFactory.createProperty(NS + "implementedBy");
}
