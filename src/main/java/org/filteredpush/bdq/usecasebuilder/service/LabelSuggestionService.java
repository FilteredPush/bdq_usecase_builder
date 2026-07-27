package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;

import java.util.List;

/**
 * Generates convention-consistent label and prefLabel suggestions for
 * {@link TestDraft} instances following the BDQ naming pattern.
 *
 * <p>The standard BDQ machine label follows the pattern:</p>
 * <pre>  TESTTYPE_INFORMATIONELEMENT_CRITERION</pre>
 * <p>e.g., {@code VALIDATION_SCIENTIFICNAME_NOTEMPTY}.</p>
 *
 * <p>Suggestions are generated from the current test type, the first ActedUpon
 * information element, and the criterion/dimension. They are only applied when
 * the user has not manually overridden the field (tracked by
 * {@link TestDraft#isLabelUserOverridden()} and
 * {@link TestDraft#isPrefLabelUserOverridden()}).</p>
 */
public class LabelSuggestionService {

    /**
     * Generates a suggested machine label for the given draft, following the
     * {@code TESTTYPE_INFORMATIONELEMENT_CRITERION} convention.
     *
     * <p>Returns {@code null} if insufficient information is available to
     * construct a meaningful suggestion.</p>
     *
     * @param draft the test draft to generate a suggestion for
     * @return suggested label string, or {@code null}
     */
    public String suggestLabel(TestDraft draft) {
        if (draft == null) {
            return null;
        }
        String typeToken = testTypeToken(draft.getType());
        String ieToken = firstIeToken(draft);
        String criterionToken = criterionToken(draft);

        if (typeToken == null || ieToken == null || criterionToken == null) {
            return null;
        }
        return typeToken + "_" + ieToken + "_" + criterionToken;
    }

    /**
     * Generates a suggested human-readable preferred label (skos:prefLabel)
     * for the given draft.
     *
     * <p>The pattern is: "{TypeName} {Information Element} {criterion}"
     * in title case, e.g. "Validation scientificName notEmpty".</p>
     *
     * @param draft the test draft to generate a suggestion for
     * @return suggested prefLabel string, or {@code null}
     */
    public String suggestPrefLabel(TestDraft draft) {
        if (draft == null) {
            return null;
        }
        String typeName = draft.getType() != null ? draft.getType().getDisplayName() : null;
        String ie = firstIeShortName(draft);
        String criterion = draft.getCriterionOrEnhancement();

        if (typeName == null || ie == null || criterion == null) {
            return null;
        }
        String criterionClean = criterion.trim();
        if (criterionClean.isEmpty()) {
            return null;
        }
        return typeName + " " + ie + " " + criterionClean;
    }

    /**
     * Applies auto-suggestions to the draft if the corresponding field has not
     * been manually overridden by the user.
     *
     * @param draft the draft to update; no-op if {@code null}
     */
    public void applyAutoSuggestions(TestDraft draft) {
        if (draft == null) {
            return;
        }
        if (!draft.isLabelUserOverridden()) {
            String suggested = suggestLabel(draft);
            if (suggested != null) {
                draft.setLabel(suggested);
            }
        }
        if (!draft.isPrefLabelUserOverridden()) {
            String suggested = suggestPrefLabel(draft);
            if (suggested != null) {
                draft.setPrefLabel(suggested);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String testTypeToken(TestType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case VALIDATION: return "VALIDATION";
            case MEASURE:    return "MEASURE";
            case AMENDMENT:  return "AMENDMENT";
            case ISSUE:      return "ISSUE";
            default:         return type.name().toUpperCase();
        }
    }

    private String firstIeToken(TestDraft draft) {
        // Prefer actedUponElements list; fall back to legacy informationElement
        String raw = firstIeRaw(draft);
        if (raw == null) {
            return null;
        }
        // Strip namespace prefix (e.g. "dwc:scientificName" → "SCIENTIFICNAME")
        String local = localPart(raw);
        return local.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String firstIeShortName(TestDraft draft) {
        String raw = firstIeRaw(draft);
        if (raw == null) {
            return null;
        }
        return localPart(raw);
    }

    private String firstIeRaw(TestDraft draft) {
        List<String> acted = draft.getActedUponElements();
        if (!acted.isEmpty()) {
            return acted.get(0);
        }
        String legacy = draft.getInformationElement();
        if (legacy != null && !legacy.trim().isEmpty()) {
            return legacy.trim();
        }
        List<String> consulted = draft.getConsultedElements();
        if (!consulted.isEmpty()) {
            return consulted.get(0);
        }
        return null;
    }

    private String criterionToken(TestDraft draft) {
        String c = draft.getCriterionOrEnhancement();
        if (c == null || c.trim().isEmpty()) {
            // Fall back to dimension
            String dim = draft.getDimension();
            if (dim == null || dim.trim().isEmpty()) {
                return null;
            }
            return dim.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        }
        return c.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private static String localPart(String qname) {
        // Handle "prefix:localName" or just "localName"
        int colon = qname.lastIndexOf(':');
        int hash = qname.lastIndexOf('#');
        int slash = qname.lastIndexOf('/');
        int sep = Math.max(colon, Math.max(hash, slash));
        if (sep >= 0 && sep < qname.length() - 1) {
            return qname.substring(sep + 1);
        }
        return qname;
    }
}
