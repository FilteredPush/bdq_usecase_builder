package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for Phase 3 – LabelSuggestionService.
 *
 * Covers requirement E3: A test supports multiple actedUpon/consulted IEs;
 * auto-suggested labels regenerate when upstream fields change unless user
 * override exists.
 */
public class LabelSuggestionServiceTest {

    private LabelSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new LabelSuggestionService();
    }

    // -----------------------------------------------------------------------
    // E3: Label suggestion from test type + IE + criterion
    // -----------------------------------------------------------------------

    @Test
    void testSuggestLabelBasicPattern() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setCriterionOrEnhancement("NotEmpty");

        String label = service.suggestLabel(draft);
        assertNotNull(label, "Label suggestion should not be null");
        assertTrue(label.startsWith("VALIDATION_"),
                "Label should start with VALIDATION_: " + label);
        assertTrue(label.contains("SCIENTIFICNAME"),
                "Label should contain SCIENTIFICNAME: " + label);
        assertTrue(label.contains("NOTEMPTY"),
                "Label should contain NOTEMPTY: " + label);
    }

    @Test
    void testSuggestLabelForAmendment() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.AMENDMENT);
        draft.addActedUponElement("dwc:country");
        draft.setCriterionOrEnhancement("Standardized");

        String label = service.suggestLabel(draft);
        assertNotNull(label);
        assertTrue(label.startsWith("AMENDMENT_"), "Should start with AMENDMENT_: " + label);
    }

    @Test
    void testSuggestLabelReturnsNullWithoutType() {
        TestDraft draft = new TestDraft();
        draft.addActedUponElement("dwc:scientificName");
        draft.setCriterionOrEnhancement("NotEmpty");
        // No type set
        assertNull(service.suggestLabel(draft),
                "Label suggestion requires test type");
    }

    @Test
    void testSuggestLabelReturnsNullWithoutIE() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.setCriterionOrEnhancement("NotEmpty");
        // No IE set
        assertNull(service.suggestLabel(draft),
                "Label suggestion requires at least one IE");
    }

    @Test
    void testSuggestLabelFallsBackToDimension() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.MEASURE);
        draft.addActedUponElement("dwc:scientificName");
        draft.setDimension("Completeness");
        // No criterion set; should fall back to dimension

        String label = service.suggestLabel(draft);
        assertNotNull(label);
        assertTrue(label.contains("COMPLETENESS"),
                "Should use COMPLETENESS from dimension: " + label);
    }

    @Test
    void testSuggestLabelUsesFirstActedUponElement() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.addActedUponElement("dwc:kingdom");
        draft.setCriterionOrEnhancement("NotEmpty");

        String label = service.suggestLabel(draft);
        // First actedUpon element should be used
        assertTrue(label.contains("SCIENTIFICNAME"),
                "Should use first actedUpon element: " + label);
    }

    @Test
    void testSuggestLabelFallsBackToLegacyIE() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        // Only legacy field set, no multi-valued lists
        draft.setInformationElement("dwc:basisOfRecord");
        draft.setCriterionOrEnhancement("Standard");

        String label = service.suggestLabel(draft);
        assertNotNull(label);
        assertTrue(label.contains("BASISOFRECORD"),
                "Should fall back to legacy IE: " + label);
    }

    // -----------------------------------------------------------------------
    // E3: Auto-suggestion update unless user override
    // -----------------------------------------------------------------------

    @Test
    void testApplyAutoSuggestionsDoesNotOverrideWhenFlagSet() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setCriterionOrEnhancement("NotEmpty");

        // User has manually set the label
        draft.setLabel("MY_CUSTOM_LABEL");
        draft.setLabelUserOverridden(true);
        draft.setPrefLabel("My custom preferred label");
        draft.setPrefLabelUserOverridden(true);

        service.applyAutoSuggestions(draft);

        assertEquals("MY_CUSTOM_LABEL", draft.getLabel(),
                "Manual label should be preserved when override flag is set");
        assertEquals("My custom preferred label", draft.getPrefLabel(),
                "Manual prefLabel should be preserved when override flag is set");
    }

    @Test
    void testApplyAutoSuggestionsUpdatesWhenNotOverridden() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setCriterionOrEnhancement("NotEmpty");
        // No override flag set

        service.applyAutoSuggestions(draft);

        assertNotNull(draft.getLabel(),
                "Label should be auto-suggested when not overridden");
        assertTrue(draft.getLabel().startsWith("VALIDATION_"),
                "Auto-suggested label should follow convention: " + draft.getLabel());
    }

    @Test
    void testAutoSuggestionRegeneratesWhenCriterionChanges() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:country");
        draft.setCriterionOrEnhancement("NotEmpty");
        // No override

        service.applyAutoSuggestions(draft);
        String first = draft.getLabel();

        // User changes criterion
        draft.setCriterionOrEnhancement("Standard");
        service.applyAutoSuggestions(draft);
        String second = draft.getLabel();

        assertNotEquals(first, second,
                "Label should regenerate when criterion changes (no override)");
        assertTrue(second.contains("STANDARD"),
                "Regenerated label should reflect new criterion: " + second);
    }

    // -----------------------------------------------------------------------
    // prefLabel suggestions
    // -----------------------------------------------------------------------

    @Test
    void testSuggestPrefLabel() {
        TestDraft draft = new TestDraft();
        draft.setType(TestType.VALIDATION);
        draft.addActedUponElement("dwc:scientificName");
        draft.setCriterionOrEnhancement("NotEmpty");

        String pref = service.suggestPrefLabel(draft);
        assertNotNull(pref);
        assertTrue(pref.startsWith("Validation "),
                "PrefLabel should start with type name: " + pref);
        assertTrue(pref.contains("scientificName"),
                "PrefLabel should contain IE local name: " + pref);
        assertTrue(pref.contains("NotEmpty"),
                "PrefLabel should contain criterion: " + pref);
    }

    @Test
    void testSuggestPrefLabelReturnsNullWithoutSufficientData() {
        TestDraft draft = new TestDraft();
        // Only type set – insufficient
        draft.setType(TestType.VALIDATION);
        assertNull(service.suggestPrefLabel(draft));
    }
}
