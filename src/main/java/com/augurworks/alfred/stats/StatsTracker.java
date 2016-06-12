package com.augurworks.alfred.stats;

import com.augurworks.alfred.TrainingStopReason;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.util.List;
import java.util.Queue;

public class StatsTracker {

    private Queue<Snapshot> cache = Queues.newLinkedBlockingQueue();
    private int maxSize;

    public StatsTracker(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addSnapshot(Snapshot snapshot) {
        cache.add(snapshot);
        if (cache.size() > maxSize) {
            cache.poll();
        }
    }

    public List<Snapshot> getStats() {
        return Lists.newArrayList(cache);
    }

    public static class Snapshot {
        private int roundsTrained;
        private long millisElapsed;
        private int maxRounds;
        private String name;
        private double trainingConstant;
        private boolean isDone;
        private TrainingStopReason completionReason;
        private double residual;
        private double cutoff;

        public Snapshot(int roundsTrained, long millisElapsed, int maxRounds, String name,
                double trainingConstant, boolean isDone, TrainingStopReason completionReason, double residual,
                double cutoff) {
            this.roundsTrained = roundsTrained;
            this.maxRounds = maxRounds;
            this.name = name;
            this.trainingConstant = trainingConstant;
            this.isDone = isDone;
            this.completionReason = completionReason;
            this.residual = residual;
            this.cutoff = cutoff;
        }

        public long getMillisElapsed() {
            return millisElapsed;
        }

        public int getRoundsTrained() {
            return roundsTrained;
        }

        public int getMaxRounds() {
            return maxRounds;
        }

        public String getName() {
            return name;
        }

        public double getTrainingConstant() {
            return trainingConstant;
        }

        public boolean isDone() {
            return isDone;
        }

        public TrainingStopReason getCompletionReason() {
            return completionReason;
        }

        public double getResidual() {
            return residual;
        }

        public double getCutoff() {
            return cutoff;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((completionReason == null) ? 0 : completionReason.hashCode());
            long temp;
            temp = Double.doubleToLongBits(cutoff);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + (isDone ? 1231 : 1237);
            result = prime * result + maxRounds;
            result = prime * result + (int) (millisElapsed ^ (millisElapsed >>> 32));
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            temp = Double.doubleToLongBits(residual);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + roundsTrained;
            temp = Double.doubleToLongBits(trainingConstant);
            result = prime * result + (int) (temp ^ (temp >>> 32));
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
            Snapshot other = (Snapshot) obj;
            if (completionReason != other.completionReason)
                return false;
            if (Double.doubleToLongBits(cutoff) != Double.doubleToLongBits(other.cutoff))
                return false;
            if (isDone != other.isDone)
                return false;
            if (maxRounds != other.maxRounds)
                return false;
            if (millisElapsed != other.millisElapsed)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (Double.doubleToLongBits(residual) != Double.doubleToLongBits(other.residual))
                return false;
            if (roundsTrained != other.roundsTrained)
                return false;
            return Double.doubleToLongBits(trainingConstant) == Double.doubleToLongBits(other.trainingConstant);
        }

    }

}
