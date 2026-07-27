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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (elseClause) {
            sb.append("ELSE");
        } else {
            sb.append("IF ").append(condition != null ? condition : "");
        }
        sb.append(" THEN status=").append(status != null ? status : "");
        sb.append("; result=").append(result != null ? result : "");
        if (commentTemplate != null && !commentTemplate.trim().isEmpty()) {
            sb.append("; comment=").append(commentTemplate.trim());
        }
        return sb.toString();
    }
}
