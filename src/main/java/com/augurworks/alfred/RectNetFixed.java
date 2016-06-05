package com.augurworks.alfred;

import com.augurworks.alfred.NetTrainSpecification.Builder;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.server.LoggingHelper;
import com.augurworks.alfred.stats.StatsTracker;
import com.augurworks.alfred.stats.StatsTracker.Snapshot;
import com.augurworks.alfred.util.BigDecimals;
import com.augurworks.alfred.util.TimeUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple rectangular neural network.
 *
 * @author saf
 *
 */
public class RectNetFixed extends Net {

    static Logger log = LoggerFactory.getLogger(RectNetFixed.class);

    private static final double NEGATIVE_INFINITY = -1000000;
    // Inputs to network
    protected InputImpl[] inputs;
    // Every neuron with the same i is in the
    // same "layer". Indexed as [col][row].
    protected FixedNeuron[][] neurons;
    // X is depth of network
    protected int x;
    // Y is height of network (number of inputs)
    protected int y;
    // There's only one final output neuron
    // since this is built to make booleans.
    protected FixedNeuron output;
    // Prints debug output when true.
    protected boolean verbose = false;
    private NetDataSpecification netData = null;
    private TimingInfo timingInfo = null;
    private TrainingSummary trainingSummary = null;

    @Nullable
    private final PrintWriter logOutputStream;

    /**
     * Constructs a new RectNet with 10 inputs and 5 layers of network.
     */
    public RectNetFixed() {
        this.x = 5;
        this.y = 10;
        init();
        this.logOutputStream = null;
    }

    /**
     * Constructs a new RectNet with given depth and number of inputs.
     *
     * @param depth
     *            number of layers in the network
     * @param numInputs
     *            number of inputs to the network
     */
    public RectNetFixed(int depth, int numInputs, PrintWriter logOutputStream) {
        if (depth < 1 || numInputs < 1) {
            throw new IllegalArgumentException("Depth and numInputs must be >= 1");
        }
        this.x = depth;
        this.y = numInputs;
        init();
        this.logOutputStream = logOutputStream;
    }

    /**
     * Constructs a new RectNet with given depth and number of inputs. Sets the
     * verbose boolean as given.
     *
     * @param depth
     *            number of layers in the network
     * @param numInputs
     *            number of inputs to the network
     * @param verbose
     *            true when RectNet displays debug output.
     */
    public RectNetFixed(int depth, int numInputs, boolean verbose) {
        if (depth < 1 || numInputs < 1) {
            throw new IllegalArgumentException("Depth and numInputs must be >= 1");
        }
        this.x = depth;
        this.y = numInputs;
        this.verbose = verbose;
        init();
        this.logOutputStream = null;
    }

    /**
     * Gets the weight between two neurons. Only works for internal layers (not
     * the output neuron layer).
     *
     * @param leftCol
     *            column number of neuron to left of connection
     * @param leftRow
     *            row number of neuron to left of connection
     * @param rightCol
     *            column number of neuron to right of connection
     * @param rightRow
     *            row number of neuron to right of connection
     * @return weight from right neuron to left neuron.
     */
    public BigDecimal getWeight(int leftCol, int leftRow, int rightCol, int rightRow) {
        Validate.isTrue(leftCol >= 0);
        Validate.isTrue(leftRow >= 0);
        Validate.isTrue(rightCol >= 0);
        Validate.isTrue(rightRow >= 0);
        Validate.isTrue(leftCol < this.x);
        Validate.isTrue(rightCol < this.x);
        Validate.isTrue(leftRow < this.y);
        Validate.isTrue(rightRow < this.y);
        return this.neurons[rightCol][rightRow].getWeight(leftRow);
    }

    /**
     * Returns the width of this net
     *
     * @return the width of this net
     */
    public int getX() {
        return x;
    }

    /**
     * returns the height of this net
     *
     * @return the height of this net
     */
    public int getY() {
        return y;
    }

    public void setData(NetDataSpecification dataSpec) {
        this.netData = dataSpec;
    }

    public void setTimingInfo(TimingInfo timingInfo) {
        this.timingInfo = timingInfo;
    }

    public TimingInfo getTimingInfo() {
        return this.timingInfo;
    }

