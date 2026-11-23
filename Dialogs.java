package org.networkcalculus.dnc.SPPAnalyzer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Dimension;
import java.io.File;
import java.util.List;

/** All user interaction (file selection and popups). */
final class Dialogs {
    private Dialogs() {}

    /** @return selected .spplog files, or an empty array if cancelled */
    static File[] chooseSpplogFiles() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setDialogTitle("Select .spplog file(s)");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("SPP log files (*.spplog)", "spplog"));
        return chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFiles() : new File[0];
    }

    static void showError(String title, String message) {
        JOptionPane.showMessageDialog(null, scrollable(message), title, JOptionPane.ERROR_MESSAGE);
    }

    static void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(null, scrollable(message), title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows all warnings for one file at once. @return true if the user acknowledged them and wants to continue */
    static boolean confirmWarnings(String fileName, List<String> warnings) {
        String text = String.join("\n", warnings);
        Object[] options = {"Acknowledge and continue", "Skip this file"};
        int choice = JOptionPane.showOptionDialog(null, scrollable(text),
                "Warnings for " + fileName, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        return choice == 0;
    }

    private static JScrollPane scrollable(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560, Math.min(300, 40 + 18 * text.split("\n").length)));
        sp.setBorder(null);
        return sp;
    }
}