package db.oscilloscope.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtocolDecoder {
    public static class DecodedMessage {
        public double startTime;
        public double endTime;
        public String protocol;
        public String message;

        public DecodedMessage(double startTime, double endTime, String protocol, String message) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.protocol = protocol;
            this.message = message;
        }
    }

    public List<DecodedMessage> decode(double[] ch1Data, double[] ch2Data, double timebase, int samples, String protocol) {
        // Validate inputs
        if (ch1Data == null || ch2Data == null || ch1Data.length != samples || ch2Data.length != samples) {
            return Collections.singletonList(new DecodedMessage(0, 0, protocol, "Error: Invalid signal data"));
        }
        if (timebase <= 0 || Double.isInfinite(timebase) || Double.isNaN(timebase)) {
            return Collections.singletonList(new DecodedMessage(0, 0, protocol, "Error: Invalid timebase"));
        }

        switch (protocol) {
            case "I2C": return decodeI2C(ch1Data, ch2Data, timebase, samples);
            case "SPI": return decodeSPI(ch1Data, ch2Data, timebase, samples);
            case "UART": return decodeUART(ch1Data, ch2Data, timebase, samples);
            case "CAN": return decodeCAN(ch1Data, ch2Data, timebase, samples);
            default: return Collections.emptyList();
        }
    }

    private boolean hasTransitions(double[] data, double threshold, int minTransitions) {
        int transitions = 0;
        boolean lastBit = data[0] > threshold;
        for (int i = 1; i < data.length; i++) {
            boolean currentBit = data[i] > threshold;
            if (lastBit != currentBit) transitions++;
            lastBit = currentBit;
            if (transitions >= minTransitions) return true;
        }
        return false;
    }

    private List<DecodedMessage> decodeI2C(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        double sampleRate = samples / timebase;

        // Validate signals
        if (!hasTransitions(ch1Data, threshold, 10) || !hasTransitions(ch2Data, threshold, 10)) {
            messages.add(new DecodedMessage(0, 0, "I2C", "Error: Insufficient signal transitions for I2C"));
            return messages;
        }

        boolean sda = ch1Data[0] > threshold;
        boolean scl = ch2Data[0] > threshold;
        int data = 0;
        int bitCount = 0;
        double startTime = 0;
        boolean reading = false;

        for (int i = 1; i < samples; i++) {
            boolean newSda = ch1Data[i] > threshold;
            boolean newScl = ch2Data[i] > threshold;

            // Detect start condition (SDA falling while SCL high)
            if (!reading && scl && !newScl && sda && !newSda) {
                reading = true;
                startTime = i * timebase;
                data = 0;
                bitCount = 0;
            }

            // Sample data on SCL rising edge
            if (reading && !scl && newScl) {
                if (bitCount < 8) {
                    data = (data << 1) | (newSda ? 1 : 0);
                    bitCount++;
                } else if (bitCount == 8) {
                    // ACK bit
                    bitCount++;
                } else if (!sda && newSda) {
                    // Stop condition (SDA rising while SCL high)
                    messages.add(new DecodedMessage(startTime, i * timebase, "I2C", String.format("Data: 0x%02X", data)));
                    reading = false;
                }
            }

            // Detect stop condition outside data frame
            if (reading && scl && newScl && !sda && newSda) {
                messages.add(new DecodedMessage(startTime, i * timebase, "I2C", String.format("Data: 0x%02X (incomplete)", data)));
                reading = false;
            }

            sda = newSda;
            scl = newScl;
        }

        if (messages.isEmpty()) {
            messages.add(new DecodedMessage(0, 0, "I2C", "Error: No valid I2C frames detected"));
        }
        return messages;
    }

    private List<DecodedMessage> decodeSPI(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        double sampleRate = samples / timebase;

        // Validate signals
        if (!hasTransitions(ch1Data, threshold, 8) || !hasTransitions(ch2Data, threshold, 8)) {
            messages.add(new DecodedMessage(0, 0, "SPI", "Error: Insufficient signal transitions for SPI"));
            return messages;
        }

        boolean sclk = ch2Data[0] > threshold;
        boolean mosi = ch1Data[0] > threshold;
        int data = 0;
        int bitCount = 0;
        double startTime = 0;

        for (int i = 1; i < samples; i++) {
            boolean newSclk = ch2Data[i] > threshold;
            boolean newMosi = ch1Data[i] > threshold;

            // Sample on SCLK rising edge (CPOL=0, CPHA=0)
            if (!sclk && newSclk) {
                if (bitCount == 0) startTime = i * timebase;
                data = (data << 1) | (newMosi ? 1 : 0);
                bitCount++;
                if (bitCount == 8) {
                    messages.add(new DecodedMessage(startTime, i * timebase, "SPI", String.format("Data: 0x%02X", data)));
                    bitCount = 0;
                    data = 0;
                }
            }
            sclk = newSclk;
            mosi = newMosi;
        }

        if (messages.isEmpty()) {
            messages.add(new DecodedMessage(0, 0, "SPI", "Error: No valid SPI data detected"));
        }
        return messages;
    }

    private List<DecodedMessage> decodeUART(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        double sampleRate = samples / timebase;
        int samplesPerBit = (int) (sampleRate / 9600); // Assume 9600 baud

        // Validate signals and baud rate
        if (samplesPerBit < 2) {
            messages.add(new DecodedMessage(0, 0, "UART", "Error: Sample rate too low for 9600 baud"));
            return messages;
        }
        if (!hasTransitions(ch1Data, threshold, 5)) {
            messages.add(new DecodedMessage(0, 0, "UART", "Error: Insufficient signal transitions for UART"));
            return messages;
        }

        for (int i = 0; i < samples - samplesPerBit * 10; i++) {
            // Detect start bit (falling edge)
            if (ch1Data[i] <= threshold && (i == 0 || ch1Data[i - 1] > threshold)) {
                int data = 0;
                boolean valid = true;

                // Sample 8 data bits
                for (int bit = 1; bit <= 8; bit++) {
                    int sampleIndex = i + (int) (bit * samplesPerBit + samplesPerBit / 2);
                    if (sampleIndex >= samples) {
                        valid = false;
                        break;
                    }
                    if (ch1Data[sampleIndex] > threshold) {
                        data |= (1 << (bit - 1));
                    }
                }

                // Check stop bit
                int stopIndex = i + (int) (9 * samplesPerBit + samplesPerBit / 2);
                if (valid && stopIndex < samples && ch1Data[stopIndex] > threshold) {
                    messages.add(new DecodedMessage(i * timebase, stopIndex * timebase, "UART",
                            String.format("Data: 0x%02X (%c)", data, data >= 32 && data <= 126 ? (char) data : '.')));
                    i += samplesPerBit * 10; // Skip to next frame
                } else if (valid) {
                    messages.add(new DecodedMessage(i * timebase, stopIndex * timebase, "UART",
                            String.format("Data: 0x%02X (invalid stop bit)", data)));
                }
            }
        }

        if (messages.isEmpty()) {
            messages.add(new DecodedMessage(0, 0, "UART", "Error: No valid UART frames detected"));
        }
        return messages;
    }

    private List<DecodedMessage> decodeCAN(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        double sampleRate = samples / timebase;

        // Avoid division by zero or invalid sample rate
        if (timebase <= 0 || Double.isInfinite(sampleRate) || Double.isNaN(sampleRate)) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: Invalid timebase or sample rate"));
            return messages;
        }

        // Validate signal
        if (!hasTransitions(ch1Data, threshold, 10)) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: Insufficient signal transitions for CAN"));
            return messages;
        }

        // Detect bit time by measuring transitions (assume 500 kbps nominal)
        double bitTime = detectBitTime(ch1Data, threshold, sampleRate);
        if (bitTime <= 0) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: No valid CAN signal detected"));
            return messages;
        }

        // Calculate samples per bit
        double samplesPerBit = sampleRate * bitTime;
        if (samplesPerBit < 1) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: Sample rate too low for CAN bit rate"));
            return messages;
        }

        // Simple CAN frame decoding
        boolean lastBit = ch1Data[0] > threshold;
        int bitCount = 0;
        int frameData = 0;
        double startTime = 0;
        boolean inFrame = false;

        for (int i = 1; i < samples; i++) {
            boolean currentBit = ch1Data[i] > threshold;
            if (lastBit != currentBit) {
                int samplesSinceLast = i - (int) (i - samplesPerBit);
                if (Math.abs(samplesSinceLast - samplesPerBit) < samplesPerBit * 0.2) {
                    if (!inFrame) {
                        inFrame = true;
                        startTime = i * timebase;
                    }
                    frameData = (frameData << 1) | (currentBit ? 1 : 0);
                    bitCount++;
                    if (bitCount == 11) { // Simplified: Assume 11-bit ID
                        messages.add(new DecodedMessage(startTime, i * timebase, "CAN", String.format("ID: 0x%03X", frameData & 0x7FF)));
                        inFrame = false;
                        bitCount = 0;
                        frameData = 0;
                    }
                }
            }
            lastBit = currentBit;
        }

        if (messages.isEmpty()) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: No valid CAN frames detected"));
        }
        return messages;
    }

    private double detectBitTime(double[] data, double threshold, double sampleRate) {
        List<Integer> transitions = new ArrayList<>();
        boolean lastBit = data[0] > threshold;
        for (int i = 1; i < data.length; i++) {
            boolean currentBit = data[i] > threshold;
            if (lastBit != currentBit) {
                transitions.add(i);
            }
            lastBit = currentBit;
        }
        if (transitions.size() < 2) return 0;
        double totalTime = 0;
        int count = 0;
        for (int i = 1; i < transitions.size(); i++) {
            double timeDiff = (transitions.get(i) - transitions.get(i - 1)) / sampleRate;
            if (timeDiff > 1e-6 && timeDiff < 1e-3) { // Filter unrealistic bit times
                totalTime += timeDiff;
                count++;
            }
        }
        return count > 0 ? totalTime / count : 0;
    }
}