    public boolean hasTimeExpired() {
        return getTimingInfo().hasTimeExpired();
    }

    public NetDataSpecification getDataSpec() {
        return this.netData;
    }

    public TrainingSummary getTrainingSummary() {
        return trainingSummary;
    }

    /**
     * Returns the weight from the output neuron to an input specified by the
     * given row.
     *
     * @param leftRow
     *            the column containing the neuron to the left of the output
     *            neuron.
     * @return the weight from the output neuron to the neuron in leftRow
     */
    public BigDecimal getOutputNeuronWeight(int leftRow) {
        Validate.isTrue(leftRow >= 0);
        Validate.isTrue(leftRow < this.y);
        return this.output.getWeight(leftRow);
    }

    /**
     * Sets the weight from the output neuron to an input specified by the given
     * row
     *
     * @param leftRow
     *            the row containing the neuron to the left of the output
     *            neuron.
     * @param w
     *            the new weight from the output neuron to the neuron at leftRow
     */
    public void setOutputNeuronWeight(int leftRow, BigDecimal w) {
        Validate.isTrue(leftRow >= 0);
        Validate.isTrue(leftRow < this.y);
        this.output.setWeight(leftRow, w);
    }

    /**
     * Sets the weight between two neurons to the given value w. Only works for
     * internal layers (not the output neuron layer).
     *
     * @param leftCol
     *            column number of neuron to left of connection
     * @param leftRow
     *            row number of neuron to left of connections
     * @param rightCol
     *            column number of neuron to right of connection
     * @param rightRow
     *            row number of neuron to right of connection
     * @param w
     *            weight to set on connection.
     */
    public void setWeight(int leftCol, int leftRow, int rightCol, int rightRow,
            BigDecimal w) {
        Validate.isTrue(leftCol >= 0);
        Validate.isTrue(leftRow >= 0);
        // right column should be >= 1 because the weights from first row to
        // inputs should never be changed
        Validate.isTrue(rightCol >= 1);
        Validate.isTrue(rightCol - leftCol == 1);
        Validate.isTrue(rightRow >= 0);
        Validate.isTrue(leftCol < this.y);
        Validate.isTrue(rightCol < this.y);
        Validate.isTrue(leftRow < this.x);
        Validate.isTrue(rightRow < this.x);
        this.neurons[rightCol][rightRow].setWeight(leftRow, w);
    }

    /**
     * Initializes the RectNet by: 1) creating neurons and inputs as necessary
     * 2) connecting neurons to the inputs 3) connecting neurons to each other
     * 4) connecting neurons to the output
     *
     * Initial weights are specified by initNum(), allowing random initial
     * weights, or some other set.
     */
    private void init() {
        initEmptyNeurons();
        // Make connections between neurons and inputs.
        initNeuronConnections();
    }

    private void initNeuronConnections() {
        for (int j = 0; j < this.y; j++) {
            this.neurons[0][j].addInput(this.inputs[j], BigDecimal.ONE);
        }
        // Make connections between neurons and neurons.
        for (int leftCol = 0; leftCol < this.x - 1; leftCol++) {
            int rightCol = leftCol + 1;
            for (int leftRow = 0; leftRow < this.y; leftRow++) {
                for (int rightRow = 0; rightRow < this.y; rightRow++) {
                    this.neurons[rightCol][rightRow].addInput(
                            this.neurons[leftCol][leftRow], initNum());
                }
            }
        }
        // Make connections between output and neurons.
        for (int j = 0; j < this.y; j++) {
            this.output.addInput(this.neurons[this.x - 1][j], initNum());
        }
    }

    private void initEmptyNeurons() {
        // Initialize arrays to blank neurons and inputs.
        this.inputs = new InputImpl[y];
        this.neurons = new FixedNeuron[x][y];
        this.output = new FixedNeuron(this.y);
        // Name the neurons for possible debug. This is not a critical
        // step.
        this.output.setName("output");
        for (int j = 0; j < this.y; j++) {
            this.inputs[j] = new InputImpl();
            // initialize the first row
            this.neurons[0][j] = new FixedNeuron(1);
            this.neurons[0][j].setName("(" + 0 + "," + j + ")");
            for (int i = 1; i < this.x; i++) {
                this.neurons[i][j] = new FixedNeuron(this.y);
                this.neurons[i][j].setName("(" + i + "," + j + ")");
            }
        }
    }

