package de.hub.mse.variantsync.ecco.threading;

import de.hub.mse.variantsync.ecco.data.Product;
import org.tinylog.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A class representing a task to save a product to a file.
 * This class implements the Runnable interface to be executed as a thread.
 */
public class ProductSaveTask implements Runnable {
    private static int processedCount = 0;
    private final Product product;
    private final String folderName;
    private final int id;

    /**
     * Constructor for ProductSaveTask.
     * 
     * @param product    The product to be saved.
     * @param folderName The name of the folder where the product will be saved.
     * @param id         The unique identifier of the product.
     */
    public ProductSaveTask(final Product product, final String folderName, final int id) {
        this.product = product;
        this.folderName = folderName;
        this.id = id;
    }

    /**
     * Saves the product to a file.
     * Creates directories if they do not exist and writes the product object to a
     * file.
     */
    @Override
    public void run() {
        final String filePath = folderName + "/" + product.getName().replaceAll("Variant", "product-") + ".product";
        synchronized (ProductSaveTask.class) {
            Logger.info("#" + processedCount + ": Saving product " + id + " to " + filePath);
            processedCount++;
        }
        try {
            Files.createDirectories(Paths.get(folderName));
        } catch (final IOException e) {
            Logger.error("Was not able to create directories for " + folderName, e);
            throw new UncheckedIOException(e);
        }
        try (final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(product);
        } catch (final IOException e) {
            Logger.error("Was not able to write products to " + filePath, e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reset the processed count to zero.
     */
    public static void resetProcessedCount() {
        ProductSaveTask.processedCount = 0;
    }
}
