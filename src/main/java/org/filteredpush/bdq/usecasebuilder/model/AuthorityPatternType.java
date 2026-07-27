package org.filteredpush.bdq.usecasebuilder.model;

/**
 * Supported conventions for representing source authorities/defaults.
 */
public enum AuthorityPatternType {
    URI_ONLY("URI-only"),
    URI_API("URI + API"),
    REGEX_BASED("Regex-based");

    private final String displayName;

    AuthorityPatternType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
