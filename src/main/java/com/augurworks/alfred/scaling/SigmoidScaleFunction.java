package com.augurworks.alfred.scaling;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class SigmoidScaleFunction implements ScaleFunction {

    private final double mean;
    private final double standardDev;

    public SigmoidScaleFunction(List<BigDecimal> inputs) {
        this.mean = computeMean(inputs);
        this.standardDev = computeStandardDev(inputs, mean);
    }

    @Override
    public double normalize(double value) {
        return (1.0 / (1.0 + Math.exp(-1.0*(value - mean)/standardDev)));
    }

    @Override
    public double denormalize(double value) {
        return (-1.0 * standardDev * Math.log((1.0 / value) - 1)) + mean;
    }

    private static double computeMean(List<BigDecimal> inputs) {
        double total = 0;
        for (BigDecimal input : inputs) {
            total += input.doubleValue();
        }
        return total / (double)inputs.size();
    }

    private static double computeStandardDev(List<BigDecimal> inputs, double mean) {
        double[] values = new double[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            values[i] = inputs.get(i).doubleValue();
        }
        return new StandardDeviation().evaluate(values, mean);
    }

}
