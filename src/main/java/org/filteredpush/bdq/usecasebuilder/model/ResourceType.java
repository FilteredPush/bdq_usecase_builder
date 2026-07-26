package org.filteredpush.bdq.usecasebuilder.model;

/**
 * The resource type a BDQ test operates on.
 *
 * <p>In bdqffdq, a test either evaluates a single record in isolation or
 * evaluates multiple records together (e.g., to check internal consistency
 * across a dataset).</p>
 */
public enum ResourceType {

    /** The test operates on a single occurrence/record in isolation. */
    SINGLE_RECORD("SingleRecord"),

    /** The test operates on multiple records taken together. */
    MULTI_RECORD("MultiRecord");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the human-readable display name for this resource type. */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
