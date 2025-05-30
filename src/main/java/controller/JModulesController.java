package controller;

import gui.JModuleGUI;
import helper.ValidationUtil;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.UiRecord;
import service.CommandExecutorServiceImpl;
import status.MainStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JModulesController implements JModuleObserver {
    private final static Logger LOGGER = LoggerFactory.getLogger(JModulesController.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService multithreadedExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private MainStatus status;
    private JModuleGUI gui;

    public JModulesController() {
        this.gui = new JModuleGUI(this);
        this.status = new MainStatus();
    }

    @Override
    public List<String> loadFileName(String singleFilename) {
        status.clearAllData();
        this.status.addFilename(singleFilename);
        return status.getJarList().stream().map(item -> new File(item).getName()).collect(Collectors.toList());
    }


    @Override
    public List<String> loadDirectory(String absolutePath) {
        status.clearAllData();
        return loadDirectoryCommon(absolutePath);
    }


    private List<String> loadDirectoryCommon(String absolutePath) {
        List<String> fileNames = new ArrayList<>();

        File absolutePathFile = new File(absolutePath);
        File[] allFiles = absolutePathFile.listFiles();
        if (absolutePathFile.isDirectory()) {
            File[] filesArr = allFiles;
            if (filesArr == null) {
                return new ArrayList<>();
            }

            List<File> files = Arrays.stream(filesArr)
                    .filter(file -> file.getName().endsWith(".jar"))
                    .toList();
            this.status.addAllJars(files.stream()
                    .map(File::getAbsolutePath)
                    .toList());

            fileNames.addAll(files.stream().map(File::getName).collect(Collectors.toSet()));
        }

        for (File file : allFiles) {
            if (file.isDirectory()) {
                fileNames.addAll(loadDirectoryCommon(file.getAbsolutePath()));
            }
        }

        LOGGER.debug("{}", fileNames);
        return fileNames.stream().distinct().sorted().collect(Collectors.toList());
    }

    @Override
    public void processJars(UiRecord uiRecord, Function<String, List<String>> engine) {
        executor.submit(() -> {
            try {
                List<String> jarList = engine.apply(uiRecord.absolutePath());
                SwingUtilities.invokeLater(() -> gui.rebuildJarJList(jarList));
                process(uiRecord.javaVersion(), this.status.getJarList());
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }


    private void process(String javaVersion, List<String> jarFilenameList) {

        SwingUtilities.invokeLater(() -> gui.setProgressBarMax(jarFilenameList.size()));
        SwingUtilities.invokeLater(() -> gui.setProgressBarCurrent(0));


        final CommandExecutorServiceImpl commandExecutorService = new CommandExecutorServiceImpl();

        List<Callable<Void>> callables = new ArrayList<>();

        for (String item : jarFilenameList) {
            Callable<Void> callable = () -> {
                processItem(javaVersion, item, commandExecutorService);
                return null;
            };
            callables.add(callable);
        }

        try {
            multithreadedExecutor.invokeAll(callables);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        SwingUtilities.invokeLater(() -> {
            gui.setFinalResult("jpackage --add-modules " + String.join(",", status.getFullModuleSet()));
            gui.showDoneMessage();
        });
    }

    private void processItem(String javaVersion, String item, CommandExecutorServiceImpl commandExecutorService) {
        Set<String> fullModuleInfo = commandExecutorService.executeJDepsCommand(javaVersion, item);
        LOGGER.debug("{}", fullModuleInfo);
        status.addAllFullModuleInfo(fullModuleInfo);
        status.addModule(parseModules(fullModuleInfo));


        SwingUtilities.invokeLater(() -> {
            gui.incrementProgressBarCurrent();
            gui.updateFullModuleInfo(status.getFullModuleInfoSet().stream().distinct().sorted().toList());
            gui.updateModuleInfo(status.getFullModuleSet().stream().distinct().sorted().toList());
        });
    }

    @Override
    public Set<String> parseModules(Set<String> modules) {
        LOGGER.debug("{}", modules);
        return modules.stream().map(item -> item.split("->")[1].trim()).collect(Collectors.toSet());
    }

    private void copyToClipboard(List<String> list) {
        StringSelection stringSelection = new StringSelection(String.join(",", list));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    @Override
    public List<String> loadPomJars(String absolutePath) {
        try {
            CommandExecutorServiceImpl commandExecutorService = new CommandExecutorServiceImpl();
            String directory = commandExecutorService.executeMavenCopyLibraries(absolutePath);
            return loadDirectoryCommon(directory);
        } catch (Exception e) {
            LOGGER.error("{}", e);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void processPomJars(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        processJars(build, this::loadPomJars);
    }

    @Override
    public void processDirectoryJars(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        processJars(build, this::loadDirectory);
    }

    @Override
    public void processFileJar(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        processJars(build, this::loadFileName);
    }

    @Override
    public void shutdown() {
        try {
            executor.shutdownNow();
            multithreadedExecutor.shutdown();
            LOGGER.debug("shutdown completed");
        } catch (Exception e) {
            LOGGER.error("{}", e);
        }
    }

}
