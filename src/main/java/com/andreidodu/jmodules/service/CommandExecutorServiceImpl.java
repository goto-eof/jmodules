package com.andreidodu.jmodules.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandExecutorServiceImpl {
    public static final String LINUX_JDEPS_COMMAND = "/usr/lib/jvm/java-21-openjdk-amd64/bin/jdeps";
    public static final String WINDOWS_JDEPS_COMMAND = "jdeps";

    public static final String JDEPS_COMMAND_MULTIRELEASE = "--multi-release";
    public static final String JDEPTS_COMMAND_OPTION = "-s";

    public static final String LINUX_MVN_COMMAND = "/usr/share/maven/bin/mvn";
    public static final String WINDOWS_MVN_COMMAND = "mvn";

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isLinux() {
        return OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public Set<String> executeJDepsCommand(String javaVersion, String jar) {
        return new HashSet<>(execute(getJDepsExecutable(), JDEPS_COMMAND_MULTIRELEASE, javaVersion, JDEPTS_COMMAND_OPTION, jar));
    }

    private String getJDepsExecutable() {
        String jdepsCommand = "jdeps";
        if (isLinux() || isMac()) {
            jdepsCommand = getBinPathIfIsSnapOrDefault(LINUX_JDEPS_COMMAND);
        } else if (isWindows()) {
            jdepsCommand = WINDOWS_JDEPS_COMMAND;
        }
        return jdepsCommand;
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
            System.out.println();


            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            List<String> listError = new ArrayList<>();
            String lineError;
            while ((lineError = errorReader.readLine()) != null) {
                listError.add(line);
            }
            System.err.println("Error: " + String.join(" ", listError));

            System.out.println("Result: " + list);
            reader.close();
            process.destroy();
            return list;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    private String getBinPathIfIsSnapOrDefault(String binPath) {
        if (System.getenv("SNAP") != null) {
            return String.format("%s/%s", System.getenv("SNAP"), binPath);
        }
        return binPath;
    }

    private String getMVNExecutable() {
        String jdepsCommand = "mvn";
        if (isLinux() || isMac()) {
            jdepsCommand = getBinPathIfIsSnapOrDefault(LINUX_MVN_COMMAND);
        } else if (isWindows()) {
            jdepsCommand = WINDOWS_MVN_COMMAND;
        }
        return jdepsCommand;
    }

    public String executeMavenCopyLibraries(String absolutePath) throws IOException {
        String exportPath = System.getProperty("user.home") + File.separator + "com.andreidodu.jmodules" + File.separator + "lib";
        Files.createDirectories(Path.of(exportPath));
        File[] files = new File(exportPath).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        this.execute(getMVNExecutable(), "dependency:copy-dependencies", "-f", absolutePath, "-DoutputDirectory=" + exportPath);
        return exportPath;
    }
}
