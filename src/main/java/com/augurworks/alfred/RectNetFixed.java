package com.augurworks.alfred;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;
import com.augurworks.alfred.stats.TrainingStage;
import com.augurworks.alfred.stats.TrainingStat;
import com.augurworks.alfred.util.BigDecimals;
import com.augurworks.alfred.util.FileParser;
import com.augurworks.alfred.util.TimeUtils;

import lombok.Data;


/**
 * Simple rectangular neural network.
 *
 * @author saf
 *
 */
@Data
public class RectNetFixed {

    private static Logger log = LoggerFactory.getLogger(RectNetFixed.class);

    private static final double NEGATIVE_INFINITY = -1000000;
    public static final double SIGMOID_ALPHA = 3;

    private final String name;
    private final NetTrainSpecification netSpec;

    // Inputs to network
    protected InputImpl[] inputs;
    // Every neuron with the same i is in the
    // same "layer". Indexed as [col][row].
    protected FixedNeuron[][] neurons;
    // There's only one final output neuron
    // since this is built to make booleans.
    protected FixedNeuron output;
    private TimingInfo timingInfo;

    private List<TrainingStat> trainingStats = new ArrayList<>();
    private TrainingStopReason trainingStopReason;
    public boolean brokeAtLocalMax;
    public BigDecimal maxScore = BigDecimal.valueOf(NEGATIVE_INFINITY);

    /**
     * Constructs a new RectNet with 10 inputs and 5 layers of network.
     */
    public RectNetFixed(String netId, List<String> trainLines, ScaleFunctionType scaleFunctionType) {
        this.netSpec = FileParser.parseLines(trainLines, scaleFunctionType);
        if (netSpec.getDepth() < 1 || netSpec.getSide() < 1) {
            throw new IllegalArgumentException("Depth and numInputs must be >= 1");
        }
        this.name = netId;
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
        Validate.isTrue(leftCol < this.netSpec.getDepth());
        Validate.isTrue(rightCol < this.netSpec.getDepth());
        Validate.isTrue(leftRow < this.netSpec.getSide());
        Validate.isTrue(rightRow < this.netSpec.getSide());
        return this.neurons[rightCol][rightRow].getWeight(leftRow);
    }

    public TimingInfo getTimingInfo() {
        return this.timingInfo;
    }

