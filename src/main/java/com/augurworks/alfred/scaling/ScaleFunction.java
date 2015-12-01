package com.augurworks.alfred.scaling;

public interface ScaleFunction {

    double normalize(double value);

    double denormalize(double value);

}
