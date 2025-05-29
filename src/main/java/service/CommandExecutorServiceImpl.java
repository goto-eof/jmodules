package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CommandExecutorServiceImpl {
    public static final String JDEPS_COMMAND = "jdeps";
    public static final String JDEPS_COMMAND_MULTIRELEASE = "--multi-release";
    public static final String JDEPTS_COMMAND_OPTION = "-s";

    public Set<String> executeJDepsCommand(String javaVersion, String jar) {
        return new HashSet<>(execute(JDEPS_COMMAND, JDEPS_COMMAND_MULTIRELEASE, javaVersion, JDEPTS_COMMAND_OPTION, jar));
    }

    private List<String> execute(final String... args) {
        try {
            System.out.println("Executing command: " + String.join(" ", args));
            ProcessBuilder builder = new ProcessBuilder(args);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> list = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            process.waitFor();
            System.out.println("Exiting process: " + process.exitValue());
            System.out.println("Result: " + list);
            return list;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public String executeMavenCopyLibraries(String absolutePath) throws IOException {
        String exportPath = System.getProperty("java.io.tmpdir") + File.separator + "jdeps-gui" + File.separator + "lib";
        Files.createDirectories(Path.of(exportPath));
        File[] files = new File(exportPath).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        this.execute("mvn", "dependency:copy-dependencies", "-f", absolutePath, "-DoutputDirectory=" + exportPath);
        return exportPath;
    }
}
