package de.hub.mse.variantsync.boosting.product;

import java.nio.file.Path;

/**
 * Represents a product passport, containing information about the product name,
 * sources root, and configuration path.
 */
public class ProductPassport {
    private final String name;
    private final Path sourcesRoot;
    private final Path configuration;

    /**
     * Constructs a new ProductPassport with the given name, sources root, and
     * configuration path.
     *
     * @param name          the name of the product
     * @param sourcesRoot   the root path for the product sources
     * @param configuration the path to the product configuration
     */
    public ProductPassport(final String name, final Path sourcesRoot, final Path configuration) {
        this.name = name;
        this.sourcesRoot = sourcesRoot;
        this.configuration = configuration;
    }

    /**
     * Returns the root path for the product sources.
     *
     * @return the root path for the product sources
     */
    public Path getSourcesRoot() {
        return sourcesRoot;
    }

    /**
     * Returns the path to the product configuration.
     *
     * @return the path to the product configuration
     */
    public Path getConfiguration() {
        return configuration;
    }

    /**
     * Returns the name of the product.
     *
     * @return the name of the product
     */
    public String getName() {
        return name;
    }
}
