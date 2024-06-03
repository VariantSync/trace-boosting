package de.hub.mse.variantsync.ecco.threading;

import de.hub.mse.variantsync.ecco.data.Product;
import org.tinylog.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProductSaveTask implements Runnable {
    private static int processedCount = 0;
    private final Product product;
    private final String folderName;
    private final int id;

    public ProductSaveTask(final Product product, final String folderName, final int id) {
        this.product = product;
        this.folderName = folderName;
        this.id = id;
    }


    @Override
    public void run() {
        final String filePath = folderName + "/" + product.getName().replaceAll("Variant", "product-") + ".product";
        synchronized (ProductSaveTask.class){
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

    public static void resetProcessedCount() {
        ProductSaveTask.processedCount = 0;
    }
}
