package org.filteredpush.bdq.usecasebuilder.model;

/**
 * Structured parameter descriptor for a test draft.
 */
public class ParameterDefinition {

    private String name;
    private String datatype;
    private String defaultAuthorityIdentifier;
    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDefaultAuthorityIdentifier() {
        return defaultAuthorityIdentifier;
    }

    public void setDefaultAuthorityIdentifier(String defaultAuthorityIdentifier) {
        this.defaultAuthorityIdentifier = defaultAuthorityIdentifier;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return name != null ? name : "(parameter)";
    }
}
