package com.augurworks.alfred;

import com.augurworks.alfred.scaling.ScaleFunction;
import com.augurworks.alfred.scaling.ScaleFunctions;
import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NetDataSpecification {

    private final List<InputsAndTarget> trainData;
    private final List<InputsAndTarget> predictionData;
    private final ScaleFunction scaleFunction;

    public NetDataSpecification(List<InputsAndTarget> trainData,
                                List<InputsAndTarget> predictionData,
                                ScaleFunction scaleFunction) {
        this.trainData = trainData;
        this.predictionData = predictionData;
        this.scaleFunction = scaleFunction;
    }

    public static class Builder {

        private List<String> dates = new ArrayList<>();
        private List<BigDecimal> targets = new ArrayList<>();
        private List<BigDecimal[]> inputSets = new ArrayList<>();
        private List<InputsAndTarget> predictionRows = new ArrayList<>();
        private double desiredMin = 0.1;
        private double desiredMax = 0.9;

        public Builder addDataRow(String date, BigDecimal target, BigDecimal[] inputs) {
            dates.add(date);
            targets.add(target);
            inputSets.add(inputs);
            return this;
        }

        public Builder addPredictionRow(String date, BigDecimal[] inputs) {
            predictionRows.add(new InputsAndTarget(date, null, inputs));
            return this;
        }

        private double getMinTarget() {
            double minSeen = Double.POSITIVE_INFINITY;
            for (BigDecimal target : targets) {
                if (target.doubleValue() < minSeen) {
                    minSeen = target.doubleValue();
                }
            }
            return minSeen;
        }

        private double getMaxTarget() {
            double maxSeen = Double.NEGATIVE_INFINITY;
            for (BigDecimal target : targets) {
                if (target.doubleValue() > maxSeen) {
                    maxSeen = target.doubleValue();
                }
            }
            return maxSeen;
        }

        public void normalizeTargetValues(ScaleFunction scaleFunction) {
            for (int i = 0; i < targets.size(); i++) {
                BigDecimal target = targets.get(i);
                targets.set(i, BigDecimal.valueOf(scaleFunction.normalize(target.doubleValue())));
            }
        }

        private List<InputsAndTarget> buildInputs() {
            List<InputsAndTarget> inputsAndTargets = new ArrayList<>();
            for (int i = 0; i < targets.size(); i++) {
                inputsAndTargets.add(new InputsAndTarget(dates.get(i), targets.get(i), inputSets.get(i)));
            }
            return inputsAndTargets;
        }

        /*
         * Normalizes inputs.
         */
        public NetDataSpecification build(ScaleFunctionType sfType) {
            ScaleFunction scaleFunc;
            if (sfType == ScaleFunctionType.LINEAR) {
                scaleFunc = ScaleFunctions.createLinearScaleFunction(
                        getMinTarget(), getMaxTarget(), desiredMin, desiredMax);
            } else if (sfType == ScaleFunctionType.SIGMOID) {
                scaleFunc = ScaleFunctions.createSigmoidScaleFunction(targets);
            } else {
                throw new IllegalArgumentException("Unrecognized function " + sfType);
            }
            normalizeTargetValues(scaleFunc);
            List<InputsAndTarget> inputsAndTargets = buildInputs();
            return new NetDataSpecification(inputsAndTargets, predictionRows, scaleFunc);
        }

    }

    public List<InputsAndTarget> getTrainData() {
        return trainData;
    }

    public List<InputsAndTarget> getPredictionData() {
        return predictionData;
    }

    public BigDecimal denormalize(BigDecimal value) {
        return BigDecimal.valueOf(scaleFunction.denormalize(value.doubleValue()));
    }
}