    /**
     * Allows network weight initialization to be changed in one location.
     *
     * @return a double that can be used to initialize weights between network
     *         connections.
     */
    private BigDecimal initNum() {
        return BigDecimal.valueOf((Math.random() - .5) * 1.0);
    }

    /**
     * Sets the inputs of this network to the values given. Length of inputs must
     * be equal to the "height" of the network.
     *
     * @param inputs
     *            array of double to set as network inputs.
     */
    public void setInputs(BigDecimal[] inputs) {
        Validate.isTrue(inputs.length == this.y);
        for (int j = 0; j < this.y; j++) {
            this.inputs[j].setValue(inputs[j]);
        }
    }

    /**
     * Returns the output value from this network run.
     */
    @Override
    public BigDecimal getOutput() {
        BigDecimal[] outs = new BigDecimal[this.y];
        BigDecimal[] ins = new BigDecimal[this.y];
        for (int j = 0; j < this.y; j++) {
            // require recursion (depth = 0) here.
            ins[j] = this.neurons[0][j].getOutput();
        }
        // indexing must start at 1, because we've already computed
        // the output from the 0th row in the previous 3 lines.
        for (int i = 1; i < this.x; i++) {
            for (int j = 0; j < this.y; j++) {
                outs[j] = this.neurons[i][j].getOutput(ins);
            }
            ins = outs;
            outs = new BigDecimal[this.y];
        }
        return this.output.getOutput(ins);
    }

    /**
     * Trains the network on a given input with a given output the number of
     * times specified by iterations. Trains via a back-propagation algorithm.
     *
     * @param inputs
     *            input values for the network.
     * @param desired
     *            what the result of the network should be.
     * @param learningConstant
     *            the "rate" at which the network learns
     * @param iterations
     *            number of times to train the network.
     * @throws InterruptedException
     */
    public void train(BigDecimal[] inputs, BigDecimal desired, int iterations,
            BigDecimal learningConstant) throws InterruptedException {
        Validate.isTrue(iterations > 0);
        Validate.isTrue(inputs.length == this.y);
        for (int lcv = 0; lcv < iterations && !hasTimeExpired(); lcv++) {
            doIteration(inputs, desired, learningConstant);
        }
    }

    /**
     * Denormalizes targets and estimates.
     */
    public String getAugout(RectNetFixed net) {
        StringBuilder sb = new StringBuilder();
        TrainingSummary summary = net.getTrainingSummary();
        sb.append("Training stop reason: ").append(summary.getStopReason().getExplanation()).append("\n");
        sb.append("Time trained: ").append(summary.getSecondsElapsed()).append("\n");
        sb.append("Rounds trained: ").append(summary.getRoundsTrained()).append("\n");
        sb.append("RMS Error: ").append(summary.getRmsError()).append("\n");
        for (InputsAndTarget trainDatum : net.getDataSpec().getTrainData()) {
            writeDataLine(sb, trainDatum, net);
        }
        for (InputsAndTarget predictionDatum : net.getDataSpec().getPredictionData()) {
            writePredictionLine(sb, predictionDatum, net);
        }
        return sb.toString();
    }

    private static void writePredictionLine(StringBuilder sb, InputsAndTarget predictionDatum, RectNetFixed net) {
        sb.append(predictionDatum.getDate()).append(" ");
        sb.append("NULL").append(" ");

        net.setInputs(predictionDatum.getInputs());
        BigDecimal trainedEstimate = net.getOutput();
        trainedEstimate = net.getDataSpec().denormalize(trainedEstimate);
        sb.append(trainedEstimate.doubleValue()).append(" ");

        sb.append("NULL").append("\n");
    }

    private static void writeDataLine(StringBuilder sb, InputsAndTarget trainDatum, RectNetFixed net) {
        sb.append(trainDatum.getDate()).append(" ");

        BigDecimal target = trainDatum.getTarget();
        target = net.getDataSpec().denormalize(target);
        sb.append(target.doubleValue()).append(" ");

        net.setInputs(trainDatum.getInputs());
        BigDecimal trainedEstimate = net.getOutput();
        trainedEstimate = net.getDataSpec().denormalize(trainedEstimate);
        sb.append(trainedEstimate.doubleValue()).append(" ");

        sb.append(Math.abs(target.doubleValue() - trainedEstimate.doubleValue()));
        sb.append("\n");
    }

