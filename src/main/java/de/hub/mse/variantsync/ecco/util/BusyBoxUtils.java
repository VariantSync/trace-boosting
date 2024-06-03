package de.hub.mse.variantsync.ecco.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BusyBoxUtils {

    public static void convertConfig(final Path configPath) throws IOException {
        final Set<String> features = new HashSet<>();

        final List<String> lines = Files.readAllLines(configPath);
        // Read the active features
        for (String line : lines) {
            line = line.trim();
            // We discard all features set to false
            if (line.endsWith("true")) {
                final String featureName = line.split(":")[0].trim();
                features.add(featureName);
            } else if (!line.endsWith("false")) {
                // The file was processed before.
                features.add(line);
            }
        }

        // Overwrite the file
        Files.delete(configPath);
        Files.write(configPath, features);
    }

    public static void convertConfigs(final Path directoryWithConfigs) {
        try {
            Files.list(directoryWithConfigs).filter(p -> p.getFileName().toString().endsWith(".config")).forEach(configPath -> {
                try {
                    convertConfig(configPath);
                } catch (final IOException e) {
                    e.printStackTrace();
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
