package de.hub.mse.variantsync.boosting;

import org.tinylog.Logger;

import de.hub.mse.variantsync.boosting.ecco.*;
import de.hub.mse.variantsync.boosting.ecco.Module;
import de.hub.mse.variantsync.boosting.parsing.*;
import de.hub.mse.variantsync.boosting.product.*;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.transformations.cnf.CNFConfig;
import org.logicng.transformations.dnf.DNFFactorization;
import org.logicng.transformations.dnf.DNFSubsumption;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TraceBoosting {

    /*
     * This is how mappings can be changed (if no changes are to be made, only do 1
     * and 7): 1.
     * create an object ecco_light of class TraceBoosting (it will initialize
     * products
     * automatically)
     * 2. after initializing, load the products using
     * loadProducts(ecco_light.getInputFolder(),
     * ecco_light.getInputFile()) 3a. choose a node from Product i using
     * products[i].getNodeFromPosition(position) and assign the mapping that fits
     * the position
     * (compare them by using position.isBefore(otherPosition) and
     * position.isAfter(otherPosition))
     * or 3b. go through all nodes using products[i].getAstNodes() and choose a
     * mapping for each
     * node by comparing node.getStartPosition() to your mapping's position (compare
     * them by using
     * position.isBefore(otherPosition) and position.isAfter(otherPosition)) 4. set
     * the mapping
     * (which must be of type Formula) for the chosen node using
     * node.setMapping(mapping) 5a. repeat
     * 3a and 4 until all mappings have been set 6. save the altered products using
     * saveProducts(products, ecco_light.getInputFolder(),
     * ecco_light.getInputFile()) 7. run the
     * algorithm by ecco_light.computeEcco();
     */

    private static final DNFFactorization dnf_simplifier_1 = new DNFFactorization();
    private static final DNFSubsumption dnf_simplifier_2 = new DNFSubsumption();
    public static FormulaFactory f = new FormulaFactory();
    private EccoSet<Feature> allFeatures;
    private int nThreads = Runtime.getRuntime().availableProcessors();

    static {
        final var builder = CNFConfig.builder();
        builder.algorithm(CNFConfig.Algorithm.FACTORIZATION);
        f.putConfiguration(builder.build());
    }

    // scenario contains the name of the ArgoUML scenario, basDir contains the name
    // of the path
    // leading to the scenario
    private final List<ProductPassport> sourceLocations;
    private final ESupportedLanguages targetLanguage;
    // private Product[] eccoProducts;
    // short explanation: "DNF" for taking the DNF formula and consider every
    // clause, "CNF" for
    // taking the CNF formula and removing clauses containing disjunctions
    /*
     * long explanation: We have implemented two different ways of how ECCO chooses
     * features
     * associated with a piece of code. Each product has a unique configuration
     * (combination of
     * features that are present or not), which is represented as a conjunctive
     * formula of feature
     * literals. Option 1: Set mapping_calculation to "DNF" ECCO associates a piece
     * of code with a
     * configuration if and only if the code appears in a product with that
     * configuration. ECCO
     * takes the disjunction of the associated configurations, since the code
     * appears if either one
     * of them is present. ECCO simplifies the formula as a DNF formula and returns
     * that as the
     * mapping of the code. Now each clause of the mapping is a condition that
     * causes the peace of
     * code to appear. Option 2 (currently used): Set mapping_calculation to "CNF"
     * ECCO uses a
     * heuristic to simplify the mapping in a sensible way. As in option 1, ECCO
     * takes the
     * disjunction of the configurations associated with a piece of code. ECCO then
     * simplifies the
     * resulting formula to a CNF formula. If all clauses in the CNF formula contain
     * disjunctions,
     * ECCO sets the mapping to the simplified DNF formula as in option 1.
     * Otherwise, ECCO reduces
     * the formula to the conjunction of all simple clauses (clauses that contain no
     * disjunctions
     * and are therefore just literals) and returns that as the mapping.. In that
     * case, the mapping
     * consists of one condition that causes the piece of code to appear.
     */
    public String mapping_calculation = "CNF";
    // inputFolder contains the products object (inputFile) java needs (after they
    // have been
    // transformed into ASTs)
    // resultsFolder will contain the products object (resultsFile) with mappings
    // after running ECCO
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

    public static FormulaFactory getFormulaFactory() {
        return f;
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

    // public void saveProducts(
    // final List<Future<ProductInitializationTask.InitResult>> products,
    // final String folderName) {
    // final List<Future<?>> futures = new ArrayList<>(products.size());
    // try(ExecutorService threadPool = Executors.newFixedThreadPool(this.nThreads))
    // {
    // products.stream().map(f -> {
    // try {
    // return f.get();
    // } catch (final InterruptedException | ExecutionException e) {
    // Logger.error("Was not able to initialize product.", e);
    // throw new RuntimeException(e);
    // }
    // }).forEach(result -> {
    // final ProductSaveTask task = new ProductSaveTask(result.product, folderName,
    // result.id);
    // futures.add(threadPool.submit(task));
    // allFeatures.addAll(result.allFeatures);
    // });
    // wait(futures);
    // } catch (ExecutionException | InterruptedException e) {
    // Logger.error("threading: ", e);
    // throw new RuntimeException(e);
    // }
    // ProductSaveTask.resetProcessedCount();
    // Logger.info("Saved all products.");
    // }

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

    private static void wait(final Collection<Future<?>> futures) throws ExecutionException, InterruptedException {
        for (final Future<?> f : futures) {
            f.get();
        }
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

    /*
     * for each class type node add Class qualified name for each method type node
     * add Method
     * qualified name for each statement node where class is non-solid add Class
     * qualified name plus
     * refinement tag for each statement node where method is non-solid add Method
     * qualified name
     * plus refinement tag (non-solid means that the class/method declaration in
     * which the node
     * appears does not have the same mapping)
     */
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

    // private <T extends Callable<V>, V> List<Future<V>> submitToThreadPool(final
    // List<T> tasks) {
    // final List<Future<V>> futures = new ArrayList<>(tasks.size());
    // tasks.forEach(t -> futures.add(threadPool.submit(t)));
    // return futures;
    // }

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

    public MainTree computeEcco() {
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
                    // TODO: Handle more than one mapping
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

        // I tried simplifying all associations at the end - instead of simplifying them
        // on the fly
        // - but it was much slower
        // TODO what does she mean by simplifying here?
        // maybe it was just inefficiently implemented?
        return associations;
    }

    /**
     * Corresponds to f2m in paper.
     */
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

    /**
     * Corresponds to uM function in paper.
     * 
     * @return updated module set
     */
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
