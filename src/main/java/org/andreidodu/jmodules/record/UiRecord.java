package org.andreidodu.jmodules.record;

import lombok.Builder;

@Builder
public record UiRecord(String javaVersion, String absolutePath) {
}
