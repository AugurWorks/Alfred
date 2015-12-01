package com.augurworks.alfred.scaling;

public class LinearScaleFunction implements ScaleFunction {
    private final double min;
    private final double max;
    private final double desiredMin;
    private final double desiredMax;

    public LinearScaleFunction(double min, double max, double desiredMin, double desiredMax) {
        this.min = min;
        this.max = max;
        this.desiredMax = desiredMax;
        this.desiredMin = desiredMin;
    }

    @Override
    public double normalize(double value) {
        return (value - min) / (max - min) * (desiredMax - desiredMin) + desiredMin;
    }

    @Override
    public double denormalize(double value) {
        return (value - desiredMin) / (desiredMax - desiredMin) * (max - min) + min;
    }
}