    private void doIteration(BigDecimal[] inputs, BigDecimal desired, BigDecimal learningConstant) throws InterruptedException {
        checkInterrupted();
        // Set the inputs
        setInputs(inputs);
        // Compute the last node error
        BigDecimal deltaF = getOutputError(desired);
        if (verbose) {
            out("DeltaF (smaller is better): " + deltaF);
        }
        // For each interior node, compute the weighted error
        // deltas are of the form
        // delta[col][row]
        BigDecimal[][] deltas = computeInteriorDeltas(deltaF);
        // now that we have the deltas, we can change the weights
        // again, we special case the last neuron
        updateLastNeuronWeights(learningConstant, deltaF);
        // now we do the same for the internal nodes
        updateInteriorNodeWeights(learningConstant, deltas);
    }

    private void updateInteriorNodeWeights(BigDecimal learningConstant,
            BigDecimal[][] deltas) {
        int leftCol;
        int leftRow;
        int rightCol;
        int rightRow;
        for (leftCol = this.x - 2; leftCol >= 0; leftCol--) {
            rightCol = leftCol + 1;
            for (leftRow = 0; leftRow < this.y; leftRow++) {
                for (rightRow = 0; rightRow < this.y; rightRow++) {
                    // w' = w + r*i*delta
                    // r is the learning constant
                    // i is the output from the leftward neuron
                    BigDecimal dw = learningConstant.multiply(this.neurons[leftCol][leftRow]
                                    .getLastOutput()).multiply(deltas[rightCol][rightRow]);
                    this.neurons[rightCol][rightRow].changeWeight(leftRow,
                            dw);
                    if (verbose) {
                        out(leftCol + "," + leftRow + "->" + rightCol + "," + rightRow);
                        out("" + this.neurons[rightCol][rightRow].getWeight(leftRow));
                    }
                }
            }
        }
    }

    private void updateLastNeuronWeights(BigDecimal learningConstant,
            BigDecimal deltaF) {
        for (int j = 0; j < this.y; j++) {
            // w' = w + r*i*delta
            // r is the learning constant
            // i is the output from the leftward neuron
            BigDecimal dw = learningConstant.multiply(this.neurons[this.x - 1][j].getLastOutput()).multiply(deltaF);
            this.output.changeWeight(j, dw);
        }
    }

    private BigDecimal[][] computeInteriorDeltas(BigDecimal deltaF) {
        BigDecimal[][] deltas = new BigDecimal[this.x + 1][this.y];
        // spoof the rightmost deltas
        for (int j = 0; j < y; j++) {
            deltas[this.x][j] = deltaF;
        }
        int leftCol;
        int leftRow;
        int rightCol;
        int rightRow;
        for (leftCol = this.x - 1; leftCol >= 0; leftCol--) {
            rightCol = leftCol + 1;
            for (leftRow = 0; leftRow < this.y; leftRow++) {
                BigDecimal lastOutput = this.neurons[leftCol][leftRow].getLastOutput();
                // since we're using alpha = 3 in the neurons
                // 3 * lastOutput * (1 - lastOutput);
                BigDecimal delta = BigDecimal.valueOf(3).multiply(lastOutput).multiply(BigDecimal.ONE.subtract(lastOutput));
                BigDecimal summedRightWeightDelta = BigDecimal.ZERO;
                for (rightRow = 0; rightRow < this.y; rightRow++) {
                    if (rightCol == this.x) {
                        summedRightWeightDelta = summedRightWeightDelta.add(this.output.getWeight(leftRow).multiply(deltaF));
                        // without the break, we were adding too many of the
                        // contributions of the output node when computing
                        // the deltas value for the layer immediately left
                        // of it.
                        break;
                    } else {
                        // summing w * delta
                        summedRightWeightDelta = summedRightWeightDelta.add(getWeight(leftCol,
                                leftRow, rightCol, rightRow).multiply(deltas[rightCol][rightRow]));
                    }
                }
                deltas[leftCol][leftRow] = delta.multiply(summedRightWeightDelta);
                if (verbose) {
                    out("leftCol: " + leftCol
                            + ", leftRow: " + leftRow + ", lo*(1-lo): "
                            + delta);
                    out("leftCol: " + leftCol
                            + ", leftRow: " + leftRow + ", srwd: "
                            + summedRightWeightDelta);
                    out("leftCol: " + leftCol
                            + ", leftRow: " + leftRow + ", delta: "
                            + deltas[leftCol][leftRow]);
                }
            }
        }
        return deltas;
    }

