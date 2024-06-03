package de.hub.mse.variantsync.boosting.product;

import org.tinylog.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A Callable task that loads a product from a given file path.
 * The task reads the product object from the file using ObjectInputStream and
 * returns a LoadResult object.
 * 
 * @param productPath The file path of the product to load
 * @return LoadResult An object containing the loaded product ID and product
 *         object
 * @throws UncheckedIOException If an IOException occurs while reading the file
 * @throws RuntimeException     If the Product class is not found
 */
public class ProductLoadTask implements Callable<ProductLoadTask.LoadResult> {
    private static int processedCount = 0;
    private final Path productPath;
    private final int id;

    /**
     * Constructs a ProductLoadTask with the given product file path.
     * Parses the product ID from the file name.
     * 
     * @param productPath The file path of the product to load
     */
    public ProductLoadTask(final Path productPath) {
        this.productPath = productPath;
        this.id = Integer.parseInt(productPath.getFileName().toString().split("\\.")[0].split("-")[1]);
    }

    /**
     * Loads the product from the file and returns a LoadResult object.
     * 
     * @return LoadResult An object containing the loaded product ID and product
     *         object
     * @throws UncheckedIOException If an IOException occurs while reading the file
     * @throws RuntimeException     If the Product class is not found
     */
    @Override
    public LoadResult call() throws Exception {
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(productPath.toFile()))) {
            synchronized (ProductLoadTask.class) {
                Logger.info("#" + processedCount + ": Loading product " + id + " from " + productPath);
                processedCount++;
            }
            return new LoadResult(id, (Product) in.readObject());
        } catch (final IOException e) {
            Logger.error("Was not able to read file: ", e);
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            Logger.error("Product class not found", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the processed count to zero.
     */
    public static void resetProcessedCount() {
        ProductLoadTask.processedCount = 0;
    }

    /**
     * Represents the result of loading a product, containing the product ID and
     * product object.
     */
    public static class LoadResult {
        public final int id;
        public final Product product;

        public LoadResult(final int id, final Product product) {
            Logger.info("#" + id + ": Finished loading.");
            this.id = id;
            this.product = product;
        }
    }
}
