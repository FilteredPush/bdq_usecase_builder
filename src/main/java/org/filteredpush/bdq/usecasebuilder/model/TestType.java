package org.filteredpush.bdq.usecasebuilder.model;

/**
 * The type of a BDQ data quality test (data quality need subclass).
 */
public enum TestType {

    /** Evaluates whether a record meets a criterion for fitness for use. */
    VALIDATION("Validation"),

    /** Measures an aspect of the quality of a record. */
    MEASURE("Measure"),

    /** Proposes a change to improve the quality of a record. */
    AMENDMENT("Amendment"),

    /** Flags a potential problem that may warrant human review. */
    ISSUE("Issue");

    private final String displayName;

    TestType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this test type.
     *
     * @return the display name (e.g., {@code "Validation"}, {@code "Amendment"})
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
