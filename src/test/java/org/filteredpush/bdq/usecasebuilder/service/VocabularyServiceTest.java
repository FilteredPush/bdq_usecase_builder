package org.filteredpush.bdq.usecasebuilder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VocabularyService}.
 */
public class VocabularyServiceTest {

    private static final String CONCRETE_USE_CASE_TERM = "Occurrence data are fit for species distribution modeling";

    @Test
    public void testLoadBuiltInVocabularies() {
        VocabularyService service = new VocabularyService();
        service.load();

        assertFalse(service.getBdqDimensions().isEmpty(), "bdqdim should load");
        assertFalse(service.getBdqCriteria().isEmpty(), "bdqcrit should load");
        assertFalse(service.getBdqEnhancements().isEmpty(), "bdqenh should load");
        assertFalse(service.getBdqValidationTerms().isEmpty(), "bdqval should load");
        assertFalse(service.getBdqUseCaseTerms().isEmpty(), "bdquc should load");
    }

    @Test
    public void testUseCaseReferenceFilteringKeepsConcreteTerms() {
        List<String> filtered = VocabularyService.filterBdqUseCaseReferenceTerms(List.of(
                "UseCase",
                "DataQualityNeed",
                CONCRETE_USE_CASE_TERM));
        assertTrue(filtered.contains(CONCRETE_USE_CASE_TERM));
        assertFalse(filtered.contains("UseCase"));
        assertFalse(filtered.contains("DataQualityNeed"));
    }

    @Test
    public void testInformationElementTermsIncludeDwcAndAc() {
        VocabularyService service = new VocabularyService();
        service.load();

        assertTrue(service.getInformationElementTerms().contains("dwc:scientificName"));
        assertTrue(service.getInformationElementTerms().contains("ac:accessURI"));
    }

    @Test
    public void testLoadCustomVocabularyFile(@TempDir Path tempDir) throws Exception {
        Path customFile = tempDir.resolve("custom-vocabularies.properties");
        java.nio.file.Files.writeString(customFile, "custom=custom:alpha,custom:beta\n");
        String previous = System.getProperty(VocabularyService.CUSTOM_FILE_PROPERTY);
        System.setProperty(VocabularyService.CUSTOM_FILE_PROPERTY, customFile.toString());
        try {
            VocabularyService service = new VocabularyService();
            service.load();
            assertTrue(service.getAllVocabularies().containsKey("custom"));
            assertTrue(service.getInformationElementTerms().contains("custom:alpha"));
        } finally {
            if (previous == null) {
                System.clearProperty(VocabularyService.CUSTOM_FILE_PROPERTY);
            } else {
                System.setProperty(VocabularyService.CUSTOM_FILE_PROPERTY, previous);
            }
        }
    }
}
