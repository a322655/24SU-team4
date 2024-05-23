package org.team4;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistogramIntervals extends JFrame {
    private static final Logger logger = Logger.getLogger(HistogramIntervals.class.getName());

    public HistogramIntervals(String title) {
        super(title);
        String filename = "data/data.txt";
        List<Interval> intervals = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        readIntervalsAndData(filename, intervals, data);

        Collections.sort(data);

        IntervalXYDataset dataset = createDataset(intervals, data);
        JFreeChart chart = createChart(dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private static void readIntervalsAndData(String filename, List<Interval> intervals, List<Integer> data) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean readingData = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    readingData = true;
                    continue;
                }

                if (readingData) {
                    int dataPoint = Integer.parseInt(line);
                    data.add(dataPoint);
                } else {
                    String[] parts = line.split(",");
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);
                    intervals.add(new Interval(start, end));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading file", e);
        }
    }

    private static IntervalXYDataset createDataset(List<Interval> intervals, List<Integer> data) {
        XYSeries series = new XYSeries("Frequency");

        for (Interval interval : intervals) {
            int count = 0;
            for (int value : data) {
                if (interval.contains(value)) {
                    count++;
                }
            }
            double intervalMidpoint = (interval.getStart() + interval.getEnd()) / 2.0;
            series.add(intervalMidpoint, count);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        return dataset;
    }

    private static JFreeChart createChart(IntervalXYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYBarChart(
                "Histogram Based on Intervals",
                "Intervals",
                false,
                "Frequency",
                dataset
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 162, 232));

        chart.setBackgroundPaint(Color.LIGHT_GRAY);
        chart.getTitle().setPaint(Color.DARK_GRAY);

        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        return chart;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HistogramIntervals demo = new HistogramIntervals("Histogram Based on Intervals");
            demo.setSize(800, 600);
            demo.setLocationRelativeTo(null);
            demo.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            demo.setVisible(true);
        });
    }

    static class Interval {
        private final int start;
        private final int end;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public boolean contains(int value) {
            return value >= start && value < end;
        }
    }
}