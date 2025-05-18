package db.oscilloscope.app;

import java.util.ArrayList;
import java.util.List;

public class PeakDetector {
    public static class Peak {
        public double frequency;
        public double magnitude;
        public boolean isHarmonic;
        public int harmonicOrder;

        public Peak(double frequency, double magnitude, boolean isHarmonic, int harmonicOrder) {
            this.frequency = frequency;
            this.magnitude = magnitude;
            this.isHarmonic = isHarmonic;
            this.harmonicOrder = harmonicOrder;
        }
    }

    public static class DetectionResult {
        public List<Peak> peaks;
        public double snr;

        public DetectionResult(List<Peak> peaks, double snr) {
            this.peaks = peaks;
            this.snr = snr;
        }
    }

    public static DetectionResult detectPeaks(double[] magnitude, double sampleRate, int samples, double maxFreq, boolean isDbScale) {
        List<Peak> peaks = new ArrayList<>();
        double threshold = calculateThreshold(magnitude, isDbScale);
        double fundamentalFreq = 0;
        double fundamentalMag = 0;
        int fundamentalIndex = -1;

        // Detect peaks (local maxima above threshold)
        for (int i = 1; i < magnitude.length - 1 && i * sampleRate / samples <= maxFreq; i++) {
            double mag = isDbScale ? magnitude[i] : 20 * Math.log10(Math.max(magnitude[i], 1e-10));
            if (mag > threshold && mag > (isDbScale ? magnitude[i - 1] : 20 * Math.log10(Math.max(magnitude[i - 1], 1e-10))) &&
                    mag > (isDbScale ? magnitude[i + 1] : 20 * Math.log10(Math.max(magnitude[i + 1], 1e-10)))) {
                double freq = i * sampleRate / samples;
                if (mag > fundamentalMag) {
                    fundamentalMag = mag;
                    fundamentalFreq = freq;
                    fundamentalIndex = i;
                }
                peaks.add(new Peak(freq, magnitude[i], false, 0));
            }
        }

        // Identify harmonics
        if (fundamentalIndex != -1) {
            for (int i = 0; i < peaks.size(); i++) {
                Peak peak = peaks.get(i);
                if (Math.abs(peak.frequency / fundamentalFreq - Math.round(peak.frequency / fundamentalFreq)) < 0.1) {
                    int order = (int) Math.round(peak.frequency / fundamentalFreq);
                    if (order >= 1) {
                        peak.isHarmonic = true;
                        peak.harmonicOrder = order;
                    }
                }
            }
        }

        // Calculate SNR
        double signalPower = 0;
        double noisePower = 0;
        int noiseCount = 0;
        for (int i = 0; i < magnitude.length && i * sampleRate / samples <= maxFreq; i++) {
            boolean isPeak = false;
            double freq = i * sampleRate / samples;
            for (Peak peak : peaks) {
                if (Math.abs(freq - peak.frequency) < sampleRate / samples) {
                    isPeak = true;
                    break;
                }
            }
            double mag = magnitude[i];
            double power = mag * mag;
            if (isPeak) {
                signalPower += power;
            } else {
                noisePower += power;
                noiseCount++;
            }
        }
        double snr = noiseCount > 0 ? 10 * Math.log10(signalPower / (noisePower / noiseCount)) : Double.POSITIVE_INFINITY;

        return new DetectionResult(peaks, snr);
    }

    private static double calculateThreshold(double[] magnitude, boolean isDbScale) {
        double sum = 0;
        for (double mag : magnitude) {
            sum += isDbScale ? mag : 20 * Math.log10(Math.max(mag, 1e-10));
        }
        double mean = sum / magnitude.length;
        double variance = 0;
        for (double mag : magnitude) {
            double value = isDbScale ? mag : 20 * Math.log10(Math.max(mag, 1e-10));
            variance += (value - mean) * (value - mean);
        }
        variance /= magnitude.length;
        double stdDev = Math.sqrt(variance);
        return mean + 2 * stdDev; // Threshold = mean + 2 standard deviations
    }
}