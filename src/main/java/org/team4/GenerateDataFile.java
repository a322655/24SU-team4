package org.team4;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenerateDataFile {
    private static final Logger logger = Logger.getLogger(GenerateDataFile.class.getName());

    public static void main(String[] args) {
        String filename = "data/data.txt";
        int numIntervals = 10;
        int numDataPoints = 100;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < numIntervals; i++) {
                int start = i * 10;
                int end = start + 10;
                writer.write(start + "," + end);
                writer.newLine();
            }

            writer.newLine();

            Random random = new Random();
            for (int i = 0; i < numDataPoints; i++) {
                int dataPoint = random.nextInt(100) + 1;
                writer.write(String.valueOf(dataPoint));
                writer.newLine();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing file", e);
        }
    }
}