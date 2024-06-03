package de.hub.mse.variantsync.boosting.data;

import java.nio.file.Path;

public class ProductPassport {
    private final String name;
    private final Path sourcesRoot;
    private final Path configuration;

    public ProductPassport(final String name, final Path sourcesRoot, final Path configuration) {
        this.name = name;
        this.sourcesRoot = sourcesRoot;
        this.configuration = configuration;
    }

    public Path getSourcesRoot() {
        return sourcesRoot;
    }

    public Path getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }
}
