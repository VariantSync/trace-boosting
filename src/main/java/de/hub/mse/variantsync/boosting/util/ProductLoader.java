package de.hub.mse.variantsync.boosting.util;

import de.hub.mse.variantsync.boosting.data.Product;
import de.hub.mse.variantsync.boosting.threading.ProductLoadTask;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ProductLoader implements Iterator<Product> {
    private final List<ProductLoadTask> remainingTasks;
    private final List<Future<ProductLoadTask.LoadResult>> futures;
    private final ExecutorService threadPool;

    public ProductLoader(final List<ProductLoadTask> loadTasks, final int nThreads) {
        this.remainingTasks = new LinkedList<>(loadTasks);
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        this.futures = new LinkedList<>();
        for (int i = 0; i < nThreads; i++) {
            if (!this.remainingTasks.isEmpty()) {
                futures.add(threadPool.submit(remainingTasks.remove(0)));
            }
        }
    }

    @Override
    public boolean hasNext() {
        return !futures.isEmpty();
    }

    @Override
    public Product next() {
        if (!this.remainingTasks.isEmpty()) {
            futures.add(threadPool.submit(remainingTasks.remove(0)));
        } else {
            this.threadPool.shutdown();
        }
        try {
            return futures.remove(0).get().product;
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
