package de.hub.mse.variantsync.ecco.threading;

import de.hub.mse.variantsync.ecco.data.*;
import de.hub.mse.variantsync.ecco.data.specialization.CAST;
import de.hub.mse.variantsync.ecco.data.specialization.ESupportedLanguages;
import de.hub.mse.variantsync.ecco.data.specialization.JavaAST;
import de.hub.mse.variantsync.ecco.data.specialization.LineAST;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ProductInitializationTask implements Callable<ProductInitializationTask.InitResult> {
    private final int productNumber;
    private final String productName;
    private final File configPath;
    private final File sourcePath;
    private final EccoSet<Feature> allFeatures;
    private final ESupportedLanguages targetLanguage;

    public ProductInitializationTask(final int productNumber, final ProductPassport passport, final ESupportedLanguages targetLanguage) {
        this.productNumber = productNumber;
        this.productName = passport.getName();
        this.configPath = passport.getConfiguration().toFile();
        this.sourcePath = passport.getSourcesRoot().toFile();
        this.allFeatures = new EccoSet<>();
        this.targetLanguage = targetLanguage;
    }

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
            switch (targetLanguage) {
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
                    throw new IllegalStateException("Unexpected value: " + targetLanguage);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Was not able to parse " + sourcePath, e);
        }

        return new InitResult(productNumber, allFeatures, new Product(productName, new EccoSet<>(), productAst, productFeatures));
    }

    public static class InitResult {
        public final int id;
        public final EccoSet<Feature> allFeatures;
        public final Product product;

        public InitResult(final int id, final EccoSet<Feature> allFeatures, final Product product) {
            this.id = id;
            this.allFeatures = allFeatures;
            this.product = product;
        }
    }

}
