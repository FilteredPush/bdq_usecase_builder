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
     * <p>When only type and information element are available (no criterion or dimension),
     * a partial label {@code TESTTYPE_INFORMATIONELEMENT} is returned so the user sees
     * an immediate suggestion as soon as the first IE is added.</p>
     *
     * <p>Returns {@code null} if insufficient information (type or IE missing) is available
     * to construct any suggestion.</p>
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

        if (typeToken == null || ieToken == null) {
            return null;
        }
        String criterionToken = criterionToken(draft);
        if (criterionToken == null) {
            // Return partial label without criterion
            return typeToken + "_" + ieToken;
        }
        return typeToken + "_" + ieToken + "_" + criterionToken;
    }

    /**
     * Generates a suggested human-readable preferred label (skos:prefLabel)
     * for the given draft.
     *
     * <p>The full pattern is: "{TypeName} {Information Element} {criterion}"
     * in title case, e.g. "Validation scientificName notEmpty". When criterion
     * is not yet set a partial suggestion "{TypeName} {Information Element}" is
     * returned so the user sees an immediate hint as soon as an IE is added.</p>
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

        if (typeName == null || ie == null) {
            return null;
        }
        String criterion = draft.getCriterionOrEnhancement();
        if (criterion == null || criterion.trim().isEmpty()) {
            // Return partial prefLabel without criterion
            return typeName + " " + ie;
        }
        return typeName + " " + ie + " " + criterion.trim();
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
