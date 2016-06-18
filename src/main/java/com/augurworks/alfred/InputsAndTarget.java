package com.augurworks.alfred;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InputsAndTarget {

    private final String date;
    private final BigDecimal target;
    private final BigDecimal[] inputs;
}
