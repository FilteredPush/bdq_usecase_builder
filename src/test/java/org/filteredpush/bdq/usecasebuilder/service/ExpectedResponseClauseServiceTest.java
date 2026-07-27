package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ExpectedResponseClause;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpectedResponseClauseServiceTest {

    @Test
    public void testClauseSerializationAndOrderingRoundTrip() {
        ExpectedResponseClauseService service = new ExpectedResponseClauseService();
        List<ExpectedResponseClause> clauses = new ArrayList<>();
        clauses.add(buildClause(false, "prereq unavailable", "INTERNAL_PREREQUISITES_NOT_MET",
                "INTERNAL_PREREQUISITES_NOT_MET", "requires setup"));
        clauses.add(buildClause(false, "record matches authority", "RUN_HAS_RESULT",
                "COMPLIANT", "good"));
        clauses.add(buildClause(true, "", "RUN_HAS_RESULT", "NOT_COMPLIANT", "fallback"));

        String text = service.toCanonicalText(clauses);
        List<ExpectedResponseClause> parsed = service.parseCanonicalText(text);

        assertEquals(3, parsed.size());
        // New compact format: "RESULT if condition"
        assertTrue(text.contains("INTERNAL_PREREQUISITES_NOT_MET if prereq unavailable"),
                "Expected INTERNAL_PREREQUISITES_NOT_MET clause in: " + text);
        assertTrue(text.contains("COMPLIANT if record matches authority"),
                "Expected COMPLIANT clause in: " + text);
        assertTrue(text.contains("otherwise NOT_COMPLIANT"),
                "Expected otherwise NOT_COMPLIANT in: " + text);
        assertEquals("COMPLIANT", parsed.get(1).getResult());
        assertTrue(parsed.get(2).isElseClause());
    }

    @Test
    public void testLegacyFormatBackwardCompat() {
        ExpectedResponseClauseService service = new ExpectedResponseClauseService();
        String legacy = "IF dwc:taxonRank is bdqval:Empty THEN status=INTERNAL_PREREQUISITES_NOT_MET\n"
                + "IF dwc:taxonRank is species THEN status=RUN_HAS_RESULT; result=COMPLIANT\n"
                + "ELSE THEN status=RUN_HAS_RESULT; result=NOT_COMPLIANT";
        List<ExpectedResponseClause> parsed = service.parseCanonicalText(legacy);
        assertEquals(3, parsed.size());
        assertEquals("INTERNAL_PREREQUISITES_NOT_MET", parsed.get(0).getStatus());
        assertEquals("dwc:taxonRank is bdqval:Empty", parsed.get(0).getCondition());
        assertEquals("COMPLIANT", parsed.get(1).getResult());
        assertTrue(parsed.get(2).isElseClause());
        assertEquals("NOT_COMPLIANT", parsed.get(2).getResult());
    }

    @Test
    public void testNewFormatParsing() {
        ExpectedResponseClauseService service = new ExpectedResponseClauseService();
        String text = "INTERNAL_PREREQUISITES_NOT_MET if dwc:taxonRank is bdqval:Empty;\n"
                + "COMPLIANT if dwc:taxonRank is species or lower;\n"
                + "otherwise NOT_COMPLIANT";
        List<ExpectedResponseClause> parsed = service.parseCanonicalText(text);
        assertEquals(3, parsed.size());
        assertEquals("INTERNAL_PREREQUISITES_NOT_MET", parsed.get(0).getStatus());
        assertEquals("dwc:taxonRank is bdqval:Empty", parsed.get(0).getCondition());
        assertEquals("RUN_HAS_RESULT", parsed.get(1).getStatus());
        assertEquals("COMPLIANT", parsed.get(1).getResult());
        assertEquals("dwc:taxonRank is species or lower", parsed.get(1).getCondition());
        assertTrue(parsed.get(2).isElseClause());
        assertEquals("NOT_COMPLIANT", parsed.get(2).getResult());
    }

    private ExpectedResponseClause buildClause(boolean isElse, String condition,
                                               String status, String result, String comment) {
        ExpectedResponseClause clause = new ExpectedResponseClause();
        clause.setElseClause(isElse);
        clause.setCondition(condition);
        clause.setStatus(status);
        clause.setResult(result);
        clause.setCommentTemplate(comment);
        return clause;
    }
}