    /**
     * Computes the error in the network, assuming that the proper inputs have
     * been set before this method is called.
     *
     * @param desired
     *            the desired output.
     * @return error using equation (output*(1-output)*(desired-output))
     */
    protected BigDecimal getOutputError(BigDecimal desired) {
        getOutput();
        // since we're using alpha = 3 in the neurons
        // 3 * last * ( 1 - last) * (desired - last)
        BigDecimal last = this.output.getLastOutput();
        BigDecimal oneMinusLast = BigDecimal.ONE.subtract(last);
        BigDecimal desiredMinusLast = desired.subtract(last);
        return BigDecimal.valueOf(3).multiply(last).multiply(oneMinusLast).multiply(desiredMinusLast);
    }

    private class TrainingStats {
        public long startTime;
        public TrainingStopReason stopReason;
        public boolean brokeAtLocalMax;
        public BigDecimal maxScore;

        public TrainingStats(long start) {
            this.startTime = start;
            this.brokeAtLocalMax = false;
            this.stopReason = TrainingStopReason.HIT_TRAINING_LIMIT;
            this.maxScore = BigDecimal.valueOf(NEGATIVE_INFINITY);
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Job detected interrupt flag");
        }
    }

    /**
     * Train a neural network from a .augtrain training file
     *
     * @param name
     *            File path to .augtrain training file
     * @param verbose
     *            Flag to display debugging text or not
     * @return The trained neural network
     * @throws InterruptedException
     */
    public RectNetFixed train(String name,
                                     List<String> trainLines,
                                     boolean verbose,
                                     boolean testing,
                                     long trainingTimeLimitMillis,
                                     ScaleFunctionType sfType,
                                     int triesRemaining,
                                     PrintWriter logOutputFile,
                                     StatsTracker stats) throws InterruptedException {

        if (trainingTimeLimitMillis <= 0) {
            LoggingHelper.out("Training timeout was " + trainingTimeLimitMillis +
                    ", which is <= 0, so jobs will not time out.", logOutputFile);
        }
        if (triesRemaining == 0) {
            throw new IllegalStateException("Unable to train file " + name + "!");
        }
        NetTrainSpecification netSpec = parseLines(trainLines, sfType, verbose);
        MDC.put("performanceCutoff", netSpec.getPerformanceCutoff());
        MDC.put("learningConstant", netSpec.getLearningConstant());
        MDC.put("minTrainingRounds", netSpec.getMinTrainingRounds());
        RectNetFixed net = new RectNetFixed(netSpec.getDepth(), netSpec.getSide(), logOutputFile);
        net.setData(netSpec.getNetData());
        net.setTimingInfo(TimingInfo.withDuration(trainingTimeLimitMillis));
        // Actually do the training part
        TrainingStats trainingStats = new TrainingStats(System.currentTimeMillis());

        int fileIteration = 0;
        BigDecimal score = null;

        List<InputsAndTarget> inputsAndTargets = netSpec.getNetData().getTrainData();
        for (fileIteration = 0; fileIteration < netSpec.getNumberFileIterations(); fileIteration++) {

            // train all data rows for numberRowIterations times.
            for (int lcv = 0; lcv < inputsAndTargets.size() && !net.hasTimeExpired(); lcv++) {
                InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
                net.train(inputsAndTarget.getInputs(),
                          inputsAndTarget.getTarget(),
                          netSpec.getNumberRowIterations(),
                          netSpec.getLearningConstant());
            }

            if (net.hasTimeExpired()) {
                trainingStats.stopReason = TrainingStopReason.OUT_OF_TIME;
                break;
            }

            // compute total score
            score = BigDecimal.ZERO;
            for (int lcv = 0; lcv < inputsAndTargets.size(); lcv++) {
                InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
                net.setInputs(inputsAndTarget.getInputs());
                // Math.pow((targets.get(lcv) - r.getOutput()), 2)
                BigDecimal difference = inputsAndTarget.getTarget().subtract(net.getOutput());
                score = score.add(difference.multiply(difference));
            }
            score = score.multiply(BigDecimal.valueOf(-1.0));
            score = score.divide(BigDecimal.valueOf(inputsAndTargets.size()), BigDecimals.MATH_CONTEXT);

            // if (score > -1 * cutoff)
            //   ==> if (score - (-1 * cutoff) > 0)
            //     ==> if (min(score - (-1 * cutoff), 0) == 0)
            if (BigDecimal.ZERO.min(
                    score.subtract(BigDecimal.valueOf(-1.0).multiply(netSpec.getPerformanceCutoff())))
                        .equals(BigDecimal.ZERO)) {
                trainingStats.stopReason = TrainingStopReason.HIT_PERFORMANCE_CUTOFF;
                break;
            }
            // if (score > maxScore)
            //   ==> if (score - maxScore > 0)
            //     ==> if (min(score - maxScore, 0) == 0)
            if (BigDecimal.ZERO.min(score.subtract(trainingStats.maxScore)).equals(BigDecimal.ZERO)) {
                trainingStats.maxScore = score;
            } else if (fileIteration < netSpec.getMinTrainingRounds()) {
                continue;
            } else {
                trainingStats.brokeAtLocalMax = true;
                break;
            }

            if (fileIteration % 100 == 0) {
                MDC.put("netScore", score.round(new MathContext(4)).toString());
                double rmsError = computeRmsError(net, inputsAndTargets);
                LoggingHelper.out("Net " + name + " has trained for " + fileIteration + " rounds, RMS Error: " + rmsError, logOutputFile);
                stats.addSnapshot(new Snapshot(fileIteration, System.currentTimeMillis() - net.timingInfo.getStartTime(),
                        netSpec.getNumberFileIterations(), name, netSpec.getLearningConstant().doubleValue(), true,
                        trainingStats.stopReason, rmsError, netSpec.getPerformanceCutoff().doubleValue()));
            }

        }
        MDC.put("netScore", score.round(new MathContext(4)).toString());
        if (trainingStats.brokeAtLocalMax) {
            long timeExpired = System.currentTimeMillis() - net.timingInfo.getStartTime();
            long timeRemaining = trainingTimeLimitMillis - timeExpired;
            LoggingHelper.out("Retraining net from file " + name + " with " +
                    TimeUtils.formatSeconds((int)timeRemaining/1000) + " remaining.", logOutputFile);
            net = this.train(name, trainLines, verbose, testing, timeRemaining, sfType, triesRemaining--, logOutputFile, stats);
        }
        int timeExpired = (int)((System.currentTimeMillis() - net.timingInfo.getStartTime())/1000);
        double rmsError = computeRmsError(net, inputsAndTargets);
        net.trainingSummary = new TrainingSummary(trainingStats.stopReason, timeExpired, fileIteration, rmsError);

        stats.addSnapshot(new Snapshot(fileIteration, System.currentTimeMillis() - net.timingInfo.getStartTime(),
                netSpec.getNumberFileIterations(), name, netSpec.getLearningConstant().doubleValue(), true,
                trainingStats.stopReason, rmsError, netSpec.getPerformanceCutoff().doubleValue()));

        return net;
    }

