package org.filteredpush.bdq.usecasebuilder.model;

/**
 * Structured expected-response clause.
 */
public class ExpectedResponseClause {

    private boolean elseClause;
    private String condition;
    private String status;
    private String result;
    private String commentTemplate;

    public boolean isElseClause() {
        return elseClause;
    }

    public void setElseClause(boolean elseClause) {
        this.elseClause = elseClause;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getCommentTemplate() {
        return commentTemplate;
    }

    public void setCommentTemplate(String commentTemplate) {
        this.commentTemplate = commentTemplate;
    }

    /**
     * Returns the human-readable outcome token for this clause.
     *
     * <p>When status is {@code RUN_HAS_RESULT}, the {@code result} value
     * (e.g. {@code COMPLIANT}) is the visible outcome. For prerequisite-not-met
     * statuses the status itself is the visible outcome.</p>
     */
    public String getOutcomeToken() {
        String statusValue = status != null ? status.trim() : "";
        String resultValue = result != null ? result.trim() : "";
        if (!statusValue.isEmpty() && !"RUN_HAS_RESULT".equals(statusValue)) {
            return statusValue;
        }
        if (!resultValue.isEmpty()) {
            return resultValue;
        }
        return statusValue.isEmpty() ? "(unknown)" : statusValue;
    }

    /**
     * Returns the clause as a compact, human-readable string in the form:
     * <ul>
     *   <li>{@code RESULT if condition} for a regular clause</li>
     *   <li>{@code otherwise RESULT} for an else/fallback clause</li>
     * </ul>
     */
    @Override
    public String toString() {
        String outcomeToken = getOutcomeToken();
        if (elseClause) {
            return "otherwise " + outcomeToken;
        }
        String cond = condition != null ? condition.trim() : "";
        return outcomeToken + " if " + cond;
    }
}
