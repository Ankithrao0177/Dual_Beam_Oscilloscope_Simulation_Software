package db.oscilloscope.app;

public class WindowFunction {
    public enum Type {
        RECTANGULAR, HAMMING, BLACKMAN, KAISER, GAUSSIAN
    }

    public static double[] generateWindow(Type type, int size) {
        double[] window = new double[size];
        switch (type) {
            case RECTANGULAR:
                for (int i = 0; i < size; i++) {
                    window[i] = 1.0;
                }
                break;
            case HAMMING:
                for (int i = 0; i < size; i++) {
                    window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (size - 1));
                }
                break;
            case BLACKMAN:
                for (int i = 0; i < size; i++) {
                    window[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1)) +
                            0.08 * Math.cos(4 * Math.PI * i / (size - 1));
                }
                break;
            case KAISER:
                double beta = 7.0; // Adjustable, 7.0 balances mainlobe width and sidelobe attenuation
                double bessel = modifiedBessel0(beta);
                for (int i = 0; i < size; i++) {
                    double r = 2.0 * i / (size - 1) - 1.0;
                    window[i] = modifiedBessel0(beta * Math.sqrt(1.0 - r * r)) / bessel;
                }
                break;
            case GAUSSIAN:
                double sigma = 0.4; // Controls window width, 0.4 is moderate tapering
                for (int i = 0; i < size; i++) {
                    double t = (i - (size - 1) / 2.0) / (sigma * (size - 1) / 2.0);
                    window[i] = Math.exp(-0.5 * t * t);
                }
                break;
        }
        // Normalize to unity gain
        double sum = 0.0;
        for (double v : window) {
            sum += v;
        }
        if (sum > 0) {
            for (int i = 0; i < size; i++) {
                window[i] /= sum / size;
            }
        }
        return window;
    }

    // Approximate modified Bessel function of the first kind, order 0
    private static double modifiedBessel0(double x) {
        double sum = 0.0;
        double term = 1.0;
        for (int k = 0; k < 20; k++) { // Sufficient terms for convergence
            sum += term;
            term *= (x * x) / (4 * k * k + 4 * k);
        }
        return sum;
    }
}