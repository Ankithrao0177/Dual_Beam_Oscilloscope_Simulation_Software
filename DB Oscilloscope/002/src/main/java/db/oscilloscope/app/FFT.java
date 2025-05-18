package db.oscilloscope.app;

public class FFT {
    public static double[] computeMagnitudeSpectrum(double[] input, String windowType, int n) {
        // Apply window function
        double[] windowed = applyWindow(input, windowType, n);

        // Perform FFT
        Complex[] complexInput = new Complex[n];
        for (int i = 0; i < n; i++) {
            complexInput[i] = new Complex(windowed[i], 0);
        }
        Complex[] fftResult = fft(complexInput);

        // Compute magnitude spectrum (first half, up to Nyquist frequency)
        double[] magnitude = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            magnitude[i] = Math.sqrt(fftResult[i].re * fftResult[i].re + fftResult[i].im * fftResult[i].im);
        }
        return magnitude;
    }

    private static double[] applyWindow(double[] input, String windowType, int n) {
        double[] windowed = new double[n];
        for (int i = 0; i < n; i++) {
            double windowValue = 1.0; // Rectangular window by default
            if ("Hamming".equals(windowType)) {
                windowValue = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
            } else if ("Blackman".equals(windowType)) {
                windowValue = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)) + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
            }
            windowed[i] = input[i] * windowValue;
        }
        return windowed;
    }

    private static Complex[] fft(Complex[] x) {
        int n = x.length;
        if (n == 1) return new Complex[] { x[0] };

        // Split even and odd
        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
            odd[k] = x[2 * k + 1];
        }

        // Recursive FFT
        Complex[] evenFft = fft(even);
        Complex[] oddFft = fft(odd);

        // Combine
        Complex[] result = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            Complex wk = new Complex(Math.cos(angle), Math.sin(angle));
            result[k] = evenFft[k].plus(wk.times(oddFft[k]));
            result[k + n / 2] = evenFft[k].minus(wk.times(oddFft[k]));
        }
        return result;
    }

    private static class Complex {
        private final double re;
        private final double im;

        public Complex(double real, double imag) {
            this.re = real;
            this.im = imag;
        }

        public Complex plus(Complex b) {
            return new Complex(this.re + b.re, this.im + b.im);
        }

        public Complex minus(Complex b) {
            return new Complex(this.re - b.re, this.im - b.im);
        }

        public Complex times(Complex b) {
            return new Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re);
        }
    }
}