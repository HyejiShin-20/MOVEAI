package com.moveai.retrieval;

/** 지식 embedding_text와 같은 라벨 형식으로 세그먼트 및 평가 질의를 만든다. */
public class QueryTextBuilder {

    public String build(SegmentContext segment) {
        return "위치: " + segment.toNodeName() + "\n"
                + "이동: " + movementLabel(
                        segment.movementMode(), segment.traversalMethod(), segment.customTraversalMethod())
                + "\n내용: " + segment.instruction().strip();
    }

    public String buildEvaluationQuestion(String question, String movementMode) {
        if (movementMode == null || movementMode.isBlank()) {
            return "내용: " + question.strip();
        }
        return "이동: " + movementMode + "\n내용: " + question.strip();
    }

    private String movementLabel(String mode, String traversal, String customTraversal) {
        String resolvedTraversal = "OTHER".equals(traversal) ? stripToNull(customTraversal) : traversal;
        return resolvedTraversal == null ? mode : mode + " / " + resolvedTraversal;
    }

    private String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