    private double computeRmsError(RectNetFixed net, List<InputsAndTarget> inputsAndTargets) {
        double totalRmsError = 0;
        for (int lcv = 0; lcv < inputsAndTargets.size(); lcv++) {
            InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
            net.setInputs(inputsAndTarget.getInputs());
            double target = inputsAndTarget.getTarget().doubleValue();
            double actual = net.getOutput().doubleValue();
            double square = Math.pow(target - actual, 2);
            totalRmsError += square;
        }
        if (totalRmsError == 0) {
            return 0;
        } else {
            return Math.sqrt(totalRmsError / (1.0 * inputsAndTargets.size()));
        }
    }

    public NetTrainSpecification parseLines(List<String> augtrain, ScaleFunctionType sfType, boolean verbose) {
        NetTrainSpecification.Builder netTrainingSpecBuilder = new Builder();
        netTrainingSpecBuilder.scaleFunctionType(sfType);
        Validate.isTrue(augtrain.size() >= 4, "Cannot parse file with no data");

        Iterator<String> fileLineIterator = augtrain.iterator();
        parseSizeLine(netTrainingSpecBuilder, fileLineIterator);
        parseTrainingInfoLine(netTrainingSpecBuilder, fileLineIterator);
        // skip the titles line
        fileLineIterator.next();
        while (fileLineIterator.hasNext()) {
            parseDataLine(netTrainingSpecBuilder, fileLineIterator);
        }
        return netTrainingSpecBuilder.build();
    }

