package org.filteredpush.bdq.usecasebuilder.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads and provides the local BDQ test catalog bundled with the application.
 *
 * <p>The catalog is read from the classpath resource
 * {@code /catalog/bdqtest_catalog.csv} (a CSV file with the columns:
 * {@code iri,label,prefLabel,type,dimension}). The first line is treated as a
 * header and is skipped. Lines that are blank or start with {@code #} are also
 * skipped.</p>
 *
 * <p>Call {@link #loadCatalog()} once to populate the catalog; the result is
 * then available via {@link #getEntries()}. The service is intentionally
 * stateless between calls and re-reads the classpath resource each time
 * {@link #loadCatalog()} is invoked (so an updated bundled resource is picked
 * up automatically without restarting).</p>
 */
public class TestCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(TestCatalogService.class);

    /** Classpath path to the bundled test catalog CSV. */
    public static final String CATALOG_RESOURCE = "/catalog/bdqtest_catalog.csv";

    private final List<TestCatalogEntry> entries = new ArrayList<>();

    /**
     * Loads (or reloads) the catalog from the classpath resource.
     *
     * @return the number of entries successfully loaded
     */
    public int loadCatalog() {
        entries.clear();
        try (InputStream is = getClass().getResourceAsStream(CATALOG_RESOURCE)) {
            if (is == null) {
                logger.warn("Catalog resource not found on classpath: {}", CATALOG_RESOURCE);
                return 0;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false; // skip header
                        continue;
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    TestCatalogEntry entry = parseLine(line);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error loading catalog: {}", e.getMessage(), e);
        }
        logger.info("Loaded {} test catalog entries", entries.size());
        return entries.size();
    }

    /**
     * Returns an unmodifiable view of the currently loaded catalog entries.
     *
     * @return list of entries; never {@code null}, may be empty if
     *         {@link #loadCatalog()} has not been called yet
     */
    public List<TestCatalogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Adds a single entry to the catalog (e.g. loaded from an external RDF document).
     * Duplicate IRIs are silently ignored.
     *
     * @param entry the entry to add; {@code null} is ignored
     */
    public void addEntry(TestCatalogEntry entry) {
        if (entry == null || entry.getIri() == null || entry.getIri().isEmpty()) {
            return;
        }
        boolean duplicate = entries.stream().anyMatch(e -> entry.getIri().equals(e.getIri()));
        if (!duplicate) {
            entries.add(entry);
        }
    }

    /**
     * Adds multiple entries to the catalog, ignoring duplicates by IRI.
     *
     * @param newEntries list of entries to add; {@code null} is ignored
     */
    public void addEntries(List<TestCatalogEntry> newEntries) {
        if (newEntries == null) {
            return;
        }
        int added = 0;
        for (TestCatalogEntry e : newEntries) {
            int before = entries.size();
            addEntry(e);
            if (entries.size() > before) {
                added++;
            }
        }
        logger.info("Added {}/{} entries from external source (duplicates skipped); catalog now has {} entries",
                added, newEntries.size(), entries.size());
    }

    // -----------------------------------------------------------------------
    // CSV parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a single CSV line into a {@link TestCatalogEntry}.
     *
     * <p>Fields are separated by commas. Fields that contain commas must be
     * enclosed in double quotes. A double quote inside a quoted field is
     * represented as two consecutive double quotes ({@code ""}).</p>
     *
     * @param line the raw CSV line (not null, not blank)
     * @return the parsed entry, or {@code null} if the line has fewer than 5
     *         fields
     */
    TestCatalogEntry parseLine(String line) {
        List<String> fields = parseCsvLine(line);
        if (fields.size() < 5) {
            logger.warn("Skipping malformed catalog line (expected 5 fields): {}", line);
            return null;
        }
        return new TestCatalogEntry(
                fields.get(0),
                fields.get(1),
                fields.get(2),
                fields.get(3),
                fields.get(4));
    }

    /**
     * Minimal RFC-4180-compatible CSV field splitter.
     *
     * @param line a single CSV line
     * @return list of field values (quotes stripped, {@code ""} unescaped)
     */
    static List<String> parseCsvLine(String line) {
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
}
