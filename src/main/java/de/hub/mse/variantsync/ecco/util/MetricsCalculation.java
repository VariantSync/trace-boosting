package de.hub.mse.variantsync.ecco.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.tinylog.Logger;
import java.util.Objects;

/**
 * Metrics calculation
 * <p>
 * Precision and Recall
 * <p>
 * Precision: Have I retrieved a lot of incorrect results?
 * <p>
 * Recall: Have I found them?
 * <p>
 * F1: Accuracy metric that combines precision and recall
 *
 * @author jabier.martinez
 */
public class MetricsCalculation {

    private static final String EXTENSION = ".txt";
    private static int retrievedNonexistentFeature_counter;
    private static double failedToRetrieve_counter;
    private static double totalPrecision, totalRecall, totalF1;

    public static void computeMetrics(final Path groundTruthDir, final Path scenarioOutputDir) throws IOException {
        System.out.println("Metrics calculation");
        final File groundTruth = groundTruthDir.toFile();
        final File yourResults = scenarioOutputDir.toFile();
        final File yourResultsMetrics = scenarioOutputDir.resolve("evaluation").toFile();
        final String results = getResults(groundTruth, yourResults);

        final File resultsFile = new File(yourResultsMetrics, "resultPrecisionRecall_" + System.currentTimeMillis() + ".csv");
        final File pathToMetrics = scenarioOutputDir.resolve("evaluation").toFile();
        if (pathToMetrics.mkdirs()) {
            Logger.info("Creating directories in path " + pathToMetrics);
        }
        final File metricsFile = scenarioOutputDir.resolve("evaluation/metrics.txt").toFile();

        try {
            FileUtils.writeFile(resultsFile, results);
            FileUtils.writeFile(metricsFile, totalPrecision + ";" + totalRecall + ";" + totalF1);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        System.out.println("Results file created");
        System.out.println("Gnuplot script:");

        System.out.println("cd '" + resultsFile.getAbsoluteFile().getParentFile().getAbsolutePath() + "'");
        System.out.println("set style data boxplot");
        System.out.println("set datafile sep ','");
        System.out.println("set style boxplot outliers pointtype 6");
        System.out.println("set style fill empty");
        System.out.println("set xtics ('Names' 1, 'Precision' 2, 'Recall' 3, 'FScore' 4) scale 0.0");
        System.out.println("set yrange [-0.04:1.04]");
        System.out.println("set title \"Actual features where nothing was retrieved= " + (int) failedToRetrieve_counter
                + " out of 24\\nInexistent features where something was retrieved= "
                + retrievedNonexistentFeature_counter + "\\nMetrics for actual features:\"");
        // [i=2:4] because the first column is for names
        // every ::1::24 to ignore the last row with the global results
        System.out.println("plot for [i=2:4] '" + resultsFile.getName()
                + "' every ::1::24 using (i):i notitle pointsize .8 lc rgb 'black'");
    }

    /**
     * Create results file and store it in the actual folder parent folder
     *
     * @param actualFolder    containing txt files with the actual values
     * @param retrievedFolder containing txt files with the retrieved values
     */
    public static String getResults(final File actualFolder, final File retrievedFolder) throws IOException {
        final StringBuilder resultsContent = new StringBuilder();
        resultsContent.append("Name,Precision,Recall,FScore,FeaturesWithoutRetrieved,InexistentFeaturesRetrieved\n");
        failedToRetrieve_counter = 0;
        final var counts = new ResultCounts(0, 0, 0);
        // Go through the ground-truth folder
        for (final File f : Objects.requireNonNull(actualFolder.listFiles())) {
            // be sure that it is a correct file
            if (f.getName().endsWith(EXTENSION)) {
                final List<String> actualLines = FileUtils.getLinesOfFile(f);
                if (!actualLines.isEmpty()) {
                    final List<String> retrievedLines;
                    // get its counterpart in the retrieved folder
                    final File f2 = new File(retrievedFolder, f.getName());
                    if (f2.exists()) {
                        retrievedLines = FileUtils.getLinesOfFile(f2);
                    } else {
                        // no file was created so it did not find anything
                        retrievedLines = new ArrayList<>();
                    }

                    final String name = f.getName().substring(0, f.getName().length() - EXTENSION.length());
                    updateResult(resultsContent, counts, actualLines, retrievedLines, name);
                }
            }
        }

        // Check retrieved but non-existent in the actual folder
        for (final File f : Objects.requireNonNull(retrievedFolder.listFiles())) {
            final File fInActualFolder = new File(actualFolder, f.getName());
            final List<String> actualLines = new ArrayList<>();
            if (f.getName().endsWith(EXTENSION) && !fInActualFolder.exists()) {
                final List<String> retrievedLines = FileUtils.getLinesOfFile(f);
                final String name = f.getName().substring(0, f.getName().length() - EXTENSION.length());
                updateResult(resultsContent, counts, actualLines, retrievedLines, name);
            }
        }

        resultsContent.append("Total,");
        // precision total
        final double precisionTotal = getPrecision(counts.tp, counts.fp);
        if (Double.isNaN(precisionTotal)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(precisionTotal).append(",");
        }

        // recall avg.
        final double recallTotal = getRecall(counts.tp, counts.fn);
        if (Double.isNaN(recallTotal)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(recallTotal).append(",");
        }

        // f1score avg.
        final double f1measureTotal = getF1(precisionTotal, recallTotal);
        if (Double.isNaN(f1measureTotal)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(f1measureTotal).append(",");
        }

        totalPrecision = precisionTotal;
        totalRecall = recallTotal;
        totalF1 = f1measureTotal;
        resultsContent.append("\n");

        // total failed to retrieve
        resultsContent.append(failedToRetrieve_counter).append(",");

        // Check retrieved but nonexistent in the actual folder
        final StringBuilder nonexistent = new StringBuilder();
        retrievedNonexistentFeature_counter = 0;
        for (final File f : Objects.requireNonNull(retrievedFolder.listFiles())) {
            final File fInActualFolder = new File(actualFolder, f.getName());
            if (f.getName().endsWith(EXTENSION) && !fInActualFolder.exists()) {
                // it does not exist in actual folder
                retrievedNonexistentFeature_counter++;
                final String name = f.getName().substring(0, f.getName().length() - EXTENSION.length());
                nonexistent.append(name);
                nonexistent.append(",");
            }
        }
        resultsContent.append(retrievedNonexistentFeature_counter).append(",");
        if (retrievedNonexistentFeature_counter > 0) {
            // remove last comma
            nonexistent.setLength(nonexistent.length() - 1);
            // append list
            resultsContent.append(nonexistent);
        }

        return resultsContent.toString();
    }

    private static void updateResult(final StringBuilder resultsContent, final ResultCounts counts, final List<String> actualLines, final List<String> retrievedLines, final String name) {
        // Count tp, fp, fn
        counts.tp += countTruePositives(actualLines, retrievedLines);
        counts.fp += countFalsePositives(actualLines, retrievedLines);
        counts.fn += countFalseNegative(actualLines, retrievedLines);

        final double precision = getPrecision(actualLines, retrievedLines);
        final double recall = getRecall(actualLines, retrievedLines);
        final double f1measure = getF1(precision, recall);

        // Append the row to the results file
        resultsContent.append(name).append(",");

        // precision
        if (Double.isNaN(precision)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(precision).append(",");
        }

        // recall
        if (Double.isNaN(recall)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(recall).append(",");
        }

        // f1score
        if (Double.isNaN(f1measure)) {
            resultsContent.append("0,");
        } else {
            resultsContent.append(f1measure).append(",");
        }

        // something retrieved or not
        if (retrievedLines.isEmpty()) {
            failedToRetrieve_counter++;
            resultsContent.append("NothingRetrieved\n");
        } else {
            resultsContent.append("SomethingRetrieved\n");
        }
    }

    /**
     * From the retrieved elements, those that are on the actual list
     */
    public static List<String> getTruePositives(final List<String> actualLines, final List<String> retrievedLines) {
        final List<String> found = new ArrayList<>();
        for (final String a : retrievedLines) {
            if (actualLines.contains(a)) {
                found.add(a);
            }
        }
        return found;
    }

    /**
     * From the retrieved elements, those that are not on the actual list
     */
    public static List<String> getFalsePositives(final List<String> actualLines, final List<String> retrievedLines) {
        final List<String> found = new ArrayList<>();
        for (final String a : retrievedLines) {
            if (!actualLines.contains(a)) {
                found.add(a);
            }
        }
        return found;
    }

    public static int countTruePositives(final List<String> actualLines, final List<String> retrievedLines) {
        return getTruePositives(actualLines, retrievedLines).size();
    }

    public static int countFalsePositives(final List<String> actualLines, final List<String> retrievedLines) {
        return getFalsePositives(actualLines, retrievedLines).size();
    }

    public static int countFalseNegative(final List<String> actualLines, final List<String> retrievedLines) {
        return actualLines.size() - countTruePositives(actualLines, retrievedLines);
    }

    public static double getPrecision(final List<String> actualLines, final List<String> retrievedLines) {
        final List<String> truePositives = getTruePositives(actualLines, retrievedLines);
        final List<String> falsePositives = getFalsePositives(actualLines, retrievedLines);
        return getPrecision(truePositives.size(), falsePositives.size());
    }

    public static double getPrecision(final int tp, final int fp) {
        return (double) tp / ((double) tp + (double) fp);
    }

    public static double getRecall(final List<String> actualLines, final List<String> retrievedLines) {
        final List<String> truePositives = getTruePositives(actualLines, retrievedLines);
        return (double) truePositives.size() / (double) actualLines.size();
    }

    public static double getRecall(final int tp, final int fn) {
        return (double) tp / ((double) tp + (double) fn);
    }

    public static double getF1(final double precision, final double recall) {
        return 2 * ((precision * recall) / (precision + recall));
    }

    private static class ResultCounts {
        int tp;
        int fp;
        int fn;

        ResultCounts(final int tp, final int fp, final int fn) {
            this.tp = tp;
            this.fp = fp;
            this.fn = fn;
        }
    }

}
