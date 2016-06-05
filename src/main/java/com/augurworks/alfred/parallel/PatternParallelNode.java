package com.augurworks.alfred.parallel;

import com.augurworks.alfred.RectNetFixed;
import com.augurworks.alfred.WeightDelta;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

public class PatternParallelNode extends RectNetFixed implements
        Callable<WeightDelta> {

    Logger log = LoggerFactory.getLogger(PatternParallelNode.class);

    private BigDecimal[][] trainingData;
    private BigDecimal[] desired;
    private int iterations;
    private BigDecimal learningConstant;

    /**
     * Should be handed: 1) Training set 2) Number of rounds 3) Current net to
     * copy
     */
    public PatternParallelNode(RectNetFixed r, BigDecimal[][] inputs,
            BigDecimal[] desired, int iterations, BigDecimal learningConstant) {
        super(r.getX(), r.getY(), null);
        init(r);
        this.trainingData = inputs;
        this.desired = desired;
        this.iterations = iterations;
        this.learningConstant = learningConstant;
    }

    private void init(RectNetFixed r) {
        // Copy the old net
        for (int leftCol = 0; leftCol < this.x - 1; leftCol++) {
            int rightCol = leftCol + 1;
            for (int leftRow = 0; leftRow < this.y; leftRow++) {
                for (int rightRow = 0; rightRow < this.y; rightRow++) {
                    BigDecimal w = r
                            .getWeight(leftCol, leftRow, rightCol, rightRow);
                    this.setWeight(leftCol, leftRow, rightCol, rightRow, w);
                }
            }
        }
        for (int j = 0; j < this.y; j++) {
            BigDecimal w = r.getOutputNeuronWeight(j);
            this.setOutputNeuronWeight(j, w);
        }
    }

    public void train(BigDecimal[] inputs, BigDecimal desired, int iterations,
            BigDecimal learningConstant, WeightDelta wd) {
        Validate.isTrue(iterations > 0);
        for (int lcv = 0; lcv < iterations; lcv++) {
            // Set the inputs
            this.setInputs(inputs);
            // Compute the last node error
            BigDecimal deltaF = this.getOutputError(desired);
            if (verbose) {
                log.info("DeltaF: {}", deltaF);
            }
            // For each interior node, compute the weighted error
            // deltas are of the form
            // delta[col][row]
            BigDecimal[][] deltas = new BigDecimal[this.x + 1][this.y];
            // spoof the rightmost deltas
            for (int j = 0; j < y; j++) {
                deltas[this.x][j] = deltaF;
            }
            int leftCol = 0;
            int leftRow = 0;
            int rightCol = 0;
            int rightRow = 0;
            for (leftCol = this.x - 1; leftCol >= 0; leftCol--) {
                rightCol = leftCol + 1;
                for (leftRow = 0; leftRow < this.y; leftRow++) {
                    BigDecimal lastOutput = this.neurons[leftCol][leftRow]
                            .getLastOutput();
                    // since we're using alpha = 3 in the neurons
                    // 3 * lastOutput * (1 - lastOutput);
                    BigDecimal delta = BigDecimal.valueOf(3).multiply(lastOutput).multiply(BigDecimal.ONE.subtract(lastOutput));
                    BigDecimal summedRightWeightDelta = BigDecimal.ZERO;
                    for (rightRow = 0; rightRow < this.y; rightRow++) {
                        if (rightCol == this.x) {
                            summedRightWeightDelta = summedRightWeightDelta.add(this.output
                                    .getWeight(leftRow).multiply(deltaF));
                            // without the break, we were adding too many of the
                            // contributions of the output node when computing
                            // the deltas value for the layer immediately left
                            // of it.
                            break;
                        } else {
                            // summing w * delta
                            summedRightWeightDelta = summedRightWeightDelta.add(getWeight(leftCol,
                                    leftRow, rightCol, rightRow)).multiply(
                                    deltas[rightCol][rightRow]);
                        }
                    }
                    deltas[leftCol][leftRow] = delta.multiply(summedRightWeightDelta);
                    if (verbose) {
                        log.info("leftCol: {}, leftRow: {}, lo*(1-lo): {}", leftCol, leftRow, delta);
                        log.info("leftCol: {}, leftRow: {}, srwd: {}", leftCol, leftRow, summedRightWeightDelta);
                        log.info("leftCol: {}, leftRow: {}, delta: {}", leftCol, leftRow, deltas[leftCol][leftRow]);
                    }
                }
            }
            // now that we have the deltas, we can change the weights
            // again, we special case the last neuron
            for (int j = 0; j < this.y; j++) {
                // w' = w + r*i*delta
                // r is the learning constant
                // i is the output from the leftward neuron
                BigDecimal dw = learningConstant.multiply(
                        this.neurons[this.x - 1][j].getLastOutput()).multiply(deltaF);
                this.output.changeWeight(j, dw);
                wd.changeOutputDelta(dw, j);
            }
            // now we do the same for the internal nodes
            for (leftCol = this.x - 2; leftCol >= 0; leftCol--) {
                rightCol = leftCol + 1;
                for (leftRow = 0; leftRow < this.y; leftRow++) {
                    for (rightRow = 0; rightRow < this.y; rightRow++) {
                        // w' = w + r*i*delta
                        // r is the learning constant
                        // i is the output from the leftward neuron
                        BigDecimal dw = learningConstant.multiply(
                                this.neurons[leftCol][leftRow]
                                        .getLastOutput()).multiply(
                                deltas[rightCol][rightRow]);
                        this.neurons[rightCol][rightRow].changeWeight(leftRow,
                                dw);
                        wd.changeInnerDelta(dw, rightCol, rightRow, leftRow);
                        if (verbose) {
                            log.info("{},{}->{},{}", leftCol, leftRow, rightCol, rightRow);
                            log.info("{}", this.neurons[rightCol][rightRow].getWeight(leftRow));
                        }
                    }
                }
            }
        }
    }

    public void train(BigDecimal[][] inputs, BigDecimal[] desired, int iterations,
            BigDecimal learningConstant, WeightDelta wd) {
        for (int i = 0; i < inputs.length; i++) {
            train(inputs[i], desired[i], iterations, learningConstant, wd);
        }
    }

    @Override
    public WeightDelta call() {
        WeightDelta wd = new WeightDelta(this.getX(), this.getY());
        this.train(trainingData, desired, iterations, learningConstant, wd);
        return wd;
    }
}
