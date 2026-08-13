package com.moveai.knowledge.embedding;

/** Python 시드 생성기와 동일한 04 §5-1 embedding_text 포맷. */
public class EmbeddingTextBuilder {

    private static final String UNKNOWN_LOCATION = "위치 미상";

    public String build(Source source) {
        StringBuilder text = new StringBuilder()
                .append("위치: ").append(location(source)).append('\n')
                .append("이동: ").append(movement(source)).append('\n')
                .append("내용: ").append(source.statement().strip());
        if (source.actionText() != null && !source.actionText().isBlank()) {
            text.append('\n').append("행동: ").append(source.actionText().strip());
        }
        return text.toString();
    }

    private String location(Source source) {
        return switch (source.targetType()) {
            case "NODE" -> requireText(source.nodeName(), "NODE 타깃 이름");
            case "SEGMENT" -> requireText(source.fromNodeName(), "SEGMENT 출발 노드")
                    + " → " + requireText(source.toNodeName(), "SEGMENT 도착 노드");
            case "PLACE" -> requireText(source.placeName(), "PLACE 이름");
            case "UNKNOWN" -> blankToDefault(source.targetFreeText(), UNKNOWN_LOCATION);
            default -> throw new IllegalArgumentException("지원하지 않는 target_type: " + source.targetType());
        };
    }

    private String movement(Source source) {
        String traversal = "OTHER".equals(source.traversalMethod())
                ? stripToNull(source.customTraversalMethod()) : stripToNull(source.traversalMethod());
        return traversal == null ? source.movementMode() : source.movementMode() + " / " + traversal;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "이 비어 있습니다.");
        }
        return value.strip();
    }

    private String blankToDefault(String value, String defaultValue) {
        String resolved = stripToNull(value);
        return resolved == null ? defaultValue : resolved;
    }

    private String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record Source(
            String knowledgeCode,
            String targetType,
            String targetFreeText,
            String placeName,
            String nodeName,
            String fromNodeName,
            String toNodeName,
            String movementMode,
            String traversalMethod,
            String customTraversalMethod,
            String statement,
            String actionText) {
    }
}
