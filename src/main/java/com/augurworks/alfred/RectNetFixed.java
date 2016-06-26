package com.augurworks.alfred;

import com.augurworks.alfred.NetTrainSpecification.Builder;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.stats.StatsTracker;
import com.augurworks.alfred.stats.StatsTracker.Snapshot;
import com.augurworks.alfred.util.BigDecimals;
import com.augurworks.alfred.util.TimeUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Iterator;
import java.util.List;

/**
 * Simple rectangular neural network.
 *
 * @author saf
 *
 */
public class RectNetFixed {

    private static Logger log = LoggerFactory.getLogger(RectNetFixed.class);
    private static final double NEGATIVE_INFINITY = -1000000;
    public static final double SIGMOID_ALPHA = 3;

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
    private NetDataSpecification netData = null;
    private TimingInfo timingInfo = null;
    private TrainingSummary trainingSummary = null;

    /**
     * Constructs a new RectNet with 10 inputs and 5 layers of network.
     */
    public RectNetFixed() {
        this(5, 10);
    }

    /**
     * Constructs a new RectNet with given depth and number of inputs.
     *
     * @param depth
     *            number of layers in the network
     * @param numInputs
     *            number of inputs to the network
     */
    public RectNetFixed(int depth, int numInputs) {
        if (depth < 1 || numInputs < 1) {
            throw new IllegalArgumentException("Depth and numInputs must be >= 1");
        }
        this.x = depth;
        this.y = numInputs;
        init();
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
     * Initializes the RectNet by:
     * 1) creating neurons and inputs as necessary
     * 2) connecting neurons to the inputs
     * 3) connecting neurons to each other
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
    public String getAugout() {
        StringBuilder sb = new StringBuilder();
        TrainingSummary summary = this.getTrainingSummary();
        sb.append("Training stop reason: ").append(summary.getStopReason().getExplanation()).append("\n");
        sb.append("Time trained: ").append(summary.getSecondsElapsed()).append("\n");
        sb.append("Rounds trained: ").append(summary.getRoundsTrained()).append("\n");
        sb.append("RMS Error: ").append(summary.getRmsError()).append("\n");
        for (InputsAndTarget trainDatum : this.getDataSpec().getTrainData()) {
            writeDataLine(sb, trainDatum);
        }
        for (InputsAndTarget predictionDatum : this.getDataSpec().getPredictionData()) {
            writePredictionLine(sb, predictionDatum);
        }
        return sb.toString();
    }

    private void writePredictionLine(StringBuilder sb, InputsAndTarget predictionDatum) {
        sb.append(predictionDatum.getDate()).append(" ");
        sb.append("NULL").append(" ");

        this.setInputs(predictionDatum.getInputs());
        BigDecimal trainedEstimate = this.getOutput();
        trainedEstimate = this.getDataSpec().denormalize(trainedEstimate);
        sb.append(trainedEstimate.doubleValue()).append(" ");

        sb.append("NULL").append("\n");
    }

    private void writeDataLine(StringBuilder sb, InputsAndTarget trainDatum) {
        sb.append(trainDatum.getDate()).append(" ");

        BigDecimal target = trainDatum.getTarget();
        target = this.getDataSpec().denormalize(target);
        sb.append(target.doubleValue()).append(" ");

        this.setInputs(trainDatum.getInputs());
        BigDecimal trainedEstimate = this.getOutput();
        trainedEstimate = this.getDataSpec().denormalize(trainedEstimate);
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
                BigDecimal delta = BigDecimal.valueOf(SIGMOID_ALPHA).multiply(lastOutput).multiply(BigDecimal.ONE.subtract(lastOutput));
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
        return BigDecimal.valueOf(SIGMOID_ALPHA).multiply(last).multiply(oneMinusLast).multiply(desiredMinusLast);
    }

    private class TrainingStats {
        public TrainingStopReason stopReason;
        public boolean brokeAtLocalMax;
        public BigDecimal maxScore;

        public TrainingStats() {
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
                              long trainingTimeLimitMillis,
                              ScaleFunctionType sfType,
                              int maxTries,
                              StatsTracker stats) throws InterruptedException {
        boolean completed = false;
        int tryNumber = 0;
        while (!completed && tryNumber < maxTries) {
            if (trainingTimeLimitMillis <= 0) {
                log.info("Training timeout was {}, which is <= 0, so jobs will not time out.", trainingTimeLimitMillis);
            }
            NetTrainSpecification netSpec = parseLines(trainLines, sfType, verbose);
            MDC.put("performanceCutoff", netSpec.getPerformanceCutoff());
            MDC.put("learningConstant", netSpec.getLearningConstant());
            MDC.put("minTrainingRounds", netSpec.getMinTrainingRounds());
            RectNetFixed net = new RectNetFixed(netSpec.getDepth(), netSpec.getSide());
            net.setData(netSpec.getNetData());
            net.setTimingInfo(TimingInfo.withDuration(trainingTimeLimitMillis));
            // Actually do the training part
            TrainingStats trainingStats = new TrainingStats();

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
                    logStatSnapshot(name, stats, netSpec, net, trainingStats, fileIteration, score, inputsAndTargets);
                }

            }
            MDC.put("netScore", score.round(new MathContext(4)).toString());
            MDC.put("roundsTrained", fileIteration);
            if (trainingStats.brokeAtLocalMax) {
                long timeExpired = System.currentTimeMillis() - net.timingInfo.getStartTime();
                long timeRemaining = trainingTimeLimitMillis - timeExpired;
                log.info("Retraining net from file {} with {} remaining.", name, TimeUtils.formatSeconds((int)timeRemaining/1000));
            } else {
                logStatSnapshot(name, stats, netSpec, net, trainingStats, fileIteration, score, inputsAndTargets);
                return net;
            }
        }
        // tried N times, now we have to give up :(.
        throw new IllegalStateException("Unable to train file " + name + "!");
    }

    private void logStatSnapshot(String name, StatsTracker stats, NetTrainSpecification netSpec, RectNetFixed net,
            TrainingStats trainingStats, int fileIteration, BigDecimal score, List<InputsAndTarget> inputsAndTargets) {
        MDC.put("netScore", score.round(new MathContext(4)).toString());
        MDC.put("roundsTrained", fileIteration);
        double rmsError = computeRmsError(net, inputsAndTargets);
        log.debug("Net {} has trained for {} rounds, RMS Error: {}", name, fileIteration, rmsError);
        net.trainingSummary = new TrainingSummary(name, netSpec.getNetData().getTrainData().get(0).getInputs().length, netSpec.getNumberFileIterations(), netSpec.getLearningConstant().doubleValue(), trainingStats.stopReason, (int) (System.currentTimeMillis() - net.timingInfo.getStartTime()), fileIteration, rmsError);

        stats.addSnapshot(new Snapshot(fileIteration, System.currentTimeMillis() - net.timingInfo.getStartTime(),
                netSpec.getNumberFileIterations(), name, netSpec.getLearningConstant().doubleValue(), true,
                trainingStats.stopReason, rmsError, netSpec.getPerformanceCutoff().doubleValue()));
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
}
