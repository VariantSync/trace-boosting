package org.variantsync.boosting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.transformations.cnf.CNFConfig;
import org.logicng.transformations.dnf.DNFFactorization;
import org.logicng.transformations.dnf.DNFSubsumption;
import org.tinylog.Logger;

import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.Association;
import org.variantsync.boosting.datastructure.EccoSet;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.datastructure.MainTree;
import org.variantsync.boosting.datastructure.Module;
import org.variantsync.boosting.parsing.AbstractAST;
import org.variantsync.boosting.parsing.CAST;
import org.variantsync.boosting.parsing.ESupportedLanguages;
import org.variantsync.boosting.parsing.JavaAST;
import org.variantsync.boosting.parsing.LineAST;
import org.variantsync.boosting.product.Product;
import org.variantsync.boosting.product.ProductInitializationTask;
import org.variantsync.boosting.product.ProductLoadTask;
import org.variantsync.boosting.product.ProductLoader;
import org.variantsync.boosting.product.ProductPassport;
import org.variantsync.boosting.product.ProductSaveTask;

/**
 * The {@code TraceBoosting} class encapsulates the algorithm for enhancing
 * retroactive comparison-based feature tracing with proactively collected feature traces.
 * It is designed to work with a set of product variants and their associated
 * feature traces to produce a more accurate and efficient tracing process.
 * The algorithm is inspired by the <a href="https://jku-isse.github.io/ecco/">ECCO tracing algorithm</a>
 * and is tailored to improve it by utilizing additional trace information.
 *
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>{@code
 * // Initialize a list to hold ProductPassport objects that describe the
 * // artifact locations for each variant
 * List<ProductPassport> productPassports = new ArrayList<>();
 *
 * // Iterate the collection of variants
 * for (Variant variant : variants) {
 *     String variantName = variant.getName();
 *     // Create a new ProductPassport for the variant and add it to the list
 *     productPassports.add(new ProductPassport(variantName,
 *             variantsDirectory.resolve(variantName), configFileMap.get(variantName)));
 * }
 *
 * // Instantiate the TraceBoosting algorithm with the product passports,
 * // working directory, and the supported language for tracing.
 * // LINES creates a simple line-based AST that is language-agnostic.
 * TraceBoosting traceBoosting = new TraceBoosting(productPassports,
 *         workingDirectory, ESupportedLanguages.LINES);
 *
 * // Retrieve the list of products from the TraceBoosting instance
 * List<Product> products = traceBoosting.getProducts();
 *
 * // Apply the proactively collected traces to the products
 * distributeMappings(products, variantGenerationResult.variantGroundTruthMap(),
 *         percentage, config.getStrip());
 *
 * // Compute the main tree which represents the AST resulting from merging all variants
 * // together with feature traces
 * MainTree mainTree = traceBoosting.compute();
 * }</pre>
 *
 * <p>
 * Note: The actual implementation of methods like {@code distributeMappings}
 * and {@code compute} are not shown in this example and should be defined
 * elsewhere in the codebase.
 * </p>
 */
public class TraceBoosting {

    private static final DNFFactorization dnf_simplifier_1 = new DNFFactorization();
    private static final DNFSubsumption dnf_simplifier_2 = new DNFSubsumption();
    public static FormulaFactory f = new FormulaFactory();

    /**
     * Static block to initialize the FormulaFactory object with a CNFConfig object
     * that specifies the simplification algorithm.
     */
    static {
        final var builder = CNFConfig.builder();
        builder.algorithm(CNFConfig.Algorithm.FACTORIZATION);
        f.putConfiguration(builder.build());
    }

    /**
     * Returns the FormulaFactory instance that is currently in use.
     * 
     * @return the FormulaFactory instance that is currently in use
     */
    public static FormulaFactory getFormulaFactory() {
        return f;
    }

