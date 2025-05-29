package status;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class MainStatus {
    @Getter
    private final List<String> jarList = new ArrayList<>();
    private List<String> detailedList = new ArrayList<>();
    private List<String> syntheticList = new ArrayList<>();

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
        detailedList = new ArrayList<>(detailedList.stream().distinct().sorted().toList());
    }

    public synchronized void addModule(Set<String> fullModule) {
        syntheticList.addAll(fullModule.stream()
                .distinct()
                .filter(item -> !"not found".equalsIgnoreCase(item))
                .filter(item -> !"JDK removed internal API".equals(item))
                .collect(Collectors.toSet()));
        syntheticList = new ArrayList<>(syntheticList.stream().distinct().sorted().collect(Collectors.toList()));
    }

    public List<String> getFullModuleInfoSet() {
        return detailedList;
    }

    public List<String> getFullModuleSet() {
        return syntheticList;
    }

}
