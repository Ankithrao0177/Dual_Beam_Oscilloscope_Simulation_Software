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
        switch (protocol) {
            case "I2C": return decodeI2C(ch1Data, ch2Data, timebase, samples);
            case "SPI": return decodeSPI(ch1Data, ch2Data, timebase, samples);
            case "UART": return decodeUART(ch1Data, ch2Data, timebase, samples);
            case "CAN": return decodeCAN(ch1Data, ch2Data, timebase, samples);
            default: return Collections.emptyList();
        }
    }

    private List<DecodedMessage> decodeI2C(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        boolean sda = ch1Data[0] > threshold;
        boolean scl = ch2Data[0] > threshold;
        int data = 0;
        int bitCount = 0;
        double startTime = 0;
        boolean reading = false;

        for (int i = 1; i < samples; i++) {
            boolean newSda = ch1Data[i] > threshold;
            boolean newScl = ch2Data[i] > threshold;
            if (!reading && scl && !newScl && sda && !newSda) {
                reading = true;
                startTime = i * timebase;
                data = 0;
                bitCount = 0;
            }
            if (reading && !scl && newScl) {
                if (bitCount < 8) {
                    data = (data << 1) | (newSda ? 1 : 0);
                    bitCount++;
                } else if (bitCount == 8) {
                    bitCount++;
                } else if (!sda && newSda) {
                    messages.add(new DecodedMessage(startTime, i * timebase, "I2C", String.format("Data: 0x%02X", data)));
                    reading = false;
                }
            }
            sda = newSda;
            scl = newScl;
        }
        return messages;
    }

    private List<DecodedMessage> decodeSPI(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        boolean sclk = ch2Data[0] > threshold;
        boolean mosi = ch1Data[0] > threshold;
        int data = 0;
        int bitCount = 0;
        double startTime = 0;

        for (int i = 1; i < samples; i++) {
            boolean newSclk = ch2Data[i] > threshold;
            boolean newMosi = ch1Data[i] > threshold;
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
        return messages;
    }

    private List<DecodedMessage> decodeUART(double[] ch1Data, double[] ch2Data, double timebase, int samples) {
        List<DecodedMessage> messages = new ArrayList<>();
        double threshold = 0.5;
        double sampleRate = samples / timebase;
        int samplesPerBit = (int) (sampleRate / 9600);
        if (samplesPerBit == 0) return messages;

        for (int i = 0; i < samples - samplesPerBit * 10; i++) {
            if (ch1Data[i] <= threshold && (i == 0 || ch1Data[i - 1] > threshold)) {
                int data = 0;
                boolean valid = true;
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
                int stopIndex = i + (int) (9 * samplesPerBit + samplesPerBit / 2);
                if (valid && stopIndex < samples && ch1Data[stopIndex] > threshold) {
                    messages.add(new DecodedMessage(i * timebase, stopIndex * timebase, "UART", String.format("Data: 0x%02X (%c)", data, data >= 32 && data <= 126 ? (char) data : '.')));
                    i += samplesPerBit * 10;
                }
            }
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

        // Detect bit time by measuring transitions (assume 500 kbps nominal)
        double bitTime = detectBitTime(ch1Data, threshold, sampleRate);
        if (bitTime <= 0) {
            messages.add(new DecodedMessage(0, 0, "CAN", "Error: No valid CAN signal detected"));
            return messages;
        }

        // Calculate samples per bit
        double samplesPerBit = sampleRate * bitTime; // Fixed: Avoid division (previously 1 / bitTime)
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