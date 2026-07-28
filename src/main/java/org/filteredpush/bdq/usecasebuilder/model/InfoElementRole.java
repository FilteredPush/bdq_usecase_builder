package org.filteredpush.bdq.usecasebuilder.model;

/**
 * The role of an information element within a BDQ test.
 *
 * <p>In bdqffdq, a test operates on information elements. Each element is
 * either directly evaluated/modified (ActedUpon) or used as context/reference
 * without being modified (Consulted).</p>
 */
public enum InfoElementRole {

    /** The information element is directly acted upon by the test. */
    ACTED_UPON("ActedUpon"),

    /** The information element is consulted for context but not modified. */
    CONSULTED("Consulted");

    private final String displayName;

    InfoElementRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this role.
     *
     * @return the display name (e.g., {@code "ActedUpon"} or {@code "Consulted"})
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
