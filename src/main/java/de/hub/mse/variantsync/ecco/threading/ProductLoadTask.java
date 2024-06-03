package de.hub.mse.variantsync.ecco.threading;

import de.hub.mse.variantsync.ecco.data.Product;
import org.tinylog.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class ProductLoadTask implements Callable<ProductLoadTask.LoadResult> {
    private static int processedCount = 0;
    private final Path productPath;
    private final int id;

    public ProductLoadTask(final Path productPath) {
        this.productPath = productPath;
        // Retrieve the products index in the product array from the file name
        this.id = Integer.parseInt(productPath.getFileName().toString().split("\\.")[0].split("-")[1]);
    }

    @Override
    public LoadResult call() throws Exception {
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(productPath.toFile()))) {
            synchronized (ProductLoadTask.class){
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

    public static void resetProcessedCount() {
        ProductLoadTask.processedCount = 0;
    }

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
