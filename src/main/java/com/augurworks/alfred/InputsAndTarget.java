package com.augurworks.alfred;

import java.math.BigDecimal;

public class InputsAndTarget {

    private final String date;
    private final BigDecimal target;
    private final BigDecimal[] inputs;

    private InputsAndTarget(String date,
                            BigDecimal target,
                            BigDecimal[] inputs) {
        this.target = target;
        this.inputs = inputs;
        this.date = date;
    }

    public static InputsAndTarget withTarget(String date,
                                             BigDecimal[] inputs,
                                             BigDecimal target) {
        return new InputsAndTarget(date, target, inputs);
    }

    public static InputsAndTarget withoutTarget(String date, BigDecimal[] inputs) {
        return new InputsAndTarget(date, null, inputs);
    }

    public BigDecimal getTarget() {
        return target;
    }

    public BigDecimal[] getInputs() {
        return inputs;
    }

    public String getDate() {
        return date;
    }

    public boolean hasTarget() {
        return target != null;
    }

}
