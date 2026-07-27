package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates and writes conformance CSV rows.
 */
public class ConformanceCsvService {

    public List<ConformanceRow> generateStarterRows(TestDraft draft) {
        List<ConformanceRow> rows = new ArrayList<>();
        List<ExpectedResponseClause> clauses = draft.getExpectedResponseClauses();
        for (int i = 0; i < clauses.size(); i++) {
            ExpectedResponseClause clause = clauses.get(i);
            ConformanceRow row = new ConformanceRow();
            row.put("Label", "Clause " + (i + 1));
            row.put("Response.status", nvl(clause.getStatus()));
            row.put("Response.result", nvl(clause.getResult()));
            row.put("Response.comment", nvl(clause.getCommentTemplate()));
            rows.add(row);
        }
        if (rows.isEmpty()) {
            ConformanceRow row = new ConformanceRow();
            row.put("Label", "Starter");
            row.put("Response.status", "RUN_HAS_RESULT");
            row.put("Response.result", "COMPLIANT");
            row.put("Response.comment", "");
            rows.add(row);
        }
        return rows;
    }

    public List<String> buildColumns(ProjectState state, TestDraft draft) {
        List<String> columns = new ArrayList<>();
        columns.add("Label");
        for (String ie : collectInformationElements(state, draft)) {
            columns.add(ie);
        }
        for (String parameter : collectParameterNames(draft)) {
            columns.add(parameter);
        }
        columns.add("Response.status");
        columns.add("Response.result");
        columns.add("Response.comment");
        return columns;
    }

    public void writePerTestCsv(File outputDir, ProjectState state) throws IOException {
        for (TestDraft draft : state.getNewTestDrafts()) {
            String base = sanitizeFileName(
                    draft.getLabel() != null && !draft.getLabel().trim().isEmpty()
                            ? draft.getLabel()
                            : draft.toString());
            File file = new File(outputDir, "conformance_" + base + ".csv");
            writeSingleCsv(file, buildColumns(state, draft), draft.getConformanceRows());
        }
    }

    public void writeCombinedCsv(File outputDir, ProjectState state) throws IOException {
        File file = new File(outputDir, "conformance_all_tests.csv");
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            pw.println("TestLabel,Label,Response.status,Response.result,Response.comment");
            for (TestDraft draft : state.getNewTestDrafts()) {
                String testLabel = draft.getLabel() != null ? draft.getLabel() : "";
                for (ConformanceRow row : draft.getConformanceRows()) {
                    pw.println(csv(testLabel) + "," + csv(row.getValues().get("Label")) + ","
                            + csv(row.getValues().get("Response.status")) + ","
                            + csv(row.getValues().get("Response.result")) + ","
                            + csv(row.getValues().get("Response.comment")));
                }
            }
        }
    }

    public void writeSingleCsv(File file, List<String> columns, List<ConformanceRow> rows)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            pw.println(String.join(",", columns));
            for (ConformanceRow row : rows) {
                List<String> values = new ArrayList<>();
                for (String column : columns) {
                    values.add(csv(row.getValues().get(column)));
                }
                pw.println(String.join(",", values));
            }
        }
    }

    private Set<String> collectInformationElements(ProjectState state, TestDraft draft) {
        Set<String> terms = new LinkedHashSet<>();
        if (draft.getInformationElement() != null && !draft.getInformationElement().trim().isEmpty()) {
            terms.add(draft.getInformationElement().trim());
        }
        state.getInformationElements().forEach(ref -> {
            if (ref.getQname() != null && !ref.getQname().trim().isEmpty()) {
                terms.add(ref.getQname().trim());
            }
        });
        return terms;
    }

    private Set<String> collectParameterNames(TestDraft draft) {
        Set<String> names = new LinkedHashSet<>();
        draft.getParameterDefinitions().forEach(def -> {
            if (def.getName() != null && !def.getName().trim().isEmpty()) {
                names.add(def.getName().trim());
            }
        });
        return names;
    }

    private String sanitizeFileName(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    }

    private String csv(String value) {
        String safe = value != null ? value : "";
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
