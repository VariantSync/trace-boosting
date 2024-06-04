package de.hub.mse.variantsync.boosting;

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

import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.ecco.Association;
import de.hub.mse.variantsync.boosting.ecco.EccoSet;
import de.hub.mse.variantsync.boosting.ecco.Feature;
import de.hub.mse.variantsync.boosting.ecco.MainTree;
import de.hub.mse.variantsync.boosting.ecco.Module;
import de.hub.mse.variantsync.boosting.parsing.AbstractAST;
import de.hub.mse.variantsync.boosting.parsing.CAST;
import de.hub.mse.variantsync.boosting.parsing.ESupportedLanguages;
import de.hub.mse.variantsync.boosting.parsing.JavaAST;
import de.hub.mse.variantsync.boosting.parsing.LineAST;
import de.hub.mse.variantsync.boosting.product.Product;
import de.hub.mse.variantsync.boosting.product.ProductInitializationTask;
import de.hub.mse.variantsync.boosting.product.ProductLoadTask;
import de.hub.mse.variantsync.boosting.product.ProductLoader;
import de.hub.mse.variantsync.boosting.product.ProductPassport;
import de.hub.mse.variantsync.boosting.product.ProductSaveTask;

public class TraceBoosting {

    private static final DNFFactorization dnf_simplifier_1 = new DNFFactorization();
    private static final DNFSubsumption dnf_simplifier_2 = new DNFSubsumption();
    public static FormulaFactory f = new FormulaFactory();
    static {
        final var builder = CNFConfig.builder();
        builder.algorithm(CNFConfig.Algorithm.FACTORIZATION);
        f.putConfiguration(builder.build());
    }

    public static FormulaFactory getFormulaFactory() {
        return f;
    }

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

    private static void wait(final Collection<Future<?>> futures) throws ExecutionException, InterruptedException {
        for (final Future<?> f : futures) {
            f.get();
        }
    }

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

    private static void writePath(final String filename, final List<String> traces,
            final String outputFolder) {
        if (!filename.equals(".txt")) {
            Logger.info("Writing traces to " + filename);
            final File moduleFile = new File(outputFolder + filename);
            try {
                Files.createDirectories(Paths.get(outputFolder));
                if (moduleFile.createNewFile()) {
                    Logger.debug("Created new file " + moduleFile);
                } else {
                    Logger.debug("File " + moduleFile + " already exists.");
                }
            } catch (final IOException e) {
                Logger.error(
                        "Was not able to create directories and file on path: " + moduleFile, e);
                throw new UncheckedIOException(e);
            }

            try (final FileWriter myWriter = new FileWriter(moduleFile, true)) {
                // Write all traces
                for (final var trace : traces) {
                    if (!trace.equals("") && !trace.equals(" Refinement")) {
                        myWriter.write(trace + "\n");
                    }
                }
            } catch (final IOException e) {
                Logger.error("Was not able to write to file " + moduleFile, e);
                throw new UncheckedIOException(e);
            }
        } else {
            Logger.info("Cannot write traces to " + filename);
        }
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

    public void evaluate(final AbstractAST mainTree, final String outputFolder) {
        Logger.info("start evaluation");
        final Map<String, List<String>> fileToTraceMap = new HashMap<>();
        for (final ASTNode astNode : mainTree.getAstNodes()) {
            final EccoSet<Formula> mappings = astNode.getMappings();
            for (final Formula mapping : mappings) {
                // if the mapping is "TRUE"
                if (mapping.literals().isEmpty()) {
                    continue;
                }
                final String trace = getTrace(astNode, mapping);
                final String fileName = getFilename(mapping);
                if (fileToTraceMap.containsKey(fileName)) {
                    final List<String> traces = fileToTraceMap.get(fileName);
                    traces.add(trace);
                } else {
                    final List<String> traces = new LinkedList<>();
                    traces.add(trace);
                    fileToTraceMap.put(fileName, traces);
                }
            }
        }
        Logger.info("start writing");
        for (final String fileName : fileToTraceMap.keySet()) {
            writePath(fileName, fileToTraceMap.get(fileName), outputFolder);
        }
        sortOutputFiles(outputFolder);
    }

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

    public MainTree computeMappings() {
        allFeatures = new EccoSet<>();
        final AbstractAST mainAST;
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
        final MainTree mainTree = new MainTree(mainAST);
        final EccoSet<Association> associations = traceExtractionAlgorithm(mainTree);
        assignProactiveTraces(associations);

        // translates mappings from associations back to products
        Logger.info("Translating mappings from associations back to products...");
        for (final Association association : associations) {
            if (association.getMapping() == null) {
                // Only calculate a mapping for associations that have no proactive mapping yet
                determineAssociationMapping(association);
            }

            // now put mappings from associations back on individual nodes
            for (final ASTNode node : association.getAstNodes()) {
                if (node.getMapping() == null) {
                    node.setMapping(association.getMapping());
                }
            }
        }
        Logger.info("...done.");
        // result file now contains products with mapped ASTs
        return mainTree;
    }

    public String[] getPaths() {
        return new String[] { getInputFolder(), getInputFile(), getResultsFolder(),
                getResultsFile() };
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(final String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(final String inputFile) {
        this.inputFile = inputFile;
    }

    public String getResultsFolder() {
        return resultsFolder;
    }

    public void setResultsFolder(final String resultsFolder) {
        this.resultsFolder = resultsFolder;
    }

    public String getResultsFile() {
        return resultsFile;
    }

    public void setResultsFile(final String resultsFile) {
        this.resultsFile = resultsFile;
    }

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

    private void determineAssociationMapping(Association association) {
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

    private List<ProductInitializationTask> startProductCreation() {
        Logger.info("Creating products");
        final Product[] products = new Product[sourceLocations.size()];

        final List<ProductInitializationTask> tasks = new ArrayList<>(products.length);
        for (int i = 0; i < sourceLocations.size(); i++) {
            tasks.add(new ProductInitializationTask(i, sourceLocations.get(i), targetLanguage));

        }
        return tasks;
    }

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
