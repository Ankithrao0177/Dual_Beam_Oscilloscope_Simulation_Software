package db.oscilloscope.app;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import java.util.List;

public class OscilloscopeController {
    @FXML private Canvas canvas;
    @FXML private Slider timebaseSlider;
    @FXML private Slider ch1VoltSlider;
    @FXML private Slider ch2VoltSlider;
    @FXML private Slider ch1PosSlider;
    @FXML private Slider ch2PosSlider;
    @FXML private Slider ch1AmplitudeSlider;
    @FXML private Slider ch2AmplitudeSlider;
    @FXML private ChoiceBox<String> ch1WaveType;
    @FXML private ChoiceBox<String> ch2WaveType;
    @FXML private ChoiceBox<String> ch1Coupling;
    @FXML private ChoiceBox<String> ch2Coupling;
    @FXML private ChoiceBox<String> triggerSource;
    @FXML private ChoiceBox<String> triggerSlope;
    @FXML private Slider triggerSlider;
    @FXML private TextField ch1FreqField;
    @FXML private TextField ch2FreqField;
    @FXML private Button runStopButton;
    @FXML private Label timebaseLabel;
    @FXML private Label ch1VoltLabel;
    @FXML private Label ch2VoltLabel;
    @FXML private Label ch1AmplitudeLabel;
    @FXML private Label ch2AmplitudeLabel;
    @FXML private Button spectrumToggleButton;
    @FXML private ChoiceBox<String> fftWindowChoice;
    @FXML private Slider maxFreqSlider;
    @FXML private Label maxFreqLabel;
    @FXML private ChoiceBox<String> protocolChoice;
    @FXML private TextArea protocolOutput;

    private WaveformGenerator generator;
    private ProtocolDecoder decoder;
    private double[] ch1Data;
    private double[] ch2Data;
    private GraphicsContext gc;
    private final int samples = 800;
    private boolean isRunning = true;
    private boolean isSpectrumMode = false;
    private double time = 0;
    private double lastTimebase = -1;
    private double lastCh1Freq = -1;
    private double lastCh2Freq = -1;
    private String lastCh1Type = "";
    private String lastCh2Type = "";
    private boolean lastCh1AC = false;
    private boolean lastCh2AC = false;
    private double lastCh1Amplitude = -1;
    private double lastCh2Amplitude = -1;

    @FXML
    public void initialize() {
        generator = new WaveformGenerator();
        decoder = new ProtocolDecoder();
        gc = canvas.getGraphicsContext2D();
        ch1Data = new double[samples];
        ch2Data = new double[samples];

        // Initialize choice boxes
        ch1WaveType.getItems().addAll("Sine", "Square", "Triangle", "Sawtooth");
        ch2WaveType.getItems().addAll("Sine", "Square", "Triangle", "Sawtooth");
        ch1WaveType.setValue("Sine");
        ch2WaveType.setValue("Sine");
        ch1Coupling.getItems().addAll("AC", "DC");
        ch2Coupling.setValue("DC");
        ch2Coupling.getItems().addAll("AC", "DC");
        ch1Coupling.setValue("DC");
        triggerSource.getItems().addAll("CH1", "CH2");
        triggerSource.setValue("CH1");
        triggerSlope.getItems().addAll("Rising", "Falling");
        triggerSlope.setValue("Rising");
        fftWindowChoice.getItems().addAll("Rectangular", "Hamming", "Blackman");
        fftWindowChoice.setValue("Hamming");
        protocolChoice.getItems().addAll("None", "I2C", "SPI", "UART", "CAN");
        protocolChoice.setValue("None");

        // Add tooltips
        timebaseSlider.setTooltip(new Tooltip("Adjusts the horizontal time scale (seconds/div)"));
        ch1VoltSlider.setTooltip(new Tooltip("Adjusts CH1 vertical scale (volts/div)"));
        ch2VoltSlider.setTooltip(new Tooltip("Adjusts CH2 vertical scale (volts/div)"));
        ch1PosSlider.setTooltip(new Tooltip("Adjusts CH1 vertical position"));
        ch2PosSlider.setTooltip(new Tooltip("Adjusts CH2 vertical position"));
        ch1AmplitudeSlider.setTooltip(new Tooltip("Adjusts CH1 waveform amplitude"));
        ch2AmplitudeSlider.setTooltip(new Tooltip("Adjusts CH2 waveform amplitude"));
        ch1FreqField.setTooltip(new Tooltip("Enter CH1 frequency in Hz"));
        ch2FreqField.setTooltip(new Tooltip("Enter CH2 frequency in Hz"));
        runStopButton.setTooltip(new Tooltip("Start or stop the waveform display"));
        spectrumToggleButton.setTooltip(new Tooltip("Toggle between time and frequency domain views"));
        fftWindowChoice.setTooltip(new Tooltip("Select FFT windowing function"));
        maxFreqSlider.setTooltip(new Tooltip("Adjust maximum frequency displayed (Hz)"));
        protocolChoice.setTooltip(new Tooltip("Select protocol to decode (CH1/CH2 signals)"));
        protocolOutput.setTooltip(new Tooltip("Displays decoded protocol messages"));

        // Update labels with slider values
        updateSliderLabels();
        timebaseSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch1VoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch2VoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch1AmplitudeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch2AmplitudeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        maxFreqSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());