    /**
     * Saves the main tree object to a file in the specified folder.
     *
     * @param mainTree   The MainTree object to be saved
     * @param folderName The name of the folder where the file will be saved
     * @throws UncheckedIOException If an IOException occurs while creating
     *                              directories or writing the object to the file
     */
    public static void saveMainTree(final MainTree mainTree, final String folderName) {
        final String filePath = folderName + "/main-tree.ast";
        Logger.info("Saving main tree to " + filePath);
        try {
            Files.createDirectories(Paths.get(folderName));
        } catch (final IOException e) {
            Logger.error("Was not able to create directories for " + folderName, e);
            throw new UncheckedIOException(e);
        }
        try (final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(mainTree);
        } catch (final IOException e) {
            Logger.error("Was not able to write products to " + filePath, e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads the main tree from the specified folder.
     *
     * @param folderName the name of the folder containing the main tree file
     * @return the MainTree object loaded from the file
     * @throws UncheckedIOException if an IOException occurs while reading the file
     * @throws RuntimeException     if the MainTree class is not found
     */
    public static MainTree loadMainTree(final String folderName) {
        final String filePath = folderName + "/main-tree.ast";
        Logger.info("Loading main tree from " + filePath);
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            return (MainTree) in.readObject();
        } catch (final IOException e) {
            Logger.error("Was not able to read file: ", e);
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            Logger.error("MainTree class not found", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits for all the futures in the given collection to complete.
     * 
     * @param futures a collection of Future objects to wait for
     * @throws ExecutionException   if any of the futures encounter an exception
     *                              during execution
     * @throws InterruptedException if the current thread is interrupted while
     *                              waiting
     */
    private static void wait(final Collection<Future<?>> futures) throws ExecutionException, InterruptedException {
        for (final Future<?> f : futures) {
            f.get();
        }
    }

    /**
     * Generates a filename based on the given formula mapping.
     * 
     * @param mapping The formula mapping to generate the filename from.
     * @return A string representing the generated filename.
     */
    private static String getFilename(final Formula mapping) {
        final StringBuilder name = new StringBuilder();
        int counter = 0;
        for (final Literal literal : mapping.literals()) {
            if (counter > 0) {
                name.append("_and_");
            }
            if (!literal.phase()) {
                name.append("not_");
            }
            name.append(literal.variable().toString());
            counter++;
        }
        name.append(".txt");
        return String.valueOf(name);
    }

    private static void sortOutputFiles(final String outputFolder) {
        Logger.debug("Sorting output files in " + outputFolder);
        final File output = new File(outputFolder);
        final File[] outputs = output.listFiles();
        for (final File file : Objects.requireNonNull(outputs)) {
            if (!file.isFile()) {
                continue;
            }
            List<String> outputList;
            try {
                outputList = Files.readAllLines(file.toPath());
                final Set<StringWrapper> outputSet = outputList.stream().map(StringWrapper::new)
                        .collect(Collectors.toSet());
                outputList = outputSet.stream().map(StringWrapper::getString)
                        .collect(Collectors.toList());
                Collections.sort(outputList);
            } catch (final IOException ex) {
                Logger.error("Was not able to load output files", ex);
                throw new UncheckedIOException(ex);
            }
            try (final FileWriter myWriter = new FileWriter(file, false)) {
                for (final String outputLine : outputList) {
                    myWriter.write(outputLine + "\n");
                }
            } catch (final IOException e) {
                Logger.error("Was not able to sort output files: ", e);
                throw new UncheckedIOException(e);
            }

        }
    }

    private static String getTrace(final ASTNode astNode, final Formula mapping) {
        final String result;
        if (astNode.getType() != ASTNode.NODE_TYPE.CLASS_OR_INTERFACE_DECLARATION
                && astNode.getType() != ASTNode.NODE_TYPE.METHOD_DECLARATION) {
            // node could possibly be a refinement
            result = getName(" Refinement", astNode, false, mapping);
        } else {
            result = getName("", astNode, true, mapping);
        }
        return result;
    }

    private static String getName(String suffix, final ASTNode astNode, boolean insideClass,
            final Formula mapping) {
        if (astNode.getParent() != null) {
            String missing_blank_space = "";
            switch (astNode.getType()) {
                case METHOD_DECLARATION:
                    missing_blank_space = " ";
                case CLASS_OR_INTERFACE_DECLARATION:
                    insideClass = true;
                    // Check whether the class/method declaration has the same mapping
                    if (astNode.getMappings().contains(mapping)) {
                        // when method/class is present (i.e. has the same mapping), we have a
                        // method/class qualified name (=> no refinement)
                        suffix = missing_blank_space + astNode.getCode();
                    } else {
                        // when method/class is NOT present, we have a method/class qualified name +
                        // "Refinement" Tag (=> keep suffix)
                        suffix = missing_blank_space + astNode.getCode() + suffix;
                    }
                    break;
                case FOLDER:
                    suffix = astNode.getCode() + "." + suffix;
                    break;
                default:
                    break;
            }
            return getName(suffix, astNode.getParent(), insideClass, mapping);
        } else {
            // return an empty String when node is outside a java class
            return insideClass ? suffix : "";
        }
    }

    private static EccoSet<Module> featuresToModules(final EccoSet<Feature> positiveFeatures,
                                                     final EccoSet<Feature> negativeFeatures) {
        final EccoSet<Module> result = new EccoSet<>();
        final EccoSet<EccoSet<Feature>> positivePowerSet = positiveFeatures.powerSet();
        final EccoSet<EccoSet<Feature>> negativePowerSet = negativeFeatures.powerSet();

        // Create all possible modules
        for (final EccoSet<Feature> posSet : positivePowerSet) {
            if (posSet.isEmpty()) {
                continue;
            }
            for (final EccoSet<Feature> negSet : negativePowerSet) {
                final EccoSet<Literal> literals = new EccoSet<>();
                posSet.stream().map(feature -> f.literal(feature.getName(), true))
                        .forEach(literals::add);
                negSet.stream().map(feature -> f.literal(feature.getName(), false))
                        .forEach(literals::add);
                result.add(new Module(literals));
            }
        }

        return result;
    }

    private static EccoSet<Module> updateModules(final EccoSet<Module> moduleSet,
            final EccoSet<Feature> negativeFeatures) {
        final EccoSet<Module> result = new EccoSet<>();
        final EccoSet<EccoSet<Feature>> negativePowerSet = negativeFeatures.powerSet();
        for (final Module module : moduleSet) {
            for (final EccoSet<Feature> negSet : negativePowerSet) {
                final EccoSet<Literal> negLiterals = new EccoSet<>();
                negSet.stream().map(feature -> f.literal(feature.getName(), false))
                        .forEach(negLiterals::add);
                result.add(new Module(module.getLiterals().unite(negLiterals)));
            }
        }
        return result;
    }

    private EccoSet<Feature> allFeatures;

    private int nThreads = Runtime.getRuntime().availableProcessors();

    private final List<ProductPassport> sourceLocations;

    private final ESupportedLanguages targetLanguage;

    /*
     * Set mapping_calculation to "CNF".
     * TraceBoosting uses a heuristic to simplify the mapping in a sensible way. As
     * in option 1, TraceBoosting takes the disjunction of the configurations
     * associated with a piece of code. TraceBoosting then simplifies the resulting
     * formula to a CNF formula.
     */
    public String mapping_calculation = "CNF";

    private String inputFolder, inputFile, resultsFolder, resultsFile;

    private final Path workingDir;

    private final List<ProductInitializationTask> productInitTasks;

    private final List<Product> products = new ArrayList<>();

    public TraceBoosting(final List<ProductPassport> sourceLocations, final Path workingDir,
            final ESupportedLanguages targetLanguage) {
        this.sourceLocations = sourceLocations;
        this.targetLanguage = targetLanguage;
        this.workingDir = workingDir;
        setPaths("input", "input", "results", "result");
        this.productInitTasks = initialize();
    }

    public EccoSet<Feature> getAllFeatures() {
        return allFeatures;
    }

    public void setNumThreads(final int numThreads) {
        Logger.info("Updating thread pool...");
        Logger.info("Shutting down old pool");
        Logger.info("Created new pool with " + numThreads + " threads.");
        this.nThreads = numThreads;
    }

    public void saveProducts(final Product[] products, final String folderName) {
        Logger.info("Saving products to " + folderName);
        final List<Future<?>> futures = new ArrayList<>(products.length);
        ExecutorService threadPool = Executors.newFixedThreadPool(this.nThreads);
        try {
            for (int i = 0; i < products.length; i++) {
                final ProductSaveTask task = new ProductSaveTask(products[i], folderName, i);
                futures.add(threadPool.submit(task));
            }
            wait(futures);
        } catch (ExecutionException | InterruptedException e) {
            Logger.error("threading: ", e);
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
        ProductSaveTask.resetProcessedCount();
        Logger.info("Saved all products.");
    }

    public List<Product> getProducts() {
        // Multi-threaded loading of products
        if (!this.productInitTasks.isEmpty()) {
            ExecutorService threadPool = Executors.newFixedThreadPool(this.nThreads);
            try {
                // Finish all product initialization tasks and store the products
                this.productInitTasks.stream().map(threadPool::submit).map(f -> {
                    try {
                        return f.get();
                    } catch (final InterruptedException | ExecutionException e) {
                        Logger.error("Was not able to initialize product.", e);
                        throw new RuntimeException(e);
                    }
                }).forEach(result -> {
                    this.products.add(result.product);
                });
            } finally {
                threadPool.shutdown();
            }
        }
        this.productInitTasks.clear();
        return this.products;
    }

    public Product[] loadProducts(final String inputFolder) {
        final ProductLoader loader = prepareProductLoader(inputFolder);
        final List<Product> products = new LinkedList<>();
        loader.forEachRemaining(products::add);
        return products.toArray(new Product[0]);
    }

    public Product[] loadProducts(final Collection<Path> productLocations) {
        final List<ProductLoadTask> tasks = new ArrayList<>();
        for (final Path productPath : productLocations) {
            final ProductLoadTask task = new ProductLoadTask(productPath);
            tasks.add(task);
        }

        final ProductLoader loader = new ProductLoader(tasks, this.nThreads);
        final List<Product> products = new LinkedList<>();
        loader.forEachRemaining(products::add);
        return products.toArray(new Product[0]);
    }

    public ProductLoader prepareProductLoader(final String folderName) {
        Logger.info("Loading all products from " + folderName);
        final Path pathToInput = Path.of(folderName);
        final List<Path> productPaths;
        try {
            productPaths = Files.list(pathToInput).filter(p -> p.toString().endsWith(".product"))
                    .collect(Collectors.toList());
            productPaths.sort(Path::compareTo);
        } catch (final IOException e) {
            Logger.error("Was not able to read input directory.", e);
            throw new RuntimeException(e);
        }
        final List<ProductLoadTask> tasks = new ArrayList<>();
        for (final Path productPath : productPaths) {
            final ProductLoadTask task = new ProductLoadTask(productPath);
            tasks.add(task);
        }

        return new ProductLoader(tasks, this.nThreads);
    }

    /**
     * Initializes the products by creating them from variants and configuration
     * files.
     * 
     * This method collects variant directories, starts the product creation
     * process, and returns a list of ProductInitializationTask objects.
     * 
     * @return A list of ProductInitializationTask objects representing the
     *         initialized products.
     */
    public List<ProductInitializationTask> initialize() {
        // creates products from variants and config files
        Logger.info("Collecting variant dirs...");
        allFeatures = new EccoSet<>();
        final List<ProductInitializationTask> products = startProductCreation();
        // saveProducts(products, inputFolder);
        // Logger.info("Parsing and saving of products complete.");
        Logger.info("...done.");
        return products;
    }

    /**
     * Computes mappings for the given target language and returns a MainTree object
     * containing the mappings.
     * 
     * @return MainTree object containing the mappings
     * @throws IllegalStateException if an unexpected target language is provided
     */
    public MainTree computeMappings() {
        // Initialize variables
        allFeatures = new EccoSet<>();
        final AbstractAST mainAST;

        // Determine mainAST based on targetLanguage
        switch (targetLanguage) {
            case C:
                mainAST = new CAST();
                break;
            case JAVA:
                mainAST = new JavaAST();
                break;
            case LINES:
                mainAST = new LineAST();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + targetLanguage);
        }

        // Create MainTree object with mainAST
        final MainTree mainTree = new MainTree(mainAST);

        // Extract associations using traceExtractionAlgorithm
        final EccoSet<Association> associations = traceExtractionAlgorithm(mainTree);

        // Assign proactive traces to associations
        assignProactiveTraces(associations);

        // Translate mappings from associations back to products
        Logger.info("Translating mappings from associations back to products...");
        for (final Association association : associations) {
            if (association.getMapping() == null) {
                // Calculate mapping for associations without proactive mapping
                determineAssociationMapping(association);
            }

            // Assign mappings from associations to individual nodes
            for (final ASTNode node : association.getAstNodes()) {
                if (node.getMapping() == null) {
                    node.setMapping(association.getMapping());
                }
            }
        }
        Logger.info("Mapping translation complete.");

        // Return MainTree object with mapped ASTs
        return mainTree;
    }

    /**
     * Returns an array of strings containing the paths for the input folder, input
     * file, results folder, and results file.
     * 
     * @return an array of strings containing the paths for the input folder, input
     *         file, results folder, and results file
     */
    public String[] getPaths() {
        return new String[] { getInputFolder(), getInputFile(), getResultsFolder(),
                getResultsFile() };
    }

    /**
     * Gets the input folder path.
     *
     * @return The input folder path as a String.
     */
    public String getInputFolder() {
        return inputFolder;
    }

    /**
     * Sets the input folder path.
     *
     * @param inputFolder The input folder path to set.
     */
    public void setInputFolder(final String inputFolder) {
        this.inputFolder = inputFolder;
    }

    /**
     * Gets the input file name.
     *
     * @return The input file name as a String.
     */
    public String getInputFile() {
        return inputFile;
    }

    /**
     * Sets the input file name.
     *
     * @param inputFile The input file name to set.
     */
    public void setInputFile(final String inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Gets the results folder path.
     *
     * @return The results folder path as a String.
     */
    public String getResultsFolder() {
        return resultsFolder;
    }

    /**
     * Sets the results folder path.
     *
     * @param resultsFolder The results folder path to set.
     */
    public void setResultsFolder(final String resultsFolder) {
        this.resultsFolder = resultsFolder;
    }

    /**
     * Gets the results file name.
     *
     * @return The results file name as a String.
     */
    public String getResultsFile() {
        return resultsFile;
    }

    /**
     * Sets the results file name.
     *
     * @param resultsFile The results file name to set.
     */
    public void setResultsFile(final String resultsFile) {
        this.resultsFile = resultsFile;
    }

    /**
     * Sets the paths for input and results folders and files.
     * 
     * @param inputFolder   the name of the input folder
     * @param inputFile     the name of the input file
     * @param resultsFolder the name of the results folder
     * @param resultsFile   the name of the results file
     * @throws UncheckedIOException if an I/O error occurs while creating
     *                              directories
     */
    public void setPaths(final String inputFolder, final String inputFile,
            final String resultsFolder, final String resultsFile) {
        final String inputFolderPath = workingDir + "/" + inputFolder;
        final String resultsFolderPath = workingDir + "/" + resultsFolder;

        try {
            Files.createDirectories(Path.of(inputFolderPath));
            Files.createDirectories(Path.of(resultsFolderPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        setInputFolder(inputFolderPath);
        setInputFile(inputFile);
        setResultsFolder(resultsFolderPath);
        setResultsFile(resultsFile);
    }

    /**
     * Extracts associations from a given main tree by merging each product AST into
     * the main tree and collecting corresponding main tree nodes in the product for
     * backtracking later on.
     *
     * @param mainTree The main tree to extract associations from
     * @return An EccoSet of Association objects representing the extracted
     *         associations
     */
    public EccoSet<Association> traceExtractionAlgorithm(final MainTree mainTree) {
        int productCount = 0;
        EccoSet<Association> associations = new EccoSet<>();
        for (Product product : this.getProducts()) {
            // merge each product AST into the main tree and collect corresponding main tree
            // nodes
            // in the product for backtracking later on
            Logger.info("Merging product #" + productCount);
            product.setAstNodesMainTree(mainTree.unite(product));
            // Forget the product's AST after the product has been merged. It is no longer
            // needed
            product.forgetAST();

            Logger.info("Considering product " + productCount + "...");
            final EccoSet<Feature> productFeatures = product.getFeatures();
            final EccoSet<Feature> negFeatures = productFeatures.without(allFeatures);
            final EccoSet<Module> modules = featuresToModules(productFeatures, allFeatures.without(productFeatures));
            allFeatures.addAll(productFeatures);
            Association aNew = new Association(modules, modules, modules, new EccoSet<>(),
                    product.getAstNodesMainTree());

            final EccoSet<Association> newAssociations = new EccoSet<>();
            for (final Association association : associations) {
                // Update modules in association
                Association updatedAssociation = new Association(
                        updateModules(association.getMin(), negFeatures),
                        updateModules(association.getAll(), negFeatures),
                        updateModules(association.getMax(), negFeatures),
                        updateModules(association.getNot(), negFeatures),
                        association.getAstNodes());

                // Intersect ASTs
                final EccoSet<ASTNode> intNodes = updatedAssociation.getAstNodes().intersect(aNew.getAstNodes());

                // compute intersection
                final Association aInt = new Association(
                        updatedAssociation.getMin().intersect(aNew.getMin()),
                        updatedAssociation.getAll().unite(aNew.getAll()),
                        updatedAssociation.getMax().unite(aNew.getMax()),
                        updatedAssociation.getNot(), intNodes);
                aInt.setMax(aInt.getAll().without(aInt.getNot()));
                if (!updatedAssociation.isBasic()) {
                    aInt.setBasic(false);
                }

                // set mapping for code appearing in association but in not aNew
                updatedAssociation.removeNodes(intNodes);
                updatedAssociation = new Association(
                        updatedAssociation.getMin().without(aInt.getMin()),
                        updatedAssociation.getAll(), new EccoSet<>(),
                        updatedAssociation.getNot().unite(aNew.getAll()),
                        updatedAssociation.getAstNodes());
                updatedAssociation
                        .setMax(updatedAssociation.getAll().without(updatedAssociation.getNot()));
                // set mapping for code appearing in aNew but in not association
                aNew.removeNodes(intNodes);
                aNew = new Association(aNew.getMin().without(aInt.getMin()), aNew.getAll(),
                        new EccoSet<>(), aNew.getNot().unite(updatedAssociation.getAll()),
                        aNew.getAstNodes());
                aNew.setMax(aNew.getAll().without(aNew.getNot()));

                updatedAssociation.setBasic(false);
                aNew.setBasic(false);

                newAssociations.add(aInt);
                newAssociations.add(updatedAssociation);
            }
            newAssociations.add(aNew);
            associations = newAssociations;
            associations.removeIf(association -> association.getAstNodes().size() == 0);
            productCount++;
            Logger.info("...done.");
        }

        return associations;
    }

    /**
     * Assigns proactive traces to associations in the given EccoSet.
     * 
     * This method uses a thread pool to assign proactive traces to associations in
     * parallel. It checks each association in the EccoSet for existing mappings and
     * sets the mapping for the association based on the number of existing mappings
     * found. If there is only one existing mapping, it sets the mapping for the
     * association. If there are no existing mappings or more than one existing
     * mapping, it does not set the mapping for the association.
     * 
     * @param associations the EccoSet of associations to assign proactive traces to
     * @throws RuntimeException if there is an error while assigning proactive
     *                          traces
     */
    private void assignProactiveTraces(EccoSet<Association> associations) {
        ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Association assoc : associations) {
            futures.add(threadPool.submit(() -> {
                // Formula does not implement equals and hashCode; we have to use Strings to
                // store them :(
                Map<String, Formula> existingMappings = new HashMap<>();
                for (ASTNode node : assoc.getAstNodes()) {
                    var mapping = node.getMapping();
                    if (mapping != null) {
                        existingMappings.putIfAbsent(mapping.toString(), mapping);
                    }
                }
                if (existingMappings.size() == 1) {
                    // One mapping for all
                    for (Formula mapping : existingMappings.values()) {
                        // loops only once
                        assoc.setMapping(mapping);
                    }
                    return 1;
                } else {
                    // Do nothing. If there is no existing mapping, we do not know anything. If
                    // there is more than one, we cannot decide.
                    return 0;
                }
            }));

        }
        int associationUpdates = 0;
        for (var future : futures) {
            try {
                // Wait for all association updates to complete
                associationUpdates += future.get();
            } catch (InterruptedException | ExecutionException e) {
                Logger.error("Was not able to finish task", e);
                throw new RuntimeException(e);
            }
        }
        Logger.info("Updated " + associationUpdates + " of " + associations.size()
                + " associations by propagating mappings.");
        threadPool.shutdown();
    }

    /**
     * Determines the mapping for the given association.
     * 
     * This method calculates the formula for the association based on the smallest
     * min modules or smallest max modules. If the association is basic, it
     * considers the disjunction of all products. Otherwise, it continues with the
     * min modules.
     * 
     * @param association The Association object for which the mapping needs to be
     *                    determined
     * @throws IllegalArgumentException if the association is null
     */
    private void determineAssociationMapping(Association association) {
        if (association == null) {
            throw new IllegalArgumentException("Association cannot be null");
        }

        // Calculate the formula for the association
        List<Module> modules = association.getSmallestMinModules();
        final Formula formula;
        if (modules.isEmpty()) {
            // Consider the disjunction of all products
            modules = association.getSmallestMaxModules();
            if (association.isBasic()) {
                formula = f.verum();
            } else {
                formula = f.or(modules.stream().map(m -> {
                    final EccoSet<Literal> literals = m.getLiterals();
                    return f.cnf(literals);
                }).collect(Collectors.toList()));
            }
        } else {
            // Continue with the min modules
            formula = f.and(modules.stream().map(m -> {
                final EccoSet<Literal> literals = m.getLiterals();
                return f.cnf(literals);
            }).collect(Collectors.toList()));
        }

        // simplify mappings to minimal formulas
        if (mapping_calculation.equals("DNF")) {
            final Formula dnf1 = formula.transform(dnf_simplifier_1);
            final Formula dnf2 = dnf1.transform(dnf_simplifier_2);
            association.setMapping(dnf2);
        } else if (mapping_calculation.equals("CNF")) {
            final Formula cnf = formula.cnf();
            Formula mapping = null;
            boolean unreduced = true;
            // if the formula only consists of one disjunctive clause, it's already the
            // mapping
            if (!cnf.type().equals(FType.AND)) {
                association.setMapping(cnf);
            } else {
                // try to reduce the CNF to clauses that contain no disjunction (=> clauses that
                // are literals)
                for (final Formula clause : cnf) {
                    if (!clause.type().equals(FType.OR)) {
                        if (unreduced) {
                            mapping = clause;
                            unreduced = false;
                        } else {
                            mapping = f.and(mapping, clause);
                        }
                    }
                }
                // if the formula could not be reduced, return to the DNF formula
                if (unreduced) {
                    final Formula dnf1 = formula.transform(dnf_simplifier_1);
                    mapping = dnf1.transform(dnf_simplifier_2);
                }
                association.setMapping(mapping);
            }
        }
    }

    /**
     * Starts the process of creating products by initializing
     * ProductInitializationTasks for each source location.
     * 
     * @return A list of ProductInitializationTasks, one for each source location
     */
    private List<ProductInitializationTask> startProductCreation() {
        Logger.info("Creating products");
        final Product[] products = new Product[sourceLocations.size()];

        final List<ProductInitializationTask> tasks = new ArrayList<>(products.length);
        for (int i = 0; i < sourceLocations.size(); i++) {
            tasks.add(new ProductInitializationTask(i, sourceLocations.get(i), targetLanguage));
        }
        return tasks;
    }

    /**
     * A class representing a wrapper for a String value.
     */
    private static class StringWrapper {
        String value;

        public StringWrapper(final String value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final StringWrapper that = (StringWrapper) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        public String getString() {
            return value;
        }
    }

}
