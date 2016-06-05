package com.augurworks.alfred;

import com.augurworks.alfred.scaling.ScaleFunctions.ScaleFunctionType;

import java.math.BigDecimal;

public class NetTrainSpecification {

    private final NetDataSpecification netData;
    private final int depth;
    private final int side;
    private final int numberRowIterations;
    private final int numberFileIterations;
    private final BigDecimal learningConstant;
    private final int minTrainingRounds;
    private final BigDecimal performanceCutoff;

    public NetTrainSpecification(NetDataSpecification netData, int depth, int side,
            int numberRowIterations, int numberFileIterations,
            BigDecimal learningConstant, int minTrainingRounds,
            BigDecimal performanceCutoff) {
        this.netData = netData;
        this.depth = depth;
        this.side = side;
        this.numberRowIterations = numberRowIterations;
        this.numberFileIterations = numberFileIterations;
        this.learningConstant = learningConstant;
        this.minTrainingRounds = minTrainingRounds;
        this.performanceCutoff = performanceCutoff;
    }

    public static class Builder {
        private NetDataSpecification.Builder dataBuilder = new NetDataSpecification.Builder();
        private int depth;
        private int numberRowIterations;
        private int numberFileIterations;
        private BigDecimal learningConstant;
        private int minTrainingRounds;
        private BigDecimal performanceCutoff;
        private int side;
        private ScaleFunctionType sfType;

        public Builder rowIterations(int numberRowIterations) {
            this.numberRowIterations = numberRowIterations;
            return this;
        }

        public Builder fileIterations(int numberFileIterations) {
            this.numberFileIterations = numberFileIterations;
            return this;
        }

        public Builder learningConstant(BigDecimal learningConstant) {
            this.learningConstant = learningConstant;
            return this;
        }

        public Builder minTrainingRounds(int minTrainingRounds) {
            this.minTrainingRounds = minTrainingRounds;
            return this;
        }

        public Builder performanceCutoff(BigDecimal performanceCutoff) {
            this.performanceCutoff = performanceCutoff;
            return this;
        }

        public Builder addInputAndTarget(BigDecimal[] inputs, BigDecimal target, String date) {
            this.dataBuilder.addDataRow(date, target, inputs);
            return this;
        }

        public Builder addPredictionRow(BigDecimal[] inputs, String date) {
            this.dataBuilder.addPredictionRow(date, inputs);
            return this;
        }

        public Builder depth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder side(int side) {
            this.side = side;
            return this;
        }

        public int getSide() {
            return side;
        }

        public NetTrainSpecification build() {
            NetDataSpecification data = dataBuilder.build(sfType);
            return new NetTrainSpecification(data, depth, side, numberRowIterations,
                    numberFileIterations, learningConstant, minTrainingRounds, performanceCutoff);
        }

        public void scaleFunctionType(ScaleFunctionType sfTypeParam) {
            this.sfType = sfTypeParam;
        }
    }

    public NetDataSpecification getNetData() {
        return netData;
    }

    public int getDepth() {
        return depth;
    }

    public int getSide() {
        return side;
    }

    public int getNumberRowIterations() {
        return numberRowIterations;
    }

    public int getNumberFileIterations() {
        return numberFileIterations;
    }

    public BigDecimal getLearningConstant() {
        return learningConstant;
    }

    public int getMinTrainingRounds() {
        return minTrainingRounds;
    }

    public BigDecimal getPerformanceCutoff() {
        return performanceCutoff;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + depth;
        result = prime
                * result
                + ((learningConstant == null) ? 0 : learningConstant.hashCode());
        result = prime * result + minTrainingRounds;
        result = prime * result + ((netData == null) ? 0 : netData.hashCode());
        result = prime * result + numberFileIterations;
        result = prime * result + numberRowIterations;
        result = prime
                * result
                + ((performanceCutoff == null) ? 0 : performanceCutoff
                        .hashCode());
        result = prime * result + side;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NetTrainSpecification other = (NetTrainSpecification) obj;
        if (depth != other.depth)
            return false;
        if (learningConstant == null) {
            if (other.learningConstant != null)
                return false;
        } else if (!learningConstant.equals(other.learningConstant))
            return false;
        if (minTrainingRounds != other.minTrainingRounds)
            return false;
        if (netData == null) {
            if (other.netData != null)
                return false;
        } else if (!netData.equals(other.netData))
            return false;
        if (numberFileIterations != other.numberFileIterations)
            return false;
        if (numberRowIterations != other.numberRowIterations)
            return false;
        if (performanceCutoff == null) {
            if (other.performanceCutoff != null)
                return false;
        } else if (!performanceCutoff.equals(other.performanceCutoff))
            return false;
        return side == other.side;
    }

    @Override
    public String toString() {
        return "NetTrainSpecification [netData=" + netData + ", depth=" + depth
                + ", side=" + side + ", numberRowIterations="
                + numberRowIterations + ", numberFileIterations="
                + numberFileIterations + ", learningConstant="
                + learningConstant + ", minTrainingRounds=" + minTrainingRounds
                + ", performanceCutoff=" + performanceCutoff
                + "]";
    }

}
