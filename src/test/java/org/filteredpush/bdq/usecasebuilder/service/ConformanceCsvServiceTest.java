package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ParameterDefinition;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConformanceCsvServiceTest {

    @Test
    public void testGenerateThreeRowsForThreeClauses() {
        ConformanceCsvService service = new ConformanceCsvService();
        TestDraft draft = buildDraftWithThreeClauses();

        List<ConformanceRow> rows = service.generateStarterRows(draft);

        assertEquals(3, rows.size());
        assertEquals("COMPLIANT", rows.get(1).getValues().get("Response.result"));
    }

    @Test
    public void testWriteCsvContainsRequiredColumns(@TempDir Path tempDir) throws Exception {
        ConformanceCsvService service = new ConformanceCsvService();
        ProjectState state = new ProjectState();
        state.addInformationElement(new InformationElementRef("dwc:scientificName", InfoElementRole.ACTED_UPON));

        TestDraft draft = buildDraftWithThreeClauses();
        ParameterDefinition parameter = new ParameterDefinition();
        parameter.setName("bdqval:sourceAuthority");
        draft.setParameterDefinitions(List.of(parameter));
        draft.setConformanceRows(service.generateStarterRows(draft));
        state.addNewTestDraft(draft);

        service.writePerTestCsv(tempDir.toFile(), state);

        Path csv = Files.list(tempDir).filter(p -> p.getFileName().toString().endsWith(".csv")).findFirst().orElseThrow();
        String content = Files.readString(csv);
        assertTrue(content.contains("Label"));
        assertTrue(content.contains("Response.status"));
        assertTrue(content.contains("Response.result"));
        assertTrue(content.contains("Response.comment"));
        assertTrue(content.contains("bdqval:sourceAuthority"));
    }

    private TestDraft buildDraftWithThreeClauses() {
        TestDraft draft = new TestDraft();
        draft.setLabel("VALIDATION_TEST");
        List<ExpectedResponseClause> clauses = new ArrayList<>();
        clauses.add(buildClause(false, "prereq", "INTERNAL_PREREQUISITES_NOT_MET",
                "INTERNAL_PREREQUISITES_NOT_MET", "blocked"));
        clauses.add(buildClause(false, "match", "RUN_HAS_RESULT", "COMPLIANT", "ok"));
        clauses.add(buildClause(true, "", "RUN_HAS_RESULT", "NOT_COMPLIANT", "fallback"));
        draft.setExpectedResponseClauses(clauses);
        return draft;
    }

    private ExpectedResponseClause buildClause(boolean elseClause, String condition,
                                               String status, String result, String comment) {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setElseClause(elseClause);
        clause.setCondition(condition);
        clause.setStatus(status);
        clause.setResult(result);
        clause.setCommentTemplate(comment);
        return clause;
    }
}
