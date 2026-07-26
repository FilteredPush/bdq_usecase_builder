package org.filteredpush.bdq.usecasebuilder.model;

/**
 * A draft definition for a BDQ use case being authored by the user.
 *
 * <p>Captures the name, description, and fitness-for-use requirements narrative
 * for the use case.</p>
 */
public class UseCaseDraft {

    private String name;
    private String description;
    private String fitnessRequirementsText;

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
     * that motivated this use case (the {@code hasFitnessForUsePurpose} text).
     */
    public String getFitnessRequirementsText() {
        return fitnessRequirementsText;
    }

    /** Sets the fitness-for-use requirements narrative. */
    public void setFitnessRequirementsText(String fitnessRequirementsText) {
        this.fitnessRequirementsText = fitnessRequirementsText;
    }

    @Override
    public String toString() {
        return (name != null && !name.isEmpty()) ? name : "(unnamed use case)";
    }
}
