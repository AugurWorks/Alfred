package com.augurworks.alfred;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents and input to the Net. Has a constant output that can be set.
 *
 * @author saf
 *
 */
@Data
public class InputImpl implements Input {

    private BigDecimal value = BigDecimal.ZERO;

    /**
     * Returns the value of this Input
     *
     * @param code
     *            unused.
     * @return the value of this input
     */
    public BigDecimal getOutput(int code) {
        return this.value;
    }

    /**
     * Returns the value of this Input.
     *
     * @return the value of this input.
     */
    public BigDecimal getOutput() {
        return this.value;
    }
}
