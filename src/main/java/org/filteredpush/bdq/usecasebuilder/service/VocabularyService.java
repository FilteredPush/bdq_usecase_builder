package org.filteredpush.bdq.usecasebuilder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Loads local controlled vocabularies used by the Swing wizard.
 *
 * <p>Built-in vocabularies are read from classpath CSV resources under
 * {@code /catalog/vocabulary/}. Custom information-element vocabularies can be
 * added through a local properties file in the working directory:
 * {@code vocab/custom-vocabularies.properties}.</p>
 */
public class VocabularyService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyService.class);

    static final String VOCAB_BASE = "/catalog/vocabulary/";
    static final String CUSTOM_FILE = "vocab/custom-vocabularies.properties";
    static final String CUSTOM_FILE_PROPERTY = "bdq.custom.vocab.file";

    private final Map<String, List<String>> vocabularies = new LinkedHashMap<>();

    /** Loads all built-in and custom vocabularies. */
    public void load() {
        vocabularies.clear();
        loadCsvVocabulary("bdqdim");
        loadCsvVocabulary("bdqcrit");
        loadCsvVocabulary("bdqenh");
        loadCsvVocabulary("bdqval");
        loadCsvVocabulary("bdquc");
        loadCsvVocabulary("dwc");
        loadCsvVocabulary("ac");
        loadCustomInformationElementVocabularies();
    }

    /**
     * Returns bdqdim terms.
     *
     * @return list of bdqdim vocabulary terms; never {@code null}
     */
    public List<String> getBdqDimensions() {
        return getVocabulary("bdqdim");
    }

    /**
     * Returns bdqcrit terms.
     *
     * @return list of bdqcrit vocabulary terms; never {@code null}
     */
    public List<String> getBdqCriteria() {
        return getVocabulary("bdqcrit");
    }

    /**
     * Returns bdqenh terms.
     *
     * @return list of bdqenh vocabulary terms; never {@code null}
     */
    public List<String> getBdqEnhancements() {
        return getVocabulary("bdqenh");
    }

    /**
     * Returns bdqval terms.
     *
     * @return list of bdqval vocabulary terms; never {@code null}
     */
    public List<String> getBdqValidationTerms() {
        return getVocabulary("bdqval");
    }

    /**
     * Returns bdquc terms.
     *
     * @return list of bdquc vocabulary terms; never {@code null}
     */
    public List<String> getBdqUseCaseTerms() {
        return getVocabulary("bdquc");
    }

    /**
     * Returns bdquc terms that represent concrete use cases rather than ontology classes.
     */
    public List<String> getBdqUseCaseReferenceTerms() {
        return filterBdqUseCaseReferenceTerms(getBdqUseCaseTerms());
    }

    /** Returns information-element terms from dwc, ac, and custom vocabularies. */
    public List<String> getInformationElementTerms() {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(getVocabulary("dwc"));
        merged.addAll(getVocabulary("ac"));
        for (Map.Entry<String, List<String>> entry : vocabularies.entrySet()) {
            String key = entry.getKey();
            if (!"dwc".equals(key) && !"ac".equals(key)
                    && !key.startsWith("bdq")) {
                merged.addAll(entry.getValue());
            }
        }
        return new ArrayList<>(merged);
    }

    /** Returns all loaded vocabularies by id. */
    public Map<String, List<String>> getAllVocabularies() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : vocabularies.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /** Returns terms for the named vocabulary (or empty list). */
    public List<String> getVocabulary(String vocabularyId) {
        List<String> terms = vocabularies.get(vocabularyId);
        return terms != null ? List.copyOf(terms) : Collections.emptyList();
    }

    private void loadCsvVocabulary(String vocabularyId) {
        String resource = VOCAB_BASE + vocabularyId + ".csv";
        List<String> terms = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null) {
                logger.warn("Vocabulary resource not found: {}", resource);
                vocabularies.put(vocabularyId, terms);
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    List<String> fields = parseCsvLine(line);
                    if (!fields.isEmpty() && !fields.get(0).trim().isEmpty()) {
                        terms.add(fields.get(0).trim());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load vocabulary {}: {}", vocabularyId, e.getMessage());
        }
        vocabularies.put(vocabularyId, terms);
    }

    private void loadCustomInformationElementVocabularies() {
        File file = new File(System.getProperty(CUSTOM_FILE_PROPERTY, CUSTOM_FILE));
        if (!file.exists() || !file.isFile()) {
            return;
        }
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);
        } catch (IOException e) {
            logger.warn("Could not load custom vocabularies file '{}': {}", file.getPath(), e.getMessage());
            return;
        }
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            List<String> terms = new ArrayList<>();
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    terms.add(trimmed);
                }
            }
            if (!terms.isEmpty()) {
                vocabularies.put(key.trim(), terms);
            }
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }

    static List<String> filterBdqUseCaseReferenceTerms(List<String> terms) {
        List<String> filtered = new ArrayList<>();
        for (String term : terms) {
            if (term == null || term.trim().isEmpty()) {
                continue;
            }
            if (!isBdqUseCaseClassTerm(term)) {
                filtered.add(term.trim());
            }
        }
        return List.copyOf(filtered);
    }

    private static boolean isBdqUseCaseClassTerm(String term) {
        return isKnownClassName(extractLocalName(term.trim()));
    }

    private static String extractLocalName(String term) {
        int slash = term.lastIndexOf('/');
        int hash = term.lastIndexOf('#');
        int colon = term.lastIndexOf(':');
        int cut = Math.max(Math.max(slash, hash), colon);
        return cut >= 0 && cut + 1 < term.length() ? term.substring(cut + 1) : term;
    }

    private static boolean isKnownClassName(String localName) {
        return "UseCase".equals(localName)
                || "DataQualityNeed".equals(localName)
                || "DataQualityAssessmentPolicy".equals(localName)
                || "Specification".equals(localName);
    }
}
