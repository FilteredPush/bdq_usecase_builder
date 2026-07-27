package org.filteredpush.bdq.usecasebuilder.model;

/**
 * A draft definition for a BDQ use case being authored by the user.
 *
 * <p>Captures the name, description, fitness-for-use requirements narrative,
 * and optional scope note for the use case.</p>
 */
public class UseCaseDraft {

    private String name;
    private String description;
    private String fitnessRequirementsText;
    private String scopeNote;

    /** Creates an empty use case draft. */
    public UseCaseDraft() {
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    /** Returns the name/label of the use case. */
    public String getName() {
        return name;
    }

    /** Sets the name/label of the use case. */
    public void setName(String name) {
        this.name = name;
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
     * Returns the narrative text describing the fitness-for-use requirements
     * that motivated this use case (the {@code hasFitnessRequirements} text).
     */
    public String getFitnessRequirementsText() {
        return fitnessRequirementsText;
    }

    /** Sets the fitness-for-use requirements narrative. */
    public void setFitnessRequirementsText(String fitnessRequirementsText) {
        this.fitnessRequirementsText = fitnessRequirementsText;
    }

    /**
     * Returns the optional scope note for this use case
     * (serialised as {@code skos:scopeNote} in RDF).
     */
    public String getScopeNote() {
        return scopeNote;
    }

    /** Sets the optional scope note. */
    public void setScopeNote(String scopeNote) {
        this.scopeNote = scopeNote;
    }

    @Override
    public String toString() {
        return (name != null && !name.isEmpty()) ? name : "(unnamed use case)";
    }
}
