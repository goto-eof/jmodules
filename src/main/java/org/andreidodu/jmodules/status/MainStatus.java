package org.andreidodu.jmodules.status;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class MainStatus {
    @Getter
    private final List<String> jarList = new ArrayList<>();
    private Queue<String> detailedList = new ConcurrentLinkedQueue<>();
    private Queue<String> syntheticList = new ConcurrentLinkedQueue<>();

    public MainStatus() {
    }

    public void addFilename(String filename) {
        jarList.add(filename);
    }

    public synchronized void addAllJars(List<String> filenameList) {
        this.jarList.addAll(filenameList);
    }

    public synchronized void clearAllData() {
        jarList.clear();
        detailedList.clear();
        syntheticList.clear();
    }

    public synchronized void addAllFullModuleInfo(Set<String> fullModuleInfo) {
        detailedList.addAll(fullModuleInfo.stream().distinct().toList());
    }

    public synchronized void addModule(Set<String> fullModule) {
        syntheticList.addAll(fullModule.stream()
                .filter(item -> !"not found".equalsIgnoreCase(item))
                .filter(item -> !"JDK removed internal API".equals(item))
                .collect(Collectors.toSet()));
    }

    public Queue<String> getFullModuleInfoSet() {
        return detailedList;
    }

    public Queue<String> getFullModuleSet() {
        return syntheticList;
    }

}
