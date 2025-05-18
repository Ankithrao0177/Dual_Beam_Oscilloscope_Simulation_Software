package db.oscilloscope.app;

import java.util.ArrayList;
import java.util.List;

public class ProtocolDecoder {
    public static class DecodedMessage {
        public String protocol;
        public String message;
        public double startTime;
        public double endTime;

        public DecodedMessage(String protocol, String message, double startTime, double endTime) {
            this.protocol = protocol;
            this.message = message;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public List<DecodedMessage> decode(double[] data1, double[] data2, double timebase, int samples, String protocol) {
        List<DecodedMessage> messages = new ArrayList<>();
        double timeStep = timebase / samples;
        double threshold = 0.5; // Threshold for digital high/low

        if ("I2C".equals(protocol)) {
            messages.addAll(decodeI2C(data1, data2, timeStep));
        } else if ("SPI".equals(protocol)) {
            messages.addAll(decodeSPI(data1, data2, timeStep));
        } else if ("UART".equals(protocol)) {
            messages.addAll(decodeUART(data1, timeStep));
        } else if ("CAN".equals(protocol)) {
            messages.addAll(decodeCAN(data1, timeStep));
        }

        return messages;
    }

    private List<DecodedMessage> decodeI2C(double[] sda, double[] scl, double timeStep) {
        List<DecodedMessage> messages = new ArrayList<>();
        boolean lastSda = sda[0] > 0.5;
        boolean lastScl = scl[0] > 0.5;
        int startIndex = -1;
        int bitCount = 0;
        List<Integer> bits = new ArrayList<>();
        StringBuilder message = new StringBuilder();

        for (int i = 1; i < sda.length; i++) {
            boolean sdaHigh = sda[i] > 0.5;
            boolean sclHigh = scl[i] > 0.5;

            // Detect start condition (SDA falling while SCL high)
            if (lastSda && !sdaHigh && sclHigh) {
                startIndex = i;
                bitCount = 0;
                bits.clear();
                message.setLength(0);
                message.append("Start");
                messages.add(new DecodedMessage("I2C", message.toString(), startIndex * timeStep, i * timeStep));
                message.setLength(0);
            }
            // Detect data bits (SCL rising edge)
            else if (!lastScl && sclHigh && startIndex >= 0) {
                bits.add(sdaHigh ? 1 : 0);
                bitCount++;
                if (bitCount == 8) {
                    int byteValue = 0;
                    for (int bit : bits) {
                        byteValue = (byteValue << 1) | bit;
                    }
                    message.append(String.format("Data: 0x%02X", byteValue));
                    messages.add(new DecodedMessage("I2C", message.toString(), (i - 7) * timeStep, i * timeStep));
                    message.setLength(0);
                    bitCount = 0;
                    bits.clear();
                }
            }
            // Detect stop condition (SDA rising while SCL high)
            else if (!lastSda && sdaHigh && sclHigh && startIndex >= 0) {
                message.append("Stop");
                messages.add(new DecodedMessage("I2C", message.toString(), i * timeStep, i * timeStep));
                startIndex = -1;
            }

            lastSda = sdaHigh;
            lastScl = sclHigh;
        }
        return messages;
    }

    private List<DecodedMessage> decodeSPI(double[] mosi, double[] sclk, double timeStep) {
        List<DecodedMessage> messages = new ArrayList<>();
        boolean lastSclk = sclk[0] > 0.5;
        int bitCount = 0;
        List<Integer> bits = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        int startIndex = -1;

        for (int i = 1; i < mosi.length; i++) {
            boolean sclkHigh = sclk[i] > 0.5;
            if (!lastSclk && sclkHigh) { // Rising edge of clock
                if (startIndex == -1) startIndex = i;
                bits.add(mosi[i] > 0.5 ? 1 : 0);
                bitCount++;
                if (bitCount == 8) {
                    int byteValue = 0;
                    for (int bit : bits) {
                        byteValue = (byteValue << 1) | bit;
                    }
                    message.append(String.format("MOSI: 0x%02X", byteValue));
                    messages.add(new DecodedMessage("SPI", message.toString(), (i - 7) * timeStep, i * timeStep));
                    message.setLength(0);
                    bitCount = 0;
                    bits.clear();
                    startIndex = -1;
                }
            }
            lastSclk = sclkHigh;
        }
        return messages;
    }

    private List<DecodedMessage> decodeUART(double[] data, double timeStep) {
        List<DecodedMessage> messages = new ArrayList<>();
        boolean lastBit = data[0] > 0.5;
        int bitCount = 0;
        List<Integer> bits = new ArrayList<>();
        int startIndex = -1;
        double baudRate = 9600; // Assume 9600 baud for simplicity
        int samplesPerBit = (int) (1.0 / baudRate / timeStep);

        for (int i = 1; i < data.length; i++) {
            if (!lastBit && data[i] <= 0.5 && startIndex == -1) { // Start bit
                startIndex = i;
                bitCount = 0;
                bits.clear();
            } else if (startIndex >= 0 && (i - startIndex) % samplesPerBit == 0) {
                bitCount++;
                if (bitCount <= 8) {
                    bits.add(data[i] > 0.5 ? 1 : 0);
                }
                if (bitCount == 9) { // 8 data bits + stop bit
                    int byteValue = 0;
                    for (int bit : bits) {
                        byteValue = (byteValue << 1) | bit;
                    }
                    messages.add(new DecodedMessage("UART", String.format("Data: 0x%02X", byteValue), startIndex * timeStep, i * timeStep));
                    startIndex = -1;
                }
            }
            lastBit = data[i] > 0.5;
        }
        return messages;
    }

    private List<DecodedMessage> decodeCAN(double[] data, double timeStep) {
        List<DecodedMessage> messages = new ArrayList<>();
        boolean lastBit = data[0] > 0.5;
        int bitCount = 0;
        List<Integer> bits = new ArrayList<>();
        int startIndex = -1;
        double bitRate = 500000; // Assume 500 kbps for CAN
        int samplesPerBit = (int) (1.0 / bitRate / timeStep);

        for (int i = 1; i < data.length; i++) {
            if (!lastBit && data[i] <= 0.5 && startIndex == -1) { // Start of frame
                startIndex = i;
                bitCount = 0;
                bits.clear();
            } else if (startIndex >= 0 && (i - startIndex) % samplesPerBit == 0) {
                bitCount++;
                bits.add(data[i] > 0.5 ? 1 : 0);
                if (bitCount == 11) { // Simplified: ID only (11 bits)
                    int id = 0;
                    for (int bit : bits) {
                        id = (id << 1) | bit;
                    }
                    messages.add(new DecodedMessage("CAN", String.format("ID: 0x%03X", id), startIndex * timeStep, i * timeStep));
                    startIndex = -1;
                }
            }
            lastBit = data[i] > 0.5;
        }
        return messages;
    }
}