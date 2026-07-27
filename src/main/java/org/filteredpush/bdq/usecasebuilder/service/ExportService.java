package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.AuthorityDefault;
import org.filteredpush.bdq.usecasebuilder.model.ParameterDefinition;
import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export service that writes the completed project state to output artifacts.
 *
 * <p>Phase 1 outputs:</p>
 * <ul>
 *   <li>{@code usecase_summary.md} – a Markdown summary of the project</li>
 *   <li>{@code project_state.json} – a JSON snapshot of the full project state</li>
 * </ul>
 *
 * <p>The output directory is taken from {@link ProjectState#getOutputDirectory()}.
 * If the directory does not exist it is created.</p>
 */
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports both artifacts (Markdown summary and JSON state) to the output
     * directory configured in {@code state}.
     *
     * @param state the project state to export; must not be {@code null}
     * @return a human-readable summary of what was written
     * @throws IOException if either file cannot be written
     */
    public String export(ProjectState state) throws IOException {
        if (state == null) {
            throw new IllegalArgumentException("ProjectState must not be null");
        }

        String dir = state.getOutputDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "output";
        }

        File outputDir = new File(dir);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }

        File mdFile = new File(outputDir, "usecase_summary.md");
        File jsonFile = new File(outputDir, "project_state.json");

        writeMarkdown(state, mdFile);
        writeJson(state, jsonFile);
        ConformanceCsvService conformanceCsvService = new ConformanceCsvService();
        conformanceCsvService.writePerTestCsv(outputDir, state);
        conformanceCsvService.writeCombinedCsv(outputDir, state);

        String msg = "Exported:\n  " + mdFile.getAbsolutePath()
                + "\n  " + jsonFile.getAbsolutePath()
                + "\n  " + new File(outputDir, "conformance_all_tests.csv").getAbsolutePath();
        logger.info("Export complete: {}", msg);
        return msg;
    }

    // -----------------------------------------------------------------------
    // Markdown writer
    // -----------------------------------------------------------------------

    /**
     * Writes a Markdown summary of the project state to the given file.
     *
     * @param state the project state
     * @param file  the target file
     * @throws IOException on write failure
     */
    void writeMarkdown(ProjectState state, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            UseCaseDraft uc = state.getUseCaseDraft();

            pw.println("# BDQ Use Case Summary");
            pw.println();
            pw.println("*Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT) + "*");
            pw.println();

            // Use case section
            pw.println("## Use Case");
            pw.println();
            pw.println("**Name:** " + nvl(uc.getName()));
            pw.println();
            pw.println("**Description:**");
            pw.println();
            pw.println(nvl(uc.getDescription()));
            pw.println();
            pw.println("**Fitness for Use Requirements:**");
            pw.println();
            pw.println(nvl(uc.getFitnessRequirementsText()));
            pw.println();

            // Information elements section
            pw.println("## Information Elements");
            pw.println();
            if (state.getInformationElements().isEmpty()) {
                pw.println("*(none defined)*");
            } else {
                pw.println("| Term | Role |");
                pw.println("|------|------|");
                for (InformationElementRef ref : state.getInformationElements()) {
                    pw.println("| `" + ref.getQname() + "` | "
                            + (ref.getRole() != null ? ref.getRole().getDisplayName() : "?") + " |");
                }
            }
            pw.println();

            // Existing tests section
            pw.println("## Selected Existing Tests");
            pw.println();
            if (state.getSelectedExistingTestIris().isEmpty()) {
                pw.println("*(none selected)*");
            } else {
                for (String iri : state.getSelectedExistingTestIris()) {
                    pw.println("- <" + iri + ">");
                }
            }
            pw.println();

            // New tests section
            pw.println("## New Tests Defined");
            pw.println();
            if (state.getNewTestDrafts().isEmpty()) {
                pw.println("*(none defined)*");
            } else {
                for (int i = 0; i < state.getNewTestDrafts().size(); i++) {
                    TestDraft td = state.getNewTestDrafts().get(i);
                    pw.println("### Test " + (i + 1) + ": " + nvl(td.getLabel()));
                    pw.println();
                    pw.println("- **Preferred label:** " + nvl(td.getPrefLabel()));
                    pw.println("- **Type:** "
                            + (td.getType() != null ? td.getType().getDisplayName() : "?"));
                    pw.println("- **Resource type:** "
                            + (td.getResourceType() != null
                                    ? td.getResourceType().getDisplayName() : "?"));
                    pw.println("- **Information element:** " + nvl(td.getInformationElement()));
                    pw.println("- **Dimension:** " + nvl(td.getDimension()));
                    pw.println("- **Criterion/Enhancement:** "
                            + nvl(td.getCriterionOrEnhancement()));
                    pw.println("- **Use-case reference:** " + nvl(td.getUseCaseReference()));
                    pw.println("- **Parameters/defaults:** " + nvl(td.getParameterDefaults()));
                    if (!td.getParameterDefinitions().isEmpty()) {
                        pw.println("- **Parameters:**");
                        for (ParameterDefinition parameter : td.getParameterDefinitions()) {
                            pw.println("  - `" + nvl(parameter.getName()) + "` "
                                    + "(datatype: " + nvl(parameter.getDatatype()) + ", "
                                    + "default authority: " + nvl(parameter.getDefaultAuthorityIdentifier()) + ")");
                        }
                    }
                    if (!td.getAuthorityDefaults().isEmpty()) {
                        pw.println("- **Authority defaults:**");
                        for (AuthorityDefault authority : td.getAuthorityDefaults()) {
                            pw.println("  - `" + nvl(authority.getIdentifier()) + "` "
                                    + "[" + (authority.getPatternType() != null
                                    ? authority.getPatternType().name() : "") + "] "
                                    + nvl(authority.getAuthorityUri()));
                        }
                    }
                    pw.println();
                    pw.println("**Expected Response:**");
                    pw.println();
                    pw.println(nvl(td.getExpectedResponse()));
                    if (!td.getExpectedResponseClauses().isEmpty()) {
                        pw.println();
                        pw.println("**Expected Response Clauses:**");
                        for (ExpectedResponseClause clause : td.getExpectedResponseClauses()) {
                            pw.println("- " + clause);
                        }
                    }
                    pw.println();
                    if (td.getNotes() != null && !td.getNotes().trim().isEmpty()) {
                        pw.println("**Notes:** " + td.getNotes());
                        pw.println();
                    }
                }
            }

            pw.println("## Gap Analysis Matrix");
            pw.println();
            if (state.getRequirementCoverageRows().isEmpty()) {
                pw.println("*(none defined)*");
            } else {
                pw.println("| Requirement ID/summary | Information Element(s) | Existing Tests mapped | New Tests drafted | Coverage status | Notes/rationale |");
                pw.println("|---|---|---|---|---|---|");
                for (RequirementCoverage row : state.getRequirementCoverageRows()) {
                    pw.println("| " + nvl(row.getRequirementId()) + " " + nvl(row.getRequirementSummary())
                            + " | " + nvl(row.getInformationElements())
                            + " | " + nvl(String.join("; ", row.getLinkedExistingTests()))
                            + " | " + nvl(String.join("; ", row.getLinkedNewTests()))
                            + " | " + row.computeStatus().getDisplayName()
                            + " | " + nvl(row.getPartialCoverageRationale()) + " |");
                }
            }
        }
        logger.debug("Wrote Markdown summary to {}", file.getAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // JSON writer
    // -----------------------------------------------------------------------

    /**
     * Writes the project state as a JSON document to the given file.
     *
     * <p>Uses a manually constructed {@code Map} structure so that no external
     * JSON library other than what the project already depends on is required.
     * The structure mirrors the domain model.</p>
     *
     * @param state the project state
     * @param file  the target file
     * @throws IOException on write failure
     */
    void writeJson(ProjectState state, File file) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("generated", LocalDateTime.now().format(TIMESTAMP_FMT));
        root.put("outputDirectory", state.getOutputDirectory());

        // Use case
        UseCaseDraft uc = state.getUseCaseDraft();
        Map<String, Object> ucMap = new LinkedHashMap<>();
        ucMap.put("name", uc.getName());
        ucMap.put("description", uc.getDescription());
        ucMap.put("fitnessRequirementsText", uc.getFitnessRequirementsText());
        root.put("useCase", ucMap);

        // Information elements
        List<Map<String, String>> ieList = new ArrayList<>();
        for (InformationElementRef ref : state.getInformationElements()) {
            Map<String, String> ieMap = new LinkedHashMap<>();
            ieMap.put("qname", ref.getQname());
            ieMap.put("role", ref.getRole() != null ? ref.getRole().name() : null);
            ieList.add(ieMap);
        }
        root.put("informationElements", ieList);

        // Selected existing tests
        root.put("selectedExistingTests", new ArrayList<>(state.getSelectedExistingTestIris()));

        // New test drafts
        List<Map<String, String>> testList = new ArrayList<>();
        for (TestDraft td : state.getNewTestDrafts()) {
            Map<String, String> testMap = new LinkedHashMap<>();
            testMap.put("label", td.getLabel());
            testMap.put("prefLabel", td.getPrefLabel());
            testMap.put("type", td.getType() != null ? td.getType().name() : null);
            testMap.put("resourceType",
                    td.getResourceType() != null ? td.getResourceType().name() : null);
            testMap.put("informationElement", td.getInformationElement());
            testMap.put("dimension", td.getDimension());
            testMap.put("criterionOrEnhancement", td.getCriterionOrEnhancement());
            testMap.put("useCaseReference", td.getUseCaseReference());
            testMap.put("parameterDefaults", td.getParameterDefaults());
            testMap.put("expectedResponse", td.getExpectedResponse());
            testMap.put("notes", td.getNotes());
            if (!td.getExpectedResponseClauses().isEmpty()) {
                testMap.put("expectedResponseClauses", joinClauses(td.getExpectedResponseClauses()));
            }
            if (!td.getAuthorityDefaults().isEmpty()) {
                testMap.put("authorityDefaults", joinAuthorities(td.getAuthorityDefaults()));
            }
            if (!td.getParameterDefinitions().isEmpty()) {
                testMap.put("parameterDefinitions", joinParameters(td.getParameterDefinitions()));
            }
            if (!td.getConformanceRows().isEmpty()) {
                testMap.put("conformanceRows", joinConformanceRows(td.getConformanceRows()));
            }
            testList.add(testMap);
        }
        root.put("newTests", testList);
        root.put("requirementCoverageMatrix", toRequirementCoverageRows(state));

        // Serialize using built-in simple JSON writer
        writeSimpleJson(root, file);

        logger.debug("Wrote JSON state to {}", file.getAbsolutePath());
    }

    /**
     * Minimal JSON serializer used as a fallback when Jackson is not on the
     * classpath. Only handles {@code String}, {@code List}, and {@code Map}
     * values.
     */
    private void writeSimpleJson(Map<String, Object> root, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            pw.print(toJson(root, 0));
        }
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object value, int indent) {
        if (value == null) {
            return "null";
        }
        String pad = "  ".repeat(indent);
        String pad1 = "  ".repeat(indent + 1);
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder sb = new StringBuilder("{\n");
            List<String> keys = new ArrayList<>(map.keySet());
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                sb.append(pad1).append('"').append(escape(k)).append("\": ")
                        .append(toJson(map.get(k), indent + 1));
                if (i < keys.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append(pad).append('}');
            return sb.toString();
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(pad1).append(toJson(list.get(i), indent + 1));
                if (i < list.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append(pad).append(']');
            return sb.toString();
        }
        // String or other
        return '"' + escape(value.toString()) + '"';
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private String joinClauses(List<ExpectedResponseClause> clauses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append(clauses.get(i).toString());
        }
        return sb.toString();
    }

    private String joinAuthorities(List<AuthorityDefault> authorities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < authorities.size(); i++) {
            AuthorityDefault authority = authorities.get(i);
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append(nvl(authority.getIdentifier())).append('|')
                    .append(authority.getPatternType() != null ? authority.getPatternType().name() : "")
                    .append('|').append(nvl(authority.getAuthorityUri()))
                    .append('|').append(nvl(authority.getApiLabel()))
                    .append('|').append(nvl(authority.getApiEndpoint()))
                    .append('|').append(nvl(authority.getRegexPattern()));
        }
        return sb.toString();
    }

    private String joinParameters(List<ParameterDefinition> parameters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            ParameterDefinition parameter = parameters.get(i);
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append(nvl(parameter.getName())).append('|')
                    .append(nvl(parameter.getDatatype())).append('|')
                    .append(nvl(parameter.getDefaultAuthorityIdentifier())).append('|')
                    .append(nvl(parameter.getNotes()));
        }
        return sb.toString();
    }

    private String joinConformanceRows(List<ConformanceRow> rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append(rows.get(i).getValues().toString());
        }
        return sb.toString();
    }

    private List<Map<String, String>> toRequirementCoverageRows(ProjectState state) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (RequirementCoverage row : state.getRequirementCoverageRows()) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("requirementId", row.getRequirementId());
            map.put("requirementSummary", row.getRequirementSummary());
            map.put("informationElements", row.getInformationElements());
            map.put("existingTests", String.join("; ", row.getLinkedExistingTests()));
            map.put("newTests", String.join("; ", row.getLinkedNewTests()));
            map.put("coverageStatus", row.computeStatus().name());
            map.put("partialCoverageRationale", row.getPartialCoverageRationale());
            map.put("notes", row.getNotes());
            rows.add(map);
        }
        return rows;
    }
}
