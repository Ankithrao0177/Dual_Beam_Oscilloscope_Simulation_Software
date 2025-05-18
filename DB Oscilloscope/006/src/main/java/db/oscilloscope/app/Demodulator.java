package db.oscilloscope.app;

public class Demodulator {
    public static class DemodulationResult {
        public double[] basebandSignal;
        public double carrierFrequency;
        public double modulationParam; // Modulation index (AM, PM) or deviation (FM)
        public String paramName; // "Modulation Index" or "Frequency Deviation (Hz)"

        public DemodulationResult(double[] basebandSignal, double carrierFrequency, double modulationParam, String paramName) {
            this.basebandSignal = basebandSignal;
            this.carrierFrequency = carrierFrequency;
            this.modulationParam = modulationParam;
            this.paramName = paramName;
        }
    }

    public DemodulationResult demodulate(double[] signal, double timebase, int samples, String type) {
        if (signal == null || signal.length != samples || timebase <= 0) {
            return new DemodulationResult(new double[samples], 0, 0, "Error");
        }

        double sampleRate = samples / timebase;
        double[] baseband = new double[samples];
        double carrierFreq = estimateCarrierFrequency(signal, sampleRate, samples);
        double modulationParam = 0;
        String paramName = "";

        switch (type) {
            case "AM":
                baseband = demodulateAM(signal, samples);
                modulationParam = estimateAMModulationIndex(signal, baseband);
                paramName = "Modulation Index";
                break;
            case "FM":
                baseband = demodulateFM(signal, sampleRate, samples);
                modulationParam = estimateFMDeviation(signal, baseband, sampleRate);
                paramName = "Frequency Deviation (Hz)";
                break;
            case "PM":
                baseband = demodulatePM(signal, sampleRate, samples);
                modulationParam = estimatePMModulationIndex(signal, baseband);
                paramName = "Modulation Index";
                break;
            default:
                return new DemodulationResult(baseband, carrierFreq, 0, "None");
        }

        return new DemodulationResult(baseband, carrierFreq, modulationParam, paramName);
    }

    private double[] demodulateAM(double[] signal, int samples) {
        // Envelope detection: rectify and low-pass filter
        double[] rectified = new double[samples];
        for (int i = 0; i < samples; i++) {
            rectified[i] = Math.abs(signal[i]);
        }

        // Simple moving average as low-pass filter (window size ~10 samples)
        double[] baseband = new double[samples];
        int window = 10;
        for (int i = 0; i < samples; i++) {
            double sum = 0;
            int count = 0;
            for (int j = Math.max(0, i - window / 2); j < Math.min(samples, i + window / 2); j++) {
                sum += rectified[j];
                count++;
            }
            baseband[i] = sum / count;
        }

        // Remove DC offset
        double mean = 0;
        for (double v : baseband) mean += v;
        mean /= samples;
        for (int i = 0; i < samples; i++) {
            baseband[i] -= mean;
        }

        return baseband;
    }

    private double[] demodulateFM(double[] signal, double sampleRate, int samples) {
        // FM: Compute instantaneous phase, differentiate to get frequency
        double[] phase = new double[samples];
        for (int i = 0; i < samples; i++) {
            phase[i] = Math.atan2(signal[i], i > 0 ? signal[i - 1] : 0);
        }

        // Differentiate phase to get instantaneous frequency
        double[] baseband = new double[samples];
        for (int i = 1; i < samples; i++) {
            double deltaPhase = phase[i] - phase[i - 1];
            if (deltaPhase > Math.PI) deltaPhase -= 2 * Math.PI;
            if (deltaPhase < -Math.PI) deltaPhase += 2 * Math.PI;
            baseband[i] = deltaPhase * sampleRate / (2 * Math.PI);
        }
        baseband[0] = baseband[1];

        // Remove DC offset (carrier frequency)
        double mean = 0;
        for (double v : baseband) mean += v;
        mean /= samples;
        for (int i = 0; i < samples; i++) {
            baseband[i] -= mean;
        }

        return baseband;
    }

    private double[] demodulatePM(double[] signal, double sampleRate, int samples) {
        // PM: Extract instantaneous phase relative to carrier
        double[] baseband = new double[samples];
        for (int i = 0; i < samples; i++) {
            baseband[i] = Math.atan2(signal[i], i > 0 ? signal[i - 1] : 0);
        }

        // Unwrap phase
        for (int i = 1; i < samples; i++) {
            double delta = baseband[i] - baseband[i - 1];
            if (delta > Math.PI) baseband[i] -= 2 * Math.PI;
            if (delta < -Math.PI) baseband[i] += 2 * Math.PI;
        }

        // Remove linear phase (carrier component)
        double mean = 0;
        for (double v : baseband) mean += v;
        mean /= samples;
        for (int i = 0; i < samples; i++) {
            baseband[i] -= mean;
        }

        return baseband;
    }

    private double estimateCarrierFrequency(double[] signal, double sampleRate, int samples) {
        // Use FFT to find dominant frequency
        int fftSize = Math.min(samples, 1024);
        double[] fftData = new double[fftSize];
        for (int i = 0; i < fftSize; i++) {
            fftData[i] = i < samples ? signal[i] : 0;
        }

        FFT.Spectrum spectrum = FFT.computeSpectrum(fftData, "Hamming", fftSize);
        double maxMag = 0;
        int maxIndex = 0;
        for (int i = 0; i < spectrum.magnitude.length / 2; i++) {
            if (spectrum.magnitude[i] > maxMag) {
                maxMag = spectrum.magnitude[i];
                maxIndex = i;
            }
        }

        return maxIndex * sampleRate / fftSize;
    }

    private double estimateAMModulationIndex(double[] signal, double[] baseband) {
        // Modulation index = (Emax - Emin) / (Emax + Emin), where E is envelope
        double maxBaseband = 0, minBaseband = 0;
        for (double v : baseband) {
            maxBaseband = Math.max(maxBaseband, v);
            minBaseband = Math.min(minBaseband, v);
        }
        double maxSignal = 0, minSignal = 0;
        for (double v : signal) {
            maxSignal = Math.max(maxSignal, v);
            minSignal = Math.min(minSignal, v);
        }
        double carrierAmp = (maxSignal + minSignal) / 2;
        if (carrierAmp == 0) return 0;
        return (maxBaseband - minBaseband) / (2 * carrierAmp);
    }

    private double estimateFMDeviation(double[] signal, double[] baseband, double sampleRate) {
        // Frequency deviation is the peak of the baseband (instantaneous frequency)
        double maxDeviation = 0;
        for (double v : baseband) {
            maxDeviation = Math.max(maxDeviation, Math.abs(v));
        }
        return maxDeviation;
    }

    private double estimatePMModulationIndex(double[] signal, double[] baseband) {
        // Modulation index is the peak phase deviation in radians
        double maxPhase = 0;
        for (double v : baseband) {
            maxPhase = Math.max(maxPhase, Math.abs(v));
        }
        return maxPhase;
    }
}