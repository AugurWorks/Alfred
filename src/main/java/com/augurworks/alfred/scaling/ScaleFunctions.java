package com.augurworks.alfred.scaling;

import java.math.BigDecimal;
import java.util.List;

public class ScaleFunctions {

    public enum ScaleFunctionType {
        LINEAR,
        SIGMOID,
        ;
    }

    public static ScaleFunction createLinearScaleFunction(double min, double max,
            double desiredMin, double desiredMax) {
        return new LinearScaleFunction(min, max, desiredMin, desiredMax);
    }

    public static ScaleFunction createSigmoidScaleFunction(List<BigDecimal> values) {
        return new SigmoidScaleFunction(values);
    }

}
