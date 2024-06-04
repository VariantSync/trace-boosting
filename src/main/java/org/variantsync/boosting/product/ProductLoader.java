package org.variantsync.boosting.product;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A class that implements an Iterator for loading products using multiple
 * threads.
 * 
 * This class takes a list of ProductLoadTask objects and a number of threads to
 * use for loading the products.
 * It then creates a thread pool with the specified number of threads and
 * submits tasks to load products.
 */
public class ProductLoader implements Iterator<Product> {
    private final List<ProductLoadTask> remainingTasks;
    private final List<Future<ProductLoadTask.LoadResult>> futures;
    private final ExecutorService threadPool;

    /**
     * Constructs a ProductLoader with the specified list of load tasks and number
     * of threads.
     * 
     * @param loadTasks a list of ProductLoadTask objects representing the tasks to
     *                  load products
     * @param nThreads  the number of threads to use for loading products
     */
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

    /**
     * Checks if there are more products to load.
     * 
     * @return true if there are more products to load, false otherwise
     */
    @Override
    public boolean hasNext() {
        return !futures.isEmpty();
    }

    /**
     * Loads the next product using a thread from the thread pool.
     * 
     * @return the next product to load
     * @throws RuntimeException if an error occurs while loading the product
     */
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
