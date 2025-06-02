package com.andreidodu.jmodules.controller;

import com.andreidodu.jmodules.record.UiRecord;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface JModuleObserver {
    List<String> loadFileName(String singleFilename);

    List<String> loadDirectory(String absolutePath);

    void processJars(UiRecord uiRecord, Function<String, List<String>> engine);

    Set<String> parseModules(Set<String> modules);

    List<String> loadPomJars(String absolutePath);

    void processPomJars(UiRecord build);

    void processDirectoryJars(UiRecord build);

    void processFileJar(UiRecord build);

    void shutdown();
}
