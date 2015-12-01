package com.augurworks.alfred;

import java.math.BigDecimal;

public class WeightDelta {
    private BigDecimal[] outputDeltas;
    // Indexed by innerDeltas[rightCol][rightRow][leftRow]
    private BigDecimal[][][] innerDeltas;

    public WeightDelta(BigDecimal[] od, BigDecimal[][][] id) {
        this.outputDeltas = od;
        this.innerDeltas = id;
    }

    public WeightDelta(int depth, int numInputs) {
        this.outputDeltas = new BigDecimal[numInputs];
        this.innerDeltas = new BigDecimal[depth][numInputs][numInputs];
    }

    public BigDecimal[] getOutputDeltas() {
        return this.outputDeltas;
    }

    public BigDecimal[][][] getInnerDeltas() {
        return this.innerDeltas;
    }

    public void changeOutputDelta(BigDecimal dw, int loc) {
        this.outputDeltas[loc] = this.outputDeltas[loc].add(dw);
    }

    public void changeInnerDelta(BigDecimal dw, int rc, int rr, int lr) {
        this.innerDeltas[rc][rr][lr] = this.innerDeltas[rc][rr][lr].add(dw);
    }

    public BigDecimal getOutputDelta(int loc) {
        return this.outputDeltas[loc];
    }

    public BigDecimal getInnerDelta(int rc, int rr, int lr) {
        return this.innerDeltas[rc][rr][lr];
    }
}
