package org.variantsync.boosting.product;

import org.tinylog.Logger;

import org.variantsync.boosting.datastructure.EccoSet;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.parsing.AbstractAST;
import org.variantsync.boosting.parsing.CAST;
import org.variantsync.boosting.parsing.ESupportedLanguages;
import org.variantsync.boosting.parsing.JavaAST;
import org.variantsync.boosting.parsing.LineAST;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A Callable task for initializing a product with the given parameters.
 * 
 */
public class ProductInitializationTask implements Callable<ProductInitializationTask.InitResult> {
    private final int productNumber;
    private final String productName;
    private final File configPath;
    private final File sourcePath;
    private final EccoSet<Feature> allFeatures;
    private final ESupportedLanguages usedLanguage;

    /**
     * Initializes a ProductInitializationTask with the given product number,
     * product passport, and target language.
     * 
     * @param productNumber  the product number to assign to the task
     * @param passport       the product passport containing information about the
     *                       product
     * @param targetLanguage the target language for the task
     */
    public ProductInitializationTask(final int productNumber, final ProductPassport passport,
            final ESupportedLanguages targetLanguage) {
        this.productNumber = productNumber;
        this.productName = passport.getName();
        this.configPath = passport.getConfiguration().toFile();
        this.sourcePath = passport.getSourcesRoot().toFile();
        this.allFeatures = new EccoSet<>();
        this.usedLanguage = targetLanguage;
    }

    /**
     * Parses the sources and creates an InitResult with the created product.
     * 
     * This method reads a configuration file specified by the configPath parameter,
     * parses the features listed in the file,
     * creates a product AST based on the target language specified, and returns an
     * InitResult object containing the parsed information.
     *
     * @return InitResult - an object containing the parsed information
     * @throws Exception - if an error occurs during parsing or creating the
     *                   InitResult object
     */
    @Override
    public InitResult call() throws Exception {
        Logger.info("Parsing variant " + productNumber + "...");
        List<String> featureStrings = new ArrayList<>();
        if (configPath.exists()) {
            try {
                featureStrings = Files.readAllLines(configPath.toPath());
            } catch (final IOException ex) {
                Logger.error("Was not able to load config:", ex);
                throw new UncheckedIOException(ex);
            }
        }
        final EccoSet<Feature> productFeatures = new EccoSet<>();
        for (final String featureString : featureStrings) {
            final Feature newFeature = new Feature(featureString);
            productFeatures.add(newFeature);
            allFeatures.add(newFeature);
        }
        final AbstractAST productAst;
        try {
            switch (usedLanguage) {
                case C:
                    productAst = new CAST(sourcePath);
                    break;
                case JAVA:
                    productAst = new JavaAST(sourcePath);
                    break;
                case LINES:
                    productAst = new LineAST(sourcePath);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + usedLanguage);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Was not able to parse " + sourcePath, e);
        }

        return new InitResult(productNumber, allFeatures,
                new Product(productName, new EccoSet<>(), productAst, productFeatures));
    }

    /**
     * Represents the result of initializing a product.
     * Contains the ID of the initialization, a set of all features, and the product
     * associated with the initialization.
     */
    public static class InitResult {
        /**
         * The ID of the initialization.
         */
        public final int id;

        /**
         * A set of all features associated with the initialization.
         */
        public final EccoSet<Feature> allFeatures;

        /**
         * The product associated with the initialization.
         */
        public final Product product;

        /**
         * Constructs a new InitResult with the specified ID, set of all features, and
         * product.
         * 
         * @param id          The ID of the initialization.
         * @param allFeatures A set of all features associated with the initialization.
         * @param product     The product associated with the initialization.
         */
        public InitResult(final int id, final EccoSet<Feature> allFeatures, final Product product) {
            this.id = id;
            this.allFeatures = allFeatures;
            this.product = product;
        }
    }

}
