package com.augurworks.alfred.util;

import com.augurworks.alfred.NetTrainSpecification;
import com.augurworks.alfred.scaling.ScaleFunctions;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

public class FileParser {

    public static NetTrainSpecification parseLines(List<String> augtrain, ScaleFunctions.ScaleFunctionType sfType) {
        NetTrainSpecification.Builder netTrainingSpecBuilder = new NetTrainSpecification.Builder();
        netTrainingSpecBuilder.scaleFunctionType(sfType);
        Validate.isTrue(augtrain.size() >= 4, "Cannot parse file with no data");

        Iterator<String> fileLineIterator = augtrain.iterator();
        parseSizeLine(netTrainingSpecBuilder, fileLineIterator);
        parseTrainingInfoLine(netTrainingSpecBuilder, fileLineIterator);
        // skip the titles line
        fileLineIterator.next();
        while (fileLineIterator.hasNext()) {
            parseDataLine(netTrainingSpecBuilder, fileLineIterator);
        }
        return netTrainingSpecBuilder.build();
    }

    private static void parseDataLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String dataLine = fileLineIterator.next();
        String[] dataLineSplit = dataLine.split(" ");
        String date = dataLineSplit[0];
        String target = dataLineSplit[1];
        // inputs
        BigDecimal[] input = new BigDecimal[netTrainingSpec.getSide()];
        dataLineSplit = dataLineSplit[2].split(",");
        for (int i = 0; i < netTrainingSpec.getSide(); i++) {
            input[i] = BigDecimal.valueOf(Double.valueOf(dataLineSplit[i]));
        }

        if (target.equalsIgnoreCase("NULL")) {
            netTrainingSpec.addPredictionRow(input, date);
        } else {
            BigDecimal targetVal = BigDecimal.valueOf(Double.valueOf(target));
            netTrainingSpec.addInputAndTarget(input, targetVal, date);
        }
    }

    private static void parseTrainingInfoLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String trainingInfoLine = fileLineIterator.next();
        String[] trainingInfoLineSplit = trainingInfoLine.split(" ");
        trainingInfoLineSplit = trainingInfoLineSplit[1].split(",");
        int rowInterations = Integer.valueOf(trainingInfoLineSplit[0]);
        int fileIterations = Integer.valueOf(trainingInfoLineSplit[1]);
        BigDecimal learningConstant = BigDecimal.valueOf(Double.valueOf(trainingInfoLineSplit[2]));
        int minTrainingRounds = Integer.valueOf(trainingInfoLineSplit[3]);
        BigDecimal cutoff = BigDecimal.valueOf(Double.valueOf(trainingInfoLineSplit[4]));

        netTrainingSpec.minTrainingRounds(minTrainingRounds);
        netTrainingSpec.learningConstant(learningConstant);
        netTrainingSpec.rowIterations(rowInterations);
        netTrainingSpec.fileIterations(fileIterations);
        netTrainingSpec.performanceCutoff(cutoff);
    }

    private static void parseSizeLine(
            NetTrainSpecification.Builder netTrainingSpec,
            Iterator<String> fileLineIterator) {
        String sizeLine = fileLineIterator.next();
        String[] sizeLineSplit = sizeLine.split(" ");
        sizeLineSplit = sizeLineSplit[1].split(",");
        netTrainingSpec.side(Integer.valueOf(sizeLineSplit[0]));
        netTrainingSpec.depth(Integer.valueOf(sizeLineSplit[1]));
    }
}
