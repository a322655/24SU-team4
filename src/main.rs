use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;

use plotters::prelude::*;

#[derive(Debug)]
struct Interval {
    start: i32,
    end: i32,
}

impl Interval {
    fn contains(&self, value: i32) -> bool {
        value >= self.start && value < self.end
    }
}

fn read_intervals_and_data(filename: &str) -> io::Result<(Vec<Interval>, Vec<i32>)> {
    let mut intervals = Vec::new();
    let mut data = Vec::new();
    let path = Path::new(filename);
    let file = File::open(&path)?;

    let reader = io::BufReader::new(file);
    let mut reading_data = false;

    for line in reader.lines() {
        let line = line?.trim().to_string();

        if line.is_empty() {
            reading_data = true;
            continue;
        }

        if reading_data {
            match line.parse::<i32>() {
                Ok(data_point) => data.push(data_point),
                Err(e) => return Err(io::Error::new(io::ErrorKind::InvalidData, e)),
            }
        } else {
            let parts: Vec<&str> = line.split(',').collect();
            if parts.len() != 2 {
                return Err(io::Error::new(io::ErrorKind::InvalidData, "Invalid interval format"));
            }

            let start: i32 = parts[0].parse().map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
            let end: i32 = parts[1].parse().map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
            intervals.push(Interval { start, end });
        }
    }

    Ok((intervals, data))
}

fn create_histogram(intervals: &[Interval], data: &[i32], filename: &str) -> Result<(), Box<dyn std::error::Error>> {
    let root = BitMapBackend::new(filename, (800, 600)).into_drawing_area();
    root.fill(&WHITE)?;

    let x_range = intervals.first().ok_or("No intervals provided")?.start..intervals.last().ok_or("No intervals provided")?.end;
    let y_max = data.len() as i32;

    let mut chart = ChartBuilder::on(&root)
        .caption("Histogram Based on Intervals", ("sans-serif", 50))
        .margin(20)
        .x_label_area_size(30)
        .y_label_area_size(30)
        .build_cartesian_2d(x_range.clone(), 0..y_max)?;

    chart.configure_mesh().draw()?;

    let mut frequencies = vec![0; intervals.len()];

    for &value in data {
        for (i, interval) in intervals.iter().enumerate() {
            if interval.contains(value) {
                frequencies[i] += 1;
            }
        }
    }

    for (interval, &freq) in intervals.iter().zip(frequencies.iter()) {
        chart.draw_series(vec![Rectangle::new(
            [(interval.start, 0), (interval.end, freq)],
            BLUE.filled(),
        )])?;
    }

    root.present()?;
    Ok(())
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let filename = "data/data.txt";
    let (intervals, data) = read_intervals_and_data(filename)?;

    let output_filename = "output/histogram.png";
    create_histogram(&intervals, &data, output_filename)?;

    Ok(())
}