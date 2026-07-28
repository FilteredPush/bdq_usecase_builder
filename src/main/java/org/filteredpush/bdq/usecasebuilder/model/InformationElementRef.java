package org.filteredpush.bdq.usecasebuilder.model;

/**
 * A reference to a Darwin Core (or other vocabulary) information element used
 * within a BDQ test or use case.
 *
 * <p>Each reference captures the qualified name (e.g., {@code dwc:scientificName})
 * and the role the element plays in tests within the use case
 * ({@link InfoElementRole#ACTED_UPON} or {@link InfoElementRole#CONSULTED}).</p>
 */
public class InformationElementRef {

    private String qname;
    private InfoElementRole role;

    /**
     * Creates a new information element reference with the given qualified name
     * and role.
     *
     * @param qname the qualified name of the term (e.g., {@code dwc:scientificName})
     * @param role  the role this element plays ({@code ACTED_UPON} or {@code CONSULTED})
     */
    public InformationElementRef(String qname, InfoElementRole role) {
        this.qname = qname;
        this.role = role;
    }

    /**
     * Returns the qualified name of this information element.
     *
     * @return the qualified name (e.g., {@code dwc:scientificName})
     */
    public String getQname() {
        return qname;
    }

    /**
     * Sets the qualified name of this information element.
     *
     * @param qname the qualified name (e.g., {@code dwc:scientificName})
     */
    public void setQname(String qname) {
        this.qname = qname;
    }

    /**
     * Returns the role of this information element.
     *
     * @return the role ({@code ACTED_UPON} or {@code CONSULTED})
     */
    public InfoElementRole getRole() {
        return role;
    }

    /**
     * Sets the role of this information element.
     *
     * @param role the role ({@code ACTED_UPON} or {@code CONSULTED})
     */
    public void setRole(InfoElementRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return qname + " [" + (role != null ? role.getDisplayName() : "?") + "]";
    }
}
