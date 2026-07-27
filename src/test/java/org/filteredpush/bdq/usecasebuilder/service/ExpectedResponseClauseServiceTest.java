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
        assertTrue(text.contains("IF prereq unavailable THEN"));
        assertTrue(text.contains("ELSE THEN"));
        assertEquals("COMPLIANT", parsed.get(1).getResult());
        assertTrue(parsed.get(2).isElseClause());
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
