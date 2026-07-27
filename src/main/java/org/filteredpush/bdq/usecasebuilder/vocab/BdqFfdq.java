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

    /** {@code bdqffdq:ValidationMethod}. */
    public static final Resource ValidationMethod =
            ResourceFactory.createResource(NS + "ValidationMethod");

    /** {@code bdqffdq:IssueMethod}. */
    public static final Resource IssueMethod =
            ResourceFactory.createResource(NS + "IssueMethod");

    /** {@code bdqffdq:MeasurementMethod}. */
    public static final Resource MeasurementMethod =
            ResourceFactory.createResource(NS + "MeasurementMethod");

    /** {@code bdqffdq:AmendmentMethod}. */
    public static final Resource AmendmentMethod =
            ResourceFactory.createResource(NS + "AmendmentMethod");

    /** {@code bdqffdq:Specification}. */
    public static final Resource Specification =
            ResourceFactory.createResource(NS + "Specification");

    /** {@code bdqffdq:InformationElement}. */
    public static final Resource InformationElement =
            ResourceFactory.createResource(NS + "InformationElement");

    /** {@code bdqffdq:ActedUpon}. */
    public static final Resource ActedUpon =
            ResourceFactory.createResource(NS + "ActedUpon");

    /** {@code bdqffdq:Consulted}. */
    public static final Resource Consulted =
            ResourceFactory.createResource(NS + "Consulted");

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
     * {@code bdqffdq:includedInPolicy} – links a policy to each data quality
     * need (test) it includes.
     */
    public static final Property includedInPolicy =
            ResourceFactory.createProperty(NS + "includedInPolicy");

    /**
     * {@code bdqffdq:hasSpecification} – links a data quality need to its
     * formal specification.
     */
    public static final Property hasSpecification =
            ResourceFactory.createProperty(NS + "hasSpecification");

    /**
     * {@code bdqffdq:hasActedUponInformationElement} – links a test to the
     * acted-upon information element node.
     */
    public static final Property hasActedUponInformationElement =
            ResourceFactory.createProperty(NS + "hasActedUponInformationElement");

    /**
     * {@code bdqffdq:hasConsultedInformationElement} – links a test to the
     * consulted information element node.
     */
    public static final Property hasConsultedInformationElement =
            ResourceFactory.createProperty(NS + "hasConsultedInformationElement");

    /** {@code bdqffdq:composedOf}. */
    public static final Property composedOf =
            ResourceFactory.createProperty(NS + "composedOf");

    /** {@code bdqffdq:hasDataQualityDimension}. */
    public static final Property hasDataQualityDimension =
            ResourceFactory.createProperty(NS + "hasDataQualityDimension");

    /** {@code bdqffdq:hasCriterion}. */
    public static final Property hasCriterion =
            ResourceFactory.createProperty(NS + "hasCriterion");

    /** {@code bdqffdq:hasEnhancement}. */
    public static final Property hasEnhancement =
            ResourceFactory.createProperty(NS + "hasEnhancement");

    /** {@code bdqffdq:forValidation}. */
    public static final Property forValidation =
            ResourceFactory.createProperty(NS + "forValidation");

    /** {@code bdqffdq:forIssue}. */
    public static final Property forIssue =
            ResourceFactory.createProperty(NS + "forIssue");

    /** {@code bdqffdq:forMeasurement}. */
    public static final Property forMeasurement =
            ResourceFactory.createProperty(NS + "forMeasurement");

    /** {@code bdqffdq:forAmendment}. */
    public static final Property forAmendment =
            ResourceFactory.createProperty(NS + "forAmendment");

    /** {@code bdqffdq:hasExpectedResponse}. */
    public static final Property hasExpectedResponse =
            ResourceFactory.createProperty(NS + "hasExpectedResponse");

    /** {@code bdqffdq:hasAuthoritiesDefaults}. */
    public static final Property hasAuthoritiesDefaults =
            ResourceFactory.createProperty(NS + "hasAuthoritiesDefaults");

    /**
     * {@code bdqffdq:implementedBy} – links an implementation to the method it
     * implements.
     */
    public static final Property implementedBy =
            ResourceFactory.createProperty(NS + "implementedBy");

    /**
     * {@code bdqffdq:hasFitnessRequirements} – describes the fitness-for-use
     * purpose of a use case (the narrative requirements that motivated the use case).
     */
    public static final Property hasFitnessRequirements =
            ResourceFactory.createProperty(NS + "hasFitnessRequirements");

    /** @deprecated Use {@link #includedInPolicy}. */
    @Deprecated
    public static final Property includesInPolicy = includedInPolicy;

    /** @deprecated Use {@link #hasFitnessRequirements}. */
    @Deprecated
    public static final Property hasFitnessForUsePurpose = hasFitnessRequirements;
}
