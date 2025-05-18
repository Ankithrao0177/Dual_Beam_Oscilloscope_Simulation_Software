package db.oscilloscope.app;

public class MathChannel {
    public enum Operation {
        NONE, ADD, SUBTRACT, MULTIPLY, DIFFERENTIATE_CH1, DIFFERENTIATE_CH2, INTEGRATE_CH1, INTEGRATE_CH2
    }

    public static double[] computeMathChannel(double[] ch1Data, double[] ch2Data, Operation operation, double timebase, int samples) {
        double[] result = new double[samples];
        double dt = timebase / samples; // Time step per sample

        switch (operation) {
            case NONE:
                return result; // Zero-filled array
            case ADD:
                for (int i = 0; i < samples; i++) {
                    result[i] = ch1Data[i] + ch2Data[i];
                }
                break;
            case SUBTRACT:
                for (int i = 0; i < samples; i++) {
                    result[i] = ch1Data[i] - ch2Data[i];
                }
                break;
            case MULTIPLY:
                double maxProduct = 4.0; // Max amplitude (2.0 * 2.0)
                for (int i = 0; i < samples; i++) {
                    result[i] = (ch1Data[i] * ch2Data[i]) / maxProduct; // Normalize to prevent overflow
                }
                break;
            case DIFFERENTIATE_CH1:
                for (int i = 0; i < samples - 1; i++) {
                    result[i] = (ch1Data[i + 1] - ch1Data[i]) / dt;
                }
                result[samples - 1] = result[samples - 2]; // Copy last value
                break;
            case DIFFERENTIATE_CH2:
                for (int i = 0; i < samples - 1; i++) {
                    result[i] = (ch2Data[i + 1] - ch2Data[i]) / dt;
                }
                result[samples - 1] = result[samples - 2];
                break;
            case INTEGRATE_CH1:
                result[0] = 0.0; // Reset integral
                for (int i = 1; i < samples; i++) {
                    result[i] = result[i - 1] + 0.5 * (ch1Data[i] + ch1Data[i - 1]) * dt;
                }
                break;
            case INTEGRATE_CH2:
                result[0] = 0.0;
                for (int i = 1; i < samples; i++) {
                    result[i] = result[i - 1] + 0.5 * (ch2Data[i] + ch2Data[i - 1]) * dt;
                }
                break;
        }
        return result;
    }
}