        // Validate frequency input
        ch1FreqField.textProperty().addListener((obs, old, val) -> validateFrequencyField(ch1FreqField, val));
        ch2FreqField.textProperty().addListener((obs, old, val) -> validateFrequencyField(ch2FreqField, val));

        // Run/Stop button action
        runStopButton.setOnAction(e -> {
            isRunning = !isRunning;
            runStopButton.setText(isRunning ? "Stop" : "Run");
        });

        // Spectrum toggle button action
        spectrumToggleButton.setOnAction(e -> {
            isSpectrumMode = !isSpectrumMode;
            spectrumToggleButton.setText(isSpectrumMode ? "Time Domain" : "Spectrum");
        });

        // Protocol choice listener to clear output
        protocolChoice.valueProperty().addListener((obs, old, val) -> protocolOutput.clear());

        // Start animation loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isRunning) {
                    updateWaveforms();
                    drawWaveforms();
                    time += 0.016; // Approx 60 FPS
                }
            }
        }.start();
    }

    private void updateSliderLabels() {
        timebaseLabel.setText(String.format("Timebase: %.3f s/div", timebaseSlider.getValue()));
        ch1VoltLabel.setText(String.format("CH1: %.2f V/div", ch1VoltSlider.getValue()));
        ch2VoltLabel.setText(String.format("CH2: %.2f V/div", ch2VoltSlider.getValue()));
        ch1AmplitudeLabel.setText(String.format("CH1 Amp: %.2f", ch1AmplitudeSlider.getValue()));
        ch2AmplitudeLabel.setText(String.format("CH2 Amp: %.2f", ch2AmplitudeSlider.getValue()));
        maxFreqLabel.setText(String.format("Max Freq: %.0f Hz", maxFreqSlider.getValue()));
    }

    private void validateFrequencyField(TextField field, String value) {
        try {
            double freq = Double.parseDouble(value);
            if (freq <= 0) throw new NumberFormatException();
            field.setStyle("-fx-border-color: #808080 #FFFFFF #FFFFFF #808080;");
        } catch (NumberFormatException e) {
            field.setStyle("-fx-border-color: red;");
        }
    }

    private void updateWaveforms() {
        double timebase = timebaseSlider.getValue();
        double ch1Volt = ch1VoltSlider.getValue();
        double ch2Volt = ch2VoltSlider.getValue();
        double ch1Amplitude = ch1AmplitudeSlider.getValue();
        double ch2Amplitude = ch2AmplitudeSlider.getValue();
        double triggerLevel = triggerSlider.getValue();
        double ch1Freq = parseFrequency(ch1FreqField.getText(), 1.0);
        double ch2Freq = parseFrequency(ch2FreqField.getText(), 1.0);
        String ch1Type = ch1WaveType.getValue();
        String ch2Type = ch2WaveType.getValue();
        boolean ch1AC = ch1Coupling.getValue().equals("AC");
        boolean ch2AC = ch2Coupling.getValue().equals("AC");

        // Only regenerate waveforms if parameters have changed
        if (timebase != lastTimebase || ch1Freq != lastCh1Freq || ch2Freq != lastCh2Freq ||
                !ch1Type.equals(lastCh1Type) || !ch2Type.equals(lastCh2Type) ||
                ch1AC != lastCh1AC || ch2AC != lastCh2AC ||
                ch1Amplitude != lastCh1Amplitude || ch2Amplitude != lastCh2Amplitude) {
            ch1Data = generator.generateWaveform(ch1Type, ch1Freq, timebase, samples, time, ch1AC, ch1Amplitude);
            ch2Data = generator.generateWaveform(ch2Type, ch2Freq, timebase, samples, time, ch2AC, ch2Amplitude);
            lastTimebase = timebase;
            lastCh1Freq = ch1Freq;
            lastCh2Freq = ch2Freq;
            lastCh1Type = ch1Type;
            lastCh2Type = ch2Type;
            lastCh1AC = ch1AC;
            lastCh2AC = ch2AC;
            lastCh1Amplitude = ch1Amplitude;
            lastCh2Amplitude = ch2Amplitude;
        }

        // Apply trigger with hysteresis
        boolean useCh1 = triggerSource.getValue().equals("CH1");
        boolean rising = triggerSlope.getValue().equals("Rising");
        int triggerPoint = findTriggerPoint(useCh1 ? ch1Data : ch2Data, triggerLevel, rising);
        shiftData(ch1Data, triggerPoint);
        shiftData(ch2Data, triggerPoint);

        // Decode protocol if selected
        String protocol = protocolChoice.getValue();
        if (!"None".equals(protocol)) {
            List<ProtocolDecoder.DecodedMessage> messages = decoder.decode(ch1Data, ch2Data, timebase, samples, protocol);
            StringBuilder output = new StringBuilder();
            for (ProtocolDecoder.DecodedMessage msg : messages) {
                output.append(String.format("[%.3fs-%.3fs] %s: %s\n", msg.startTime, msg.endTime, msg.protocol, msg.message));
            }
            protocolOutput.setText(output.toString());
        } else {
            protocolOutput.clear();
        }
    }

    private void drawWaveforms() {
        // Set textured background with subtle gradient
        Stop[] stops = new Stop[] {
                new Stop(0, Color.rgb(20, 20, 30)),
                new Stop(1, Color.rgb(40, 40, 50))
        };
        LinearGradient bgGradient = new LinearGradient(0, 0, 0, canvas.getHeight(), false, CycleMethod.NO_CYCLE, stops);
        gc.setFill(bgGradient);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (isSpectrumMode) {
            // Compute FFT for both channels
            double sampleRate = samples / timebaseSlider.getValue();
            double maxFreq = maxFreqSlider.getValue();
            String windowType = fftWindowChoice.getValue();
            double[] ch1Spectrum = FFT.computeMagnitudeSpectrum(ch1Data, windowType, samples);
            double[] ch2Spectrum = FFT.computeMagnitudeSpectrum(ch2Data, windowType, samples);

            // Draw frequency domain grid
            gc.setStroke(Color.rgb(100, 80, 50, 0.5));
            gc.setLineWidth(0.8);
            for (int i = 0; i <= canvas.getWidth(); i += canvas.getWidth() / 10) {
                gc.strokeLine(i, 0, i, canvas.getHeight());
            }
            for (int i = 0; i <= canvas.getHeight(); i += canvas.getHeight() / 8) {
                gc.strokeLine(0, i, canvas.getWidth(), i);
            }

            // Draw frequency axis labels
            gc.setFill(Color.rgb(100, 80, 50));
            for (int i = 0; i <= 10; i++) {
                double freq = i * maxFreq / 10;
                gc.fillText(String.format("%.0f Hz", freq), i * canvas.getWidth() / 10, canvas.getHeight() - 10);
            }

            // Draw spectra with phosphor-like effects
            double height = canvas.getHeight() / 2;
            double ch1Volt = ch1VoltSlider.getValue();
            double ch2Volt = ch2VoltSlider.getValue();
            double ch1Pos = ch1PosSlider.getValue();
            double ch2Pos = ch2PosSlider.getValue();
            double maxMagnitude = 0;
            for (int i = 0; i < ch1Spectrum.length; i++) {
                maxMagnitude = Math.max(maxMagnitude, Math.max(ch1Spectrum[i], ch2Spectrum[i]));
            }
            if (maxMagnitude == 0) maxMagnitude = 1;

            Glow glow = new Glow(0.7);
            GaussianBlur blur = new GaussianBlur(1.0);
            glow.setInput(blur);

            gc.setStroke(Color.rgb(255, 255, 100, 0.9));
            gc.setLineWidth(2.2);
            gc.setEffect(glow);
            gc.beginPath();
            for (int i = 0; i < ch1Spectrum.length; i++) {
                double freq = i * sampleRate / samples;
                if (freq > maxFreq) break;
                double x = (freq / maxFreq) * canvas.getWidth();
                double y = height - (ch1Spectrum[i] / maxMagnitude * height / ch1Volt) + ch1Pos;
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            gc.setStroke(Color.rgb(100, 255, 255, 0.9));
            gc.beginPath();
            for (int i = 0; i < ch2Spectrum.length; i++) {
                double freq = i * sampleRate / samples;
                if (freq > maxFreq) break;
                double x = (freq / maxFreq) * canvas.getWidth();
                double y = height - (ch2Spectrum[i] / maxMagnitude * height / ch2Volt) + ch2Pos;
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();
            gc.setEffect(null);
        } else {
            // Time-domain display
            gc.setStroke(Color.rgb(100, 80, 50, 0.5));
            gc.setLineWidth(0.8);
            for (int i = 0; i <= canvas.getWidth(); i += canvas.getWidth() / 10) {
                gc.strokeLine(i, 0, i, canvas.getHeight());
            }
            for (int i = 0; i <= canvas.getHeight(); i += canvas.getHeight() / 8) {
                gc.strokeLine(0, i, canvas.getWidth(), i);
            }
            gc.setStroke(Color.rgb(100, 80, 50, 0.2));
            gc.setLineWidth(0.4);
            for (int i = 0; i <= canvas.getWidth(); i += canvas.getWidth() / 50) {
                gc.strokeLine(i, 0, i, canvas.getHeight());
            }
            for (int i = 0; i <= canvas.getHeight(); i += canvas.getHeight() / 40) {
                gc.strokeLine(0, i, canvas.getWidth(), i);
            }
            gc.setStroke(Color.rgb(100, 80, 50));
            gc.setLineWidth(1.0);
            gc.setEffect(new Glow(0.3));
            gc.strokeLine(0, canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight() / 2);
            gc.setEffect(null);

            double height = canvas.getHeight() / 2;
            double ch1Volt = ch1VoltSlider.getValue();
            double ch2Volt = ch2VoltSlider.getValue();
            double ch1Pos = ch1PosSlider.getValue();
            double ch2Pos = ch2PosSlider.getValue();

            Glow glow = new Glow(0.7);
            GaussianBlur blur = new GaussianBlur(1.0);
            glow.setInput(blur);

            gc.setStroke(Color.rgb(255, 255, 100, 0.9));
            gc.setLineWidth(2.2);
            gc.setEffect(glow);
            gc.beginPath();
            for (int i = 0; i < samples; i++) {
                double x = i * canvas.getWidth() / samples;
                double y = height - (ch1Data[i] * height / ch1Volt) + ch1Pos;
                if ("Square".equals(ch1WaveType.getValue()) && i > 0 && Math.abs(ch1Data[i] - ch1Data[i - 1]) > 1.0) {
                    y += (ch1Data[i] > ch1Data[i - 1] ? 10 : -10) * ch1Volt / height;
                }
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            gc.setStroke(Color.rgb(100, 255, 255, 0.9));
            gc.beginPath();
            for (int i = 0; i < samples; i++) {
                double x = i * canvas.getWidth() / samples;
                double y = height - (ch2Data[i] * height / ch2Volt) + ch2Pos;
                if ("Square".equals(ch2WaveType.getValue()) && i > 0 && Math.abs(ch2Data[i] - ch2Data[i - 1]) > 1.0) {
                    y += (ch2Data[i] > ch2Data[i - 1] ? 10 : -10) * ch2Volt / height;
                }
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();
            gc.setEffect(null);

            // Draw protocol annotations
            String protocol = protocolChoice.getValue();
            if (!"None".equals(protocol) && !isSpectrumMode) {
                List<ProtocolDecoder.DecodedMessage> messages = decoder.decode(ch1Data, ch2Data, timebaseSlider.getValue(), samples, protocol);
                gc.setFill(Color.WHITE);
                gc.setFont(new javafx.scene.text.Font("Courier New", 10));
                for (ProtocolDecoder.DecodedMessage msg : messages) {
                    double xStart = (msg.startTime / timebaseSlider.getValue()) * canvas.getWidth();
                    double xEnd = (msg.endTime / timebaseSlider.getValue()) * canvas.getWidth();
                    double y = "CH1".equals(triggerSource.getValue()) ? ch1Pos - 20 : ch2Pos + 20;
                    gc.fillText(msg.message, xStart, y);
                }
            }
        }
    }

    private double parseFrequency(String text, double defaultValue) {
        try {
            double value = Double.parseDouble(text);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int findTriggerPoint(double[] data, double level, boolean rising) {
        double hysteresis = 0.05;
        for (int i = 1; i < data.length; i++) {
            if (rising && data[i - 1] < level - hysteresis && data[i] >= level + hysteresis) {
                return i;
            } else if (!rising && data[i - 1] > level + hysteresis && data[i] <= level - hysteresis) {
                return i;
            }
        }
        return 0;
    }

    private void shiftData(double[] data, int shift) {
        double[] temp = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            temp[i] = data[(i + shift) % data.length];
        }
        System.arraycopy(temp, 0, data, 0, data.length);
    }
}