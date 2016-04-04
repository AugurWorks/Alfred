package com.augurworks.alfred.parallel;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.augurworks.alfred.Net;
import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.WeightDelta;

/**
 * Master
 *
 * @author saf
 *
 */
public class PatternParallelRectNet extends RectNetFixed {

    public PatternParallelRectNet() {
        super();
    }

    public PatternParallelRectNet(int depth, int side) {
        super(depth, side, null);
    }

    /**
     * Trains a neural network from .augtrain file.
     *
     * @param fileName
     *            The absolute path to the .augtrain file.
     * @param nodes
     *            The number of nodes (threads) to use in training.
     * @param verbose
     *            Flag to display debug text or not
     * @return The trained neural network
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static PatternParallelRectNet trainFile(String fileName, int nodes,
            boolean verbose, String saveFile, boolean testing) throws InterruptedException,
            ExecutionException {
        /*
         * Copy-paste from the RectNetFixed parsing code. TODO abstract this
         * code block into another method.
         */
        boolean valid = Net.validateAUGt(fileName);
        if (!valid) {
            System.err.println("File not valid format.");
            throw new RuntimeException("File not valid");
        }
        // Now we need to pull information out of the augtrain file.
        Charset charset = Charset.forName("US-ASCII");
        Path file = Paths.get(fileName);
        String line = null;
        int lineNumber = 1;
        String[] lineSplit;
        int side = 0;
        int depth = 0;
        int rowIter = 0;
        int fileIter = 0;
        BigDecimal learningConstant = BigDecimal.ZERO;
        int minTrainingRounds = 0;
        BigDecimal cutoff = BigDecimal.ZERO;
        ArrayList<BigDecimal[]> inputSets = new ArrayList<BigDecimal[]>();
        ArrayList<BigDecimal> targets = new ArrayList<BigDecimal>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            while ((line = reader.readLine()) != null) {
                try {
                    lineSplit = line.split(" ");
                    switch (lineNumber) {
                    case 1:
                        // Information about network
                        String[] size = lineSplit[1].split(",");
                        side = Integer.valueOf(size[0]);
                        depth = Integer.valueOf(size[1]);
                        break;
                    case 2:
                        // Information about training run
                        size = lineSplit[1].split(",");
                        rowIter = Integer.valueOf(size[0]);
                        fileIter = Integer.valueOf(size[1]);
                        learningConstant = BigDecimal.valueOf(Double.valueOf(size[2]));
                        minTrainingRounds = Integer.valueOf(size[3]);
                        cutoff = BigDecimal.valueOf(Double.valueOf(size[4]));
                        break;
                    case 3:
                        // Titles
                        break;
                    default:
                        // expected
                        BigDecimal target = BigDecimal.valueOf(Double.valueOf(lineSplit[0]));
                        targets.add(target);
                        // inputs
                        BigDecimal[] input = new BigDecimal[side];
                        size = lineSplit[1].split(",");
                        for (int i = 0; i < side; i++) {
                            input[i] = BigDecimal.valueOf(Double.valueOf(size[i]));
                        }
                        inputSets.add(input);
                        break;
                    }
                    lineNumber++;
                } catch (Exception e) {
                    System.err
                            .println("Training failed at line: " + lineNumber);
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            throw new RuntimeException("IOException in parsing file");
        }
        // Information about the training file.
        if (verbose) {
            System.out.println("-------------------------");
            System.out.println("File path: " + fileName);
            System.out.println("Number Inputs: " + side);
            System.out.println("Net depth: " + depth);
            System.out.println("Number training sets: " + targets.size());
            System.out.println("Row iterations: " + rowIter);
            System.out.println("File iterations: " + fileIter);
            System.out.println("Learning constant: " + learningConstant);
            System.out.println("Minimum training rounds: " + minTrainingRounds);
            System.out.println("Performance cutoff: " + cutoff);
            System.out.println("-------------------------");
        }
        /*
         * END of copy-paste region
         */
        long start = System.currentTimeMillis();
        // Create the Parallel Net
        PatternParallelRectNet r = new PatternParallelRectNet(depth, side);
        BigDecimal maxScore = BigDecimal.valueOf(Double.NEGATIVE_INFINITY);
        BigDecimal score = BigDecimal.ZERO;
        BigDecimal testScore = BigDecimal.ZERO;
        BigDecimal lastScore = BigDecimal.valueOf(Double.POSITIVE_INFINITY);
        BigDecimal bestCheck = BigDecimal.valueOf(Double.POSITIVE_INFINITY);
        BigDecimal bestTestCheck = BigDecimal.valueOf(Double.POSITIVE_INFINITY);
        int i = 0;
        boolean brokeAtPerfCutoff = false;

        // For the partitioning (gross) FIXME
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int numberInList = 0; numberInList < inputSets.size(); numberInList++) {
            list.add(numberInList);
        }
        // Partition the data set, kick off children
        final ExecutorService service = Executors.newFixedThreadPool(nodes);

        try {
            for (i = 0; i < fileIter; i++) {
                List<Future<WeightDelta>> futures = new ArrayList<Future<WeightDelta>>(
                        nodes);
                for (int nodeNum = 0; nodeNum < nodes; nodeNum++) {
                    java.util.Collections.shuffle(list);
                    // TODO pick a fraction
                    int subsetSize = inputSets.size() / nodes;
                    subsetSize = 200; // FIXME
                    BigDecimal[][] inpts = new BigDecimal[subsetSize][inputSets.get(0).length];
                    BigDecimal[] desired = new BigDecimal[subsetSize];
                    for (int location = 0; location < subsetSize; location++) {
                        int index = list.get(location);
                        inpts[location] = inputSets.get(index);
                        desired[location] = targets.get(index);
                    }
                    PatternParallelNode p = new PatternParallelNode(r, inpts,
                            desired, rowIter, learningConstant);
                    futures.add(service.submit(p));
                }
                // Sync and check the status
                for (int futNum = 0; futNum < futures.size(); futNum++) {
                    Future<WeightDelta> future = futures.get(futNum);
                    WeightDelta wd = future.get();

                    // integrate the weight deltas
                    for (int j = 0; j < r.y; j++) {
                        // w' = w + r*i*delta
                        // r is the learning constant
                        // i is the output from the leftward neuron
                        BigDecimal dw = wd.getOutputDelta(j).divide(BigDecimal.valueOf(nodes));
                        r.output.changeWeight(j, dw);
                    }
                    // now we do the same for the internal nodes
                    for (int leftCol = r.x - 2; leftCol >= 0; leftCol--) {
                        int rightCol = leftCol + 1;
                        for (int leftRow = 0; leftRow < r.y; leftRow++) {
                            for (int rightRow = 0; rightRow < r.y; rightRow++) {
                                // w' = w + r*i*delta
                                // r is the learning constant
                                // i is the output from the leftward neuron
                                BigDecimal dw = wd.getInnerDelta(rightCol,
                                        rightRow, leftRow).divide(BigDecimal.valueOf(nodes));
                                r.neurons[rightCol][rightRow].changeWeight(
                                        leftRow, dw);
                            }
                        }
                    }
                }
                score = BigDecimal.ZERO;
                for (int lcv = 0; lcv < inputSets.size(); lcv++) {
                    r.setInputs(inputSets.get(lcv));
                    BigDecimal diff = targets.get(lcv).subtract(r.getOutput());
                    score = score.add(diff.multiply(diff));
                }
                score = score.multiply(BigDecimal.valueOf(-1.0));
                score = score.divide(BigDecimal.valueOf(inputSets.size()));
                if (i % 100 == 0) {
                    int diffCounter = 0;
                    int diffCounter2 = 0;
                    BigDecimal diffCutoff = BigDecimal.valueOf(.1);
                    BigDecimal diffCutoff2 = BigDecimal.valueOf(.05);
                    // bestCheck > -1.0 * score
                    if (bestCheck.max(BigDecimal.valueOf(-1.0).multiply(score)).equals(bestCheck)) {
                        RectNetFixed.saveNet(saveFile, r);
                        if (testing) {
                            int idx = saveFile.replaceAll("\\\\", "/").lastIndexOf(
                                    "/");
                            int idx2 = saveFile.lastIndexOf(
                                            ".");
                            testScore = RectNetFixed.testNet(
                                    saveFile.substring(0, idx + 1)
                                            + "OneThird.augtrain", r, verbose);
                            if (testScore.min(bestTestCheck).equals(testScore)) {
                                RectNetFixed.saveNet(saveFile.substring(0, idx2)
                                        + "Test.augsave", r);
                                bestTestCheck = testScore;
                            }
                        }
                        bestCheck = BigDecimal.valueOf(-1.0).multiply(score);
                    }
                    for (int lcv = 0; lcv < inputSets.size(); lcv++) {
                        r.setInputs(inputSets.get(lcv));
                        // Math.abs(targets.get(lcv) - r.getOutput()) > diffCutoff
                        if (targets.get(lcv).subtract(r.getOutput()).abs().min(diffCutoff).equals(diffCounter)) {
                            diffCounter++;
                        }
                        // Math.abs(targets.get(lcv) - r.getOutput()) > diffCutoff2
                        if (targets.get(lcv).subtract(r.getOutput()).abs().min(diffCutoff2).equals(diffCutoff2)) {
                            diffCounter2++;
                        }
                    }
                    System.out.println(i + " rounds trained.");
                    System.out.println("Current score: " + -1.0 * score.doubleValue());
                    System.out.println("Min Score=" + -1.0 * maxScore.doubleValue());
                    if (testing) {
                        System.out.println("Current Test Score=" + testScore);
                        System.out.println("Min Test Score=" + bestTestCheck);
                    }
                    System.out.println("Score change=" + (lastScore.add(score)).doubleValue());
                    System.out.println("Inputs Over " + diffCutoff + "="
                            + diffCounter + " of " + inputSets.size());
                    System.out.println("Inputs Over " + diffCutoff2 + "="
                            + diffCounter2 + " of " + inputSets.size());
                    BigDecimal diff = BigDecimal.ZERO;
                    for (int lcv = 0; lcv < inputSets.size(); lcv++) {
                        r.setInputs(inputSets.get(lcv));
                        diff = diff.add(r.getOutput().subtract(targets.get(lcv)));
                    }
                    System.out.println("AvgDiff=" + diff.doubleValue()
                            / (1.0 * inputSets.size()));
                    System.out.println("Current learning constant: "
                            + learningConstant);
                    System.out.println("Time elapsed (s): "
                            + (System.currentTimeMillis() - start) / 1000.0);
                    System.out.println("");
                }
                lastScore = BigDecimal.valueOf(-1.0).multiply(score);
                if (score.max(BigDecimal.valueOf(-1.0).multiply(cutoff)).equals(score)) {
                    brokeAtPerfCutoff = true;
                    break;
                }
                if (score.max(maxScore).equals(score)) {
                    maxScore = score;
                }
            }
        } finally {
            service.shutdown();
        }
        if (verbose) {
            // Information about performance and training.
            if (brokeAtPerfCutoff) {
                System.out.println("Performance cutoff hit.");
            } else {
                System.out.println("Training round limit reached.");
            }
            System.out.println("Rounds trained: " + i);
            System.out.println("Final score of " + -1 * score.doubleValue());
            System.out.println("Time elapsed (ms): "
                    + ((System.currentTimeMillis() - start)));
            // Results
            System.out.println("-------------------------");
            // System.out.println("Test Results: ");
            for (int lcv = 0; lcv < inputSets.size(); lcv++) {
                r.setInputs(inputSets.get(lcv));
                // System.out.println("Input " + lcv);
                // System.out.println("\tTarget: " + targets.get(lcv));
                // System.out.println("\tActual: " + r.getOutput());
            }
            // System.out.println("-------------------------");
        }
        return r;
    }

    /**
     * Load a neural network from a .augsave file
     *
     * @author TheConnMan
     * @param fileName
     *            File path to an .augsave file containing a neural network
     * @return Neural network from the .augsave file
     */
    public static PatternParallelRectNet loadNet(String fileName) {
        boolean valid = Net.validateAUGs(fileName);
        if (!valid) {
            System.err.println("File not valid format.");
            throw new RuntimeException("File not valid format");
        }
        // Now we need to pull information out of the augsave file.
        Charset charset = Charset.forName("US-ASCII");
        Path file = Paths.get(fileName);
        String line = null;
        int lineNumber = 1;
        String[] lineSplit;
        String[] edges;
        int side = 0;
        int depth = 0;
        int curCol = 0;
        int curRow = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            line = reader.readLine();
            try {
                lineSplit = line.split(" ");
                String[] size = lineSplit[1].split(",");
                side = Integer.valueOf(size[1]);
                depth = Integer.valueOf(size[0]);
            } catch (Exception e) {
                System.err.println("Loading failed at line: " + lineNumber);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            throw new RuntimeException("Failed to load file");
        }
        PatternParallelRectNet net = new PatternParallelRectNet(depth, side);
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            while ((line = reader.readLine()) != null) {
                try {
                    lineSplit = line.split(" ");
                    switch (lineNumber) {
                    case 1:
                        break;
                    case 2:
                        String outputs[] = lineSplit[1].split(",");
                        for (int edgeNum = 0; edgeNum < outputs.length; edgeNum++) {
                            net.output.setWeight(edgeNum,
                                    BigDecimal.valueOf(Double.parseDouble(outputs[edgeNum])));
                        }
                        break;
                    default:
                        curCol = Integer.valueOf(lineSplit[0]);
                        curRow = (lineNumber - 3) % side;
                        edges = lineSplit[1].split(",");
                        for (int edgeNum = 0; edgeNum < edges.length; edgeNum++) {
                            net.neurons[curCol][curRow].setWeight(edgeNum,
                                    BigDecimal.valueOf(Double.parseDouble(edges[edgeNum])));
                        }
                        break;
                    }
                    lineNumber++;
                } catch (Exception e) {
                    System.err.println("Loading failed at line: " + lineNumber);
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            throw new RuntimeException("Failed to load file");
        }
        return net;
    }

    public static void main(String[] args) {
        // String prefix =
        // "C:\\Users\\Stephen\\workspace\\AugurWorks\\Core\\java\\nets\\test_files\\";
        String prefix = "/root/Core/java/nets/test_files/";
        // String prefix = "C:\\Users\\TheConnMan\\workspace\\Core\\java\\nets\\test_files\\";
        String trainingFile = prefix + "TwoThirds.augtrain";
        String testFile = prefix + "OneThird.augtrain";
        String savedFile = prefix + "TwoThirdsTrained.augsave";
        PatternParallelRectNet r;
        try {
            r = PatternParallelRectNet.trainFile(trainingFile, 4, false, savedFile, true);
            RectNetFixed.testNet(testFile, r, true);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*r = PatternParallelRectNet.loadNet(savedFile);
        RectNetFixed.testNet(testFile, r, true);*/


        System.exit(0);
    }
}
