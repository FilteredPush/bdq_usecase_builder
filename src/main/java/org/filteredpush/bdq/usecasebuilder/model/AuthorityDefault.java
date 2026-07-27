package org.filteredpush.bdq.usecasebuilder.model;

/**
 * Structured authority/default descriptor for a test draft.
 */
public class AuthorityDefault {

    private String identifier;
    private AuthorityPatternType patternType = AuthorityPatternType.URI_ONLY;
    private String authorityUri;
    private String apiLabel;
    private String apiEndpoint;
    private String regexPattern;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public AuthorityPatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(AuthorityPatternType patternType) {
        this.patternType = patternType;
    }

    public String getAuthorityUri() {
        return authorityUri;
    }

    public void setAuthorityUri(String authorityUri) {
        this.authorityUri = authorityUri;
    }

    public String getApiLabel() {
        return apiLabel;
    }

    public void setApiLabel(String apiLabel) {
        this.apiLabel = apiLabel;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    @Override
    public String toString() {
        return identifier != null ? identifier : "(authority)";
    }
}
