package db.oscilloscope.app;

public class FFT {
    public static class Spectrum {
        public double[] magnitude;
        public double[] phase;

        public Spectrum(double[] magnitude, double[] phase) {
            this.magnitude = magnitude;
            this.phase = phase;
        }
    }

    public static Spectrum computeSpectrum(double[] data, String windowType, int fftSize) {
        // Map string window type to WindowFunction.Type
        WindowFunction.Type type;
        switch (windowType.toLowerCase()) {
            case "rectangular":
                type = WindowFunction.Type.RECTANGULAR;
                break;
            case "hamming":
                type = WindowFunction.Type.HAMMING;
                break;
            case "blackman":
                type = WindowFunction.Type.BLACKMAN;
                break;
            case "kaiser":
                type = WindowFunction.Type.KAISER;
                break;
            case "gaussian":
                type = WindowFunction.Type.GAUSSIAN;
                break;
            default:
                type = WindowFunction.Type.HAMMING;
        }

        // Apply window function
        double[] window = WindowFunction.generateWindow(type, fftSize);
        double[] windowedData = new double[fftSize];
        for (int i = 0; i < fftSize; i++) {
            windowedData[i] = data[i] * window[i];
        }

        // Perform FFT
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];
        System.arraycopy(windowedData, 0, real, 0, fftSize);

        fft(real, imag, fftSize);

        // Compute magnitude and phase
        double[] magnitude = new double[fftSize / 2];
        double[] phase = new double[fftSize / 2];
        for (int i = 0; i < fftSize / 2; i++) {
            magnitude[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]) / fftSize;
            phase[i] = Math.atan2(imag[i], real[i]);
        }

        return new Spectrum(magnitude, phase);
    }

    private static void fft(double[] real, double[] imag, int n) {
        int bits = (int) (Math.log(n) / Math.log(2));
        if (Math.pow(2, bits) != n) {
            throw new IllegalArgumentException("FFT size must be a power of 2");
        }

        // Bit-reversal permutation
        for (int i = 0; i < n; i++) {
            int j = 0;
            for (int k = 0; k < bits; k++) {
                j = (j << 1) | ((i >> k) & 1);
            }
            if (j > i) {
                double temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
        }

        // Cooley-Tukey FFT
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2 * Math.PI / len;
            double wtemp = Math.sin(0.5 * angle);
            double wpr = -2 * wtemp * wtemp;
            double wpi = Math.sin(angle);
            double wr = 1.0;
            double wi = 0.0;

            for (int m = 0; m < len / 2; m++) {
                for (int i = m; i < n; i += len) {
                    int j = i + len / 2;
                    double vr = real[j] * wr - imag[j] * wi;
                    double vi = real[j] * wi + imag[j] * wr;
                    real[j] = real[i] - vr;
                    imag[j] = imag[i] - vi;
                    real[i] += vr;
                    imag[i] += vi;
                }
                double wtemp_wr = wr;
                wr += wr * wpr - wi * wpi;
                wi += wtemp_wr * wpi + wi * wpr;
            }
        }
    }
}