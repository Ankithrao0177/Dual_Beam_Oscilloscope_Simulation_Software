package db.oscilloscope.app;

import java.util.Random;

public class WaveformGenerator {
    private final Random random = new Random();

    public double[] generateWaveform(String type, double frequency, double timebase, int samples, double time, boolean acCoupling, double amplitude) {
        double[] data = new double[samples];
        double period = 1.0 / frequency;
        double timeStep = timebase / samples;
        double noiseLevel = 0.05; // Small noise for realism
        double modulation = 0.1 * Math.sin(time * 0.1); // Slow amplitude modulation

        for (int i = 0; i < samples; i++) {
            double t = i * timeStep;
            double phase = 2 * Math.PI * frequency * t;

            switch (type) {
                case "Sine":
                    data[i] = Math.sin(phase) * (1 + modulation);
                    break;
                case "Square":
                    data[i] = Math.sin(phase) >= 0 ? 1.0 : -1.0;
                    break;
                case "Triangle":
                    data[i] = 2 * Math.abs((t % period) / period - 0.5) - 1;
                    break;
                case "Sawtooth":
                    data[i] = 2 * ((t % period) / period) - 1;
                    break;
                default:
                    data[i] = 0;
            }

            // Apply amplitude
            data[i] *= amplitude;

            // Add noise
            data[i] += (random.nextGaussian() * noiseLevel);

            // Apply AC coupling (remove DC component)
            if (acCoupling) {
                data[i] -= 0.5 * Math.sin(phase); // Simulate high-pass filter effect
            }
        }
        return data;
    }
}