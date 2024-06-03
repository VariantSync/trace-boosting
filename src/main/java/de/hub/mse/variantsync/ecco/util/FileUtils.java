package de.hub.mse.variantsync.ecco.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.tinylog.Logger;
import java.util.stream.Collectors;

/**
 * General utils
 *
 * @author jabier.martinez
 */
public class FileUtils {
    /**
     * Get lines of a file
     *
     * @param file the file to read
     * @return list of strings
     */
    public static List<String> getLinesOfFile(final File file) throws IOException {
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.lines().collect(Collectors.toList());
        } catch (final IOException e) {
            Logger.error("Was not able to read lines of " + file + ": ", e);
            throw e;
        }
    }

    /**
     * Get string
     *
     * @param file The file to read
     * @return The lines of the file in a single string
     */
    public static String getStringOfFile(final File file) throws IOException {
        final StringBuilder string = new StringBuilder();
        for (final String line : getLinesOfFile(file)) {
            string.append(line).append("\n");
        }
        string.setLength(string.length() - 1);
        return string.toString();
    }

    /**
     * Get all files recursively (not folders) inside one folder. Or the file
     * itself if the input is a file. Ignore the staging folder.
     *
     * @param dir The directory in which to look
     * @return list of .java files in (sub-) directories
     */
    public static List<File> getAllJavaFilesIgnoringStagingFolder(final File dir) {
        return getAllJavaFiles(dir, true);
    }

    /**
     * Get all files recursively (not folders) inside one folder. Or the file
     * itself if the input is a file.
     *
     * @param dir The directory that is considered
     * @return List of .java file in (sub-) directories
     */
    public static List<File> getAllJavaFiles(final File dir) {
        return getAllJavaFiles(dir, false);
    }

    private static List<File> getAllJavaFiles(final File file, final boolean ignoreStagingFolder) {
        final List<File> files = new LinkedList<>();

        if (!file.isDirectory()) {
            if (file.getName().endsWith(".java")) {
                files.add(file);
            }
        } else {
            for (final File f : Objects.requireNonNull(file.listFiles())) {
                if (!(f.getName().equalsIgnoreCase("staging") && ignoreStagingFolder)) {
                    files.addAll(getAllJavaFiles(f, true));
                }
            }
        }
        return files;
    }

    /**
     * Append text to file
     *
     * @param file The file to append to
     * @param text The text that is to be appended
     * @throws IOException If appending to the file is not possible.
     */
    public static void appendToFile(final File file, final String text) throws IOException {
        try (final BufferedWriter output = new BufferedWriter(new FileWriter(file, true))) {
            output.append(text);
            output.newLine();
        } catch (final IOException e) {
            Logger.error("Was not able to append line to file " + file + ": ", e);
            throw e;
        }
    }

    /**
     * No append, just overwrite with new text
     *
     * @param file The file to write to
     * @param text The text which is to be written
     * @throws IOException If writing is not possible.
     */
    public static void writeFile(final File file, final String text) throws IOException {
        try (final BufferedWriter output = new BufferedWriter(new FileWriter(file, false))) {
            output.append(text);
        } catch (final IOException e) {
            Logger.error("Was not able to write to file " + file + ":", e);
            throw e;
        }
    }

    /**
     * Copy file content (not directory) inside another file. It will replace
     * the content if it already exists.
     *
     * @param sourceFile      The file that is to be copied
     * @param destinationFile The path to the copied file
     */
    public static void copyFile(final File sourceFile, final File destinationFile) throws IOException {
        if (sourceFile.isDirectory()) {
            throw new IllegalArgumentException("This method should only be used with files!");
        }
        Logger.info("Copying " + sourceFile + " to " + destinationFile);
        if (destinationFile.mkdirs()) {
            Logger.info("Created parent directories for " + destinationFile);
        }
        try {
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            Logger.error("Was not able to copy file " + sourceFile + " to " + destinationFile, e);
            throw e;
        }
    }
}
