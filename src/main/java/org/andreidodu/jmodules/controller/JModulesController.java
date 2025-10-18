package org.andreidodu.jmodules.controller;

import org.andreidodu.jmodules.gui.JModuleGUI;
import org.andreidodu.jmodules.helper.ValidationUtil;
import org.andreidodu.jmodules.record.UiRecord;
import org.andreidodu.jmodules.service.CommandExecutorServiceImpl;
import org.andreidodu.jmodules.status.MainStatus;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JModulesController implements JModuleObserver {
    private final static Logger log = LoggerFactory.getLogger(JModulesController.class);
    private final ExecutorService mainExecutor = Executors.newSingleThreadExecutor();
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
        return status.getJarList().stream()
                .map(item -> new File(item).getName())
                .collect(Collectors.toList());
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

        log.debug("{}", fileNames);
        return fileNames.stream().distinct().sorted().collect(Collectors.toList());
    }

    @Override
    public void processJars(UiRecord uiRecord, Function<String, List<String>> engine) {
        mainExecutor.submit(() -> {
            try {
                List<String> jarList = engine.apply(uiRecord.absolutePath());
                SwingUtilities.invokeLater(() -> {
                    gui.rebuildJarJList(jarList);
                    gui.getStatusLabel().setText("Processing jar files. Please wait...");
                });
                processOnMultipleThreads(uiRecord.javaVersion(), this.status.getJarList());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }


    private void processOnMultipleThreads(String javaVersion, List<String> jarFilenameList) {


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
            List<Future<Void>> futures = multithreadedExecutor.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            SwingUtilities.invokeLater(() -> enableButtons(true));
            gui.getStatusLabel().setText("Waiting for user input");
        }

        SwingUtilities.invokeLater(() -> {
            gui.setFinalResult("jpackage --add-modules " + new HashSet<>(status.getFullModuleSet()).stream().sorted().collect(Collectors.joining(",")));
            gui.showDoneMessage();
        });
    }

    private void enableButtons(boolean enable) {
        gui.getSubmitPomButton().setEnabled(enable);
        gui.getSubmitFileButton().setEnabled(enable);
        gui.getSubmitDirectoryButton().setEnabled(enable);
        gui.getJavaReleaseVersionTextField().setEnabled(enable);
        gui.pack();
    }

    private void processItem(String javaVersion, String item, CommandExecutorServiceImpl commandExecutorService) {
        Set<String> fullModuleInfo = commandExecutorService.executeJDepsCommand(javaVersion, item);
        log.debug("{}", fullModuleInfo);
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
        log.debug("{}", modules);
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
            log.error("{}", e.getMessage());
            return List.of();
        }
    }

    @SneakyThrows
    @Override
    public void processPomJars(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        SwingUtilities.invokeLater(() -> {
            enableButtons(false);
            gui.getStatusLabel().setText("Loading jar files. It will take few minutes (download in progress). Please wait...");
        });
        showPleaseWaitWarningMessage();
        processJars(build, this::loadPomJars);
    }

    private static void showPleaseWaitWarningMessage() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "It takes a while to generate the local repo by downloading all dependencies. Please wait.", "Warning", JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override
    public void processDirectoryJars(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        SwingUtilities.invokeLater(() -> {
            enableButtons(false);
            gui.getStatusLabel().setText("Loading jar files. Please wait...");
        });
        processJars(build, this::loadDirectory);
    }

    @Override
    public void processFileJar(UiRecord build) {
        ValidationUtil.validateJavaVersion(build.javaVersion());
        status.clearAllData();
        SwingUtilities.invokeLater(() -> {
            enableButtons(false);
            gui.getStatusLabel().setText("Loading jar files. Please wait...");
        });
        processJars(build, this::loadFileName);
    }

    @Override
    public void shutdown() {
        try {
            multithreadedExecutor.shutdownNow();
            mainExecutor.shutdownNow();
            log.debug("shutdown completed");
        } catch (Exception e) {
            log.error("{}", e);
        }
    }

}