    public boolean hasTimeExpired() {
        return getTimingInfo().hasTimeExpired();
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
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            this.neurons[0][j].addInput(this.inputs[j], BigDecimal.ONE);
        }
        // Make connections between neurons and neurons.
        for (int leftCol = 0; leftCol < this.netSpec.getDepth() - 1; leftCol++) {
            int rightCol = leftCol + 1;
            for (int leftRow = 0; leftRow < this.netSpec.getSide(); leftRow++) {
                for (int rightRow = 0; rightRow < this.netSpec.getSide(); rightRow++) {
                    this.neurons[rightCol][rightRow].addInput(
                            this.neurons[leftCol][leftRow], initNum());
                }
            }
        }
        // Make connections between output and neurons.
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            this.output.addInput(this.neurons[this.netSpec.getDepth() - 1][j], initNum());
        }
    }

    private void initEmptyNeurons() {
        // Initialize arrays to blank neurons and inputs.
        this.inputs = new InputImpl[this.netSpec.getSide()];
        this.neurons = new FixedNeuron[this.getNetSpec().getDepth()][this.netSpec.getSide()];
        this.output = new FixedNeuron(this.netSpec.getSide());
        // Name the neurons for possible debug. This is not a critical
        // step.
        this.output.setName("output");
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            this.inputs[j] = new InputImpl();
            // initialize the first row
            this.neurons[0][j] = new FixedNeuron(1);
            this.neurons[0][j].setName("(" + 0 + "," + j + ")");
            for (int i = 1; i < this.netSpec.getDepth(); i++) {
                this.neurons[i][j] = new FixedNeuron(this.netSpec.getSide());
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
        Validate.isTrue(inputs.length == this.netSpec.getSide());
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            this.inputs[j].setValue(inputs[j]);
        }
    }

    /**
     * Returns the output value from this network run.
     */
    public BigDecimal getOutput() {
        log.debug("Getting Alfred output");
        BigDecimal[] outs = new BigDecimal[this.netSpec.getSide()];
        BigDecimal[] ins = new BigDecimal[this.netSpec.getSide()];
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            // require recursion (depth = 0) here.
            ins[j] = this.neurons[0][j].getOutput();
        }
        // indexing must start at 1, because we've already computed
        // the output from the 0th row in the previous 3 lines.
        for (int i = 1; i < this.netSpec.getDepth(); i++) {
            for (int j = 0; j < this.netSpec.getSide(); j++) {
                outs[j] = this.neurons[i][j].getOutput(ins);
            }
            ins = outs;
            outs = new BigDecimal[this.netSpec.getSide()];
        }
        log.debug("Alfred output calculations complete");
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
        Validate.isTrue(inputs.length == this.netSpec.getSide());
        for (int lcv = 0; lcv < iterations && !hasTimeExpired(); lcv++) {
            doIteration(inputs, desired, learningConstant);
        }
    }

    /**
     * Denormalizes targets and estimates.
     */
    public String getAugout() {
        StringBuilder sb = new StringBuilder();
        for (InputsAndTarget trainDatum : this.getNetSpec().getNetData().getTrainData()) {
            writeDataLine(sb, trainDatum);
        }
        for (InputsAndTarget predictionDatum : this.getNetSpec().getNetData().getPredictionData()) {
            writePredictionLine(sb, predictionDatum);
        }
        return sb.toString();
    }

    private void writePredictionLine(StringBuilder sb, InputsAndTarget predictionDatum) {
        sb.append(predictionDatum.getDate()).append(" ");
        sb.append("NULL").append(" ");

        this.setInputs(predictionDatum.getInputs());
        BigDecimal trainedEstimate = this.getOutput();
        trainedEstimate = this.getNetSpec().getNetData().denormalize(trainedEstimate);
        sb.append(trainedEstimate.doubleValue()).append(" ");

        sb.append("NULL").append("\n");
    }

    private void writeDataLine(StringBuilder sb, InputsAndTarget trainDatum) {
        sb.append(trainDatum.getDate()).append(" ");

        BigDecimal target = trainDatum.getTarget();
        target = this.getNetSpec().getNetData().denormalize(target);
        sb.append(target.doubleValue()).append(" ");

        this.setInputs(trainDatum.getInputs());
        BigDecimal trainedEstimate = this.getOutput();
        trainedEstimate = this.getNetSpec().getNetData().denormalize(trainedEstimate);
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
        for (leftCol = this.netSpec.getDepth() - 2; leftCol >= 0; leftCol--) {
            rightCol = leftCol + 1;
            for (leftRow = 0; leftRow < this.netSpec.getSide(); leftRow++) {
                for (rightRow = 0; rightRow < this.netSpec.getSide(); rightRow++) {
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
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            // w' = w + r*i*delta
            // r is the learning constant
            // i is the output from the leftward neuron
            BigDecimal dw = learningConstant.multiply(this.neurons[this.netSpec.getDepth() - 1][j].getLastOutput()).multiply(deltaF);
            this.output.changeWeight(j, dw);
        }
    }

    private BigDecimal[][] computeInteriorDeltas(BigDecimal deltaF) {
        BigDecimal[][] deltas = new BigDecimal[this.netSpec.getDepth() + 1][this.netSpec.getSide()];
        // spoof the rightmost deltas
        for (int j = 0; j < this.netSpec.getSide(); j++) {
            deltas[this.netSpec.getDepth()][j] = deltaF;
        }
        int leftCol;
        int leftRow;
        int rightCol;
        int rightRow;
        for (leftCol = this.netSpec.getDepth() - 1; leftCol >= 0; leftCol--) {
            rightCol = leftCol + 1;
            for (leftRow = 0; leftRow < this.netSpec.getSide(); leftRow++) {
                BigDecimal lastOutput = this.neurons[leftCol][leftRow].getLastOutput();
                // since we're using alpha = 3 in the neurons
                // 3 * lastOutput * (1 - lastOutput);
                BigDecimal delta = BigDecimal.valueOf(SIGMOID_ALPHA).multiply(lastOutput).multiply(BigDecimal.ONE.subtract(lastOutput));
                BigDecimal summedRightWeightDelta = BigDecimal.ZERO;
                for (rightRow = 0; rightRow < this.netSpec.getSide(); rightRow++) {
                    if (rightCol == this.netSpec.getDepth()) {
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

    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Job detected interrupt flag");
        }
    }

    /**
     * Train a neural network from a .augtrain training file
     *
     * @param trainingTimeLimitMillis
     * @param maxTries
     * @return The trained neural network
     * @throws InterruptedException
     */
    public RectNetFixed train(long trainingTimeLimitMillis, int maxTries) throws InterruptedException {
        log.info("Starting Alfred training");
        int tryNumber = 0;
        while (tryNumber < maxTries) {
            if (trainingTimeLimitMillis <= 0) {
                log.info("Training timeout was {}, which is <= 0, so jobs will not time out.", trainingTimeLimitMillis);
            }
            MDC.put("performanceCutoff", netSpec.getPerformanceCutoff());
            MDC.put("learningConstant", netSpec.getLearningConstant());
            MDC.put("minTrainingRounds", netSpec.getMinTrainingRounds());
            this.timingInfo = TimingInfo.withDuration(trainingTimeLimitMillis);
            // Actually do the training part

            int fileIteration;
            BigDecimal score = null;

            List<InputsAndTarget> inputsAndTargets = netSpec.getNetData().getTrainData();
            logStatSnapshot(0, null, inputsAndTargets, TrainingStage.STARTING, Optional.empty());
            for (fileIteration = 0; fileIteration < netSpec.getNumberFileIterations(); fileIteration++) {

                // train all data rows for numberRowIterations times.
                for (int lcv = 0; lcv < inputsAndTargets.size() && !this.hasTimeExpired(); lcv++) {
                    InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
                    this.train(inputsAndTarget.getInputs(),
                              inputsAndTarget.getTarget(),
                              netSpec.getNumberRowIterations(),
                              netSpec.getLearningConstant());
                }

                if (this.hasTimeExpired()) {
                    log.debug("Breaking training because time ran out");
                    this.trainingStopReason = TrainingStopReason.OUT_OF_TIME;
                    break;
                }

                // compute total score
                score = BigDecimal.ZERO;
                for (int lcv = 0; lcv < inputsAndTargets.size(); lcv++) {
                    InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
                    this.setInputs(inputsAndTarget.getInputs());
                    // Math.pow((targets.get(lcv) - r.getOutput()), 2)
                    BigDecimal difference = inputsAndTarget.getTarget().subtract(this.getOutput());
                    score = score.add(difference.multiply(difference));
                }
                double errorsSquared = score.doubleValue();
                double rmsError = getRmsError(inputsAndTargets.size(), errorsSquared);
                score = score.multiply(BigDecimal.valueOf(-1.0));
                score = score.divide(BigDecimal.valueOf(inputsAndTargets.size()), BigDecimals.MATH_CONTEXT);

                // if (score > -1 * cutoff)
                //   ==> if (score - (-1 * cutoff) > 0)
                //     ==> if (min(score - (-1 * cutoff), 0) == 0)
                if (BigDecimal.ZERO.min(
                        score.subtract(BigDecimal.valueOf(-1.0).multiply(netSpec.getPerformanceCutoff())))
                            .equals(BigDecimal.ZERO)) {
                    this.trainingStopReason = TrainingStopReason.HIT_PERFORMANCE_CUTOFF;
                    break;
                }
                // if (score > maxScore)
                //   ==> if (score - maxScore > 0)
                //     ==> if (min(score - maxScore, 0) == 0)
                if (BigDecimal.ZERO.min(score.subtract(this.maxScore)).equals(BigDecimal.ZERO)) {
                    this.maxScore = score;
                } else if (fileIteration < netSpec.getMinTrainingRounds()) {
                    continue;
                } else {
                    log.debug("Breaking training at a local max");
                    this.brokeAtLocalMax = true;
                    break;
                }

                if (fileIteration % 100 == 0) {
                    logStatSnapshot(fileIteration, score, inputsAndTargets, TrainingStage.RUNNING, Optional.of(rmsError));
                }

            }
            if (this.trainingStopReason == null) {
                log.debug("Breaking training because the training limit was hit");
                this.trainingStopReason = TrainingStopReason.HIT_TRAINING_LIMIT;
            }
            MDC.put("netScore", score.round(new MathContext(4)).toString());
            MDC.put("roundsTrained", fileIteration);
            if (this.brokeAtLocalMax) {
                long timeExpired = System.currentTimeMillis() - this.timingInfo.getStartTime();
                long timeRemaining = trainingTimeLimitMillis - timeExpired;
                log.info("Retraining net from file {} with {} remaining.", name, TimeUtils.formatSeconds((int)timeRemaining/1000));
            } else {
                log.debug("Finished Alfred training");
                logStatSnapshot(fileIteration, score, inputsAndTargets, TrainingStage.DONE, Optional.empty());
                return this;
            }
        }
        // tried N times, now we have to give up :(.
        throw new IllegalStateException("Unable to train file " + name + "!");
    }

    private double getRmsError(int size, double errorsSquared) {
        if (errorsSquared == 0) {
            return 0;
        } else {
            return Math.sqrt(errorsSquared / (1.0 * size));
        }
    }

    private void logStatSnapshot(int fileIteration, BigDecimal score, List<InputsAndTarget> inputsAndTargets, TrainingStage trainingStage, Optional<Double> rmsError) {
        MDC.put("netScore", score == null ? null : score.round(new MathContext(4)).toString());
        MDC.put("roundsTrained", fileIteration);
        double rmsErrorValue;
        if (rmsError.isPresent()) {
            rmsErrorValue = rmsError.get();
        } else {
            rmsErrorValue = computeRmsError(inputsAndTargets);
        }
        log.debug("Net {} has trained for {} rounds, RMS Error: {}", this.name, fileIteration, rmsError);
        TrainingStat trainingStat = new TrainingStat(this.name, this.netSpec.getNetData().getTrainData().get(0).getInputs().length, this.netSpec.getLearningConstant().doubleValue(), this.netSpec.getNumberRowIterations());
        trainingStat.setRmsError(rmsErrorValue);
        trainingStat.setSecondsElapsed((int) (System.currentTimeMillis() - this.timingInfo.getStartTime()) / 1000);
        trainingStat.setRoundsTrained(fileIteration);
        trainingStat.setTrainingStage(trainingStage);
        if (this.trainingStopReason != null) {
            trainingStat.setTrainingStopReason(this.trainingStopReason);
        }
        this.trainingStats.add(trainingStat);
    }

    private double computeRmsError(List<InputsAndTarget> inputsAndTargets) {
        double totalRmsError = 0;
        for (int lcv = 0; lcv < inputsAndTargets.size(); lcv++) {
            InputsAndTarget inputsAndTarget = inputsAndTargets.get(lcv);
            this.setInputs(inputsAndTarget.getInputs());
            double target = inputsAndTarget.getTarget().doubleValue();
            double actual = this.getOutput().doubleValue();
            double square = Math.pow(target - actual, 2);
            totalRmsError += square;
        }
        return getRmsError(inputsAndTargets.size(), totalRmsError);
    }
}
