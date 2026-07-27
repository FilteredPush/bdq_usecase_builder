package org.filteredpush.bdq.usecasebuilder.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One editable conformance CSV row for a test draft.
 */
public class ConformanceRow {

    private final Map<String, String> values = new LinkedHashMap<>();

    public Map<String, String> getValues() {
        return values;
    }

    public void put(String key, String value) {
        values.put(key, value);
    }
}
