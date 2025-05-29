package record;

import lombok.Builder;

@Builder
public record UiRecord(String javaVersion, String absolutePath) {
}