    private void parseDataLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String dataLine = fileLineIterator.next();
        String[] dataLineSplit = dataLine.split(" ");
        String date = dataLineSplit[0];
        String target = dataLineSplit[1];
        // inputs
        BigDecimal[] input = new BigDecimal[netTrainingSpec.getSide()];
        dataLineSplit = dataLineSplit[2].split(",");
        for (int i = 0; i < netTrainingSpec.getSide(); i++) {
            input[i] = BigDecimal.valueOf(Double.valueOf(dataLineSplit[i]));
        }

        if (target.equalsIgnoreCase("NULL")) {
            netTrainingSpec.addPredictionRow(input, date);
        } else {
            BigDecimal targetVal = BigDecimal.valueOf(Double.valueOf(target));
            netTrainingSpec.addInputAndTarget(input, targetVal, date);
        }
    }

    private void parseTrainingInfoLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String trainingInfoLine = fileLineIterator.next();
        String[] trainingInfoLineSplit = trainingInfoLine.split(" ");
        trainingInfoLineSplit = trainingInfoLineSplit[1].split(",");
        int rowInterations = Integer.valueOf(trainingInfoLineSplit[0]);
        int fileIterations = Integer.valueOf(trainingInfoLineSplit[1]);
        BigDecimal learningConstant = BigDecimal.valueOf(Double.valueOf(trainingInfoLineSplit[2]));
        int minTrainingRounds = Integer.valueOf(trainingInfoLineSplit[3]);
        BigDecimal cutoff = BigDecimal.valueOf(Double.valueOf(trainingInfoLineSplit[4]));

        netTrainingSpec.minTrainingRounds(minTrainingRounds);
        netTrainingSpec.learningConstant(learningConstant);
        netTrainingSpec.rowIterations(rowInterations);
        netTrainingSpec.fileIterations(fileIterations);
        netTrainingSpec.performanceCutoff(cutoff);
    }

    private void parseSizeLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String sizeLine = fileLineIterator.next();
        String[] sizeLineSplit = sizeLine.split(" ");
        sizeLineSplit = sizeLineSplit[1].split(",");
        netTrainingSpec.side(Integer.valueOf(sizeLineSplit[0]));
        netTrainingSpec.depth(Integer.valueOf(sizeLineSplit[1]));
    }

    /**
     * Input a filename and a neural network to save the neural network as a
     * .augsave file
     *
     * @author TheConnMan
     * @param fileName
     *            Filepath ending in .augsave where the network will be saved
     * @param net
     *            Neural net to be saved
     */
    public void saveNet(String fileName, RectNetFixed net) {
        try {
            if (!(fileName.toLowerCase().endsWith(".augsave"))) {
                log.error("Output file name to save to should end in .augsave");
                return;
            }
            PrintWriter out = new PrintWriter(new FileWriter(fileName));
            out.println("net " + Integer.toString(net.getX()) + ","
                    + Integer.toString(net.getY()));
            String line = "O ";
            for (int j = 0; j < net.getY(); j++) {
                line += net.getOutputNeuronWeight(j) + ",";
            }
            out.println(line.substring(0, line.length() - 1));
            for (int leftCol = 0; leftCol < net.getX() - 1; leftCol++) {
                int rightCol = leftCol + 1;
                for (int rightRow = 0; rightRow < net.getY(); rightRow++) {
                    line = rightCol + " ";
                    for (int leftRow = 0; leftRow < net.getY(); leftRow++) {
                        line += net.neurons[rightCol][rightRow].getWeight(leftRow).doubleValue() + ",";
                    }
                    out.println(line.substring(0, line.length() - 1));
                }
            }
            out.close();
        } catch (IOException e) {
            log.error("Error occurred opening file to saveNet");
            throw new IllegalArgumentException("Could not open file");
        }
    }

    /**
     * @param fileName
     * @param r
     */
    public BigDecimal testNet(String fileName, RectNetFixed r,
            boolean verbose) {
        boolean valid = Net.validateAUGTest(fileName, r.y);
        if (!valid) {
            log.error("File not valid format.");
            throw new RuntimeException("File not valid format");
        }
        // Now we need to pull information out of the augtrain file.
        Charset charset = Charset.forName("US-ASCII");
        Path file = Paths.get(fileName);
        String line = null;
        int lineNumber = 1;
        String[] lineSplit;
        int side = r.y;
        String[] size;
        ArrayList<BigDecimal[]> inputSets = new ArrayList<BigDecimal[]>();
        ArrayList<BigDecimal> targets = new ArrayList<BigDecimal>();
        BigDecimal[] maxMinNumbers = new BigDecimal[4];
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            while ((line = reader.readLine()) != null) {
                try {
                    lineSplit = line.split(" ");
                    switch (lineNumber) {
                    case 1:
                        String[] temp = lineSplit[1].split(",");
                        for (int j = 0; j < 4; j++) {
                            maxMinNumbers[j] = BigDecimal.valueOf(Double.valueOf(temp[j + 2]));
                        }
                        break;
                    case 2:
                        size = lineSplit[1].split(",");
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
                    log.error("Training failed at line: " + lineNumber);
                }
            }
        } catch (IOException x) {
           log.error("IOException: %s%n", x);
            System.exit(1);
        }
        BigDecimal score = BigDecimal.ZERO;
        for (int lcv = 0; lcv < inputSets.size(); lcv++) {
            r.setInputs(inputSets.get(lcv));
            // score += Math.pow((targets.get(lcv) - r.getOutput()), 2);
            BigDecimal diff = targets.get(lcv).multiply(r.getOutput());
            score = score.add(diff.multiply(diff));
        }
        if (verbose) {
            log.info("Final score of {}", score.doubleValue()
                    / (1.0 * inputSets.size()));
            // Results

            log.info("-------------------------");
            log.info("Test Results: ");
            log.info("Actual, Prediction");
            score = BigDecimal.ZERO;
            BigDecimal score2 = BigDecimal.ZERO;
            for (int lcv = 0; lcv < inputSets.size(); lcv++) {
                r.setInputs(inputSets.get(lcv));

                BigDecimal tempTarget = (targets.get(lcv).subtract(maxMinNumbers[3])).multiply(
                        (maxMinNumbers[0].subtract(maxMinNumbers[1]))).divide(
                        (maxMinNumbers[2].subtract(maxMinNumbers[3])), BigDecimals.MATH_CONTEXT).add(maxMinNumbers[1]);
                BigDecimal tempOutput = (r.getOutput().subtract(maxMinNumbers[3])).multiply(
                        (maxMinNumbers[0].subtract(maxMinNumbers[1]))).divide(
                        (maxMinNumbers[2].subtract(maxMinNumbers[3])), BigDecimals.MATH_CONTEXT).add(maxMinNumbers[1]);
                log.info(tempTarget + "," + tempOutput);
                score = score.add(tempTarget.subtract(tempOutput).abs());
                BigDecimal diff = tempTarget.subtract(tempOutput);
                score2 = score2.add(diff.multiply(diff));
            }
            score = score.divide(BigDecimal.valueOf(inputSets.size()), BigDecimals.MATH_CONTEXT);
            score2 = score2.divide(BigDecimal.valueOf(inputSets.size()), BigDecimals.MATH_CONTEXT);
            log.info("-------------------------");
            log.info("Average error={}", score);
            log.info("Average squared error={}", score2);
        }
        return score.divide(BigDecimal.valueOf(inputSets.size()), BigDecimals.MATH_CONTEXT);
    }

    private void out(String message) {
        LoggingHelper.out(message, logOutputStream);
    }

}
