package com.andreidodu.jmodules.helper;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class GuiHelper {


    public static String getPomFile(JFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().equals("pom.xml") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "maven";
            }
        });

        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            return selectedDir.getAbsolutePath();

        }
        return null;
    }

    public static String getFile(JFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".jar") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "JAR file";
            }
        });

        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();

            return selectedDir.getAbsolutePath();

        }

        return null;
    }

    public static String getDirectory(JFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();

            return selectedDir.getAbsolutePath();


        }
        return null;
    }
}
