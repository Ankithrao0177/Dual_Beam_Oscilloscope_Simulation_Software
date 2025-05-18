package db.oscilloscope.app;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import javafx.stage.Stage;
import java.io.IOException;
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
    @FXML private Button digitalToggleButton;
    @FXML private ChoiceBox<String> freqScaleChoice;
    @FXML private ChoiceBox<String> magScaleChoice;
    @FXML private CheckBox peakDetectionCheckBox;
    @FXML private Label snrLabel;
    @FXML private ChoiceBox<String> rbwChoice;
    @FXML private Slider dynamicRangeSlider;
    @FXML private Label dynamicRangeLabel;
    @FXML private Button compareSpectraButton;
    @FXML private Button modulationWindowButton;
    @FXML private ChoiceBox<String> mathOperationChoice;
    @FXML private Slider mathVoltSlider;
    @FXML private Slider mathPosSlider;
    @FXML private Label mathVoltLabel;
    @FXML private ChoiceBox<String> mathColorChoice;
    @FXML private ChoiceBox<String> demodulationChoice;
    @FXML private Slider basebandVoltSlider;
    @FXML private Slider basebandPosSlider;
    @FXML private Label basebandVoltLabel;
    @FXML private Label modulationParamsLabel;
    //@FXML private Button modulationWindowButton;
    @FXML private Button eyeDiagramButton;

    private WaveformGenerator generator;
    private ProtocolDecoder decoder;
    private Demodulator demodulator;
    private double[] ch1Data;
    private double[] ch2Data;
    private double[] mathData;
    private double[] basebandData;
    private boolean[] ch1Digital;
    private boolean[] ch2Digital;
    private GraphicsContext gc;
    private final int samples = 800;
    private boolean isRunning = true;
    private boolean isSpectrumMode = false;
    private boolean showDigital = false;
    private boolean isCompareSpectra = false;
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
    private String lastTriggerSource = "";
    private String lastTriggerSlope = "";
    private double lastTriggerLevel = Double.MAX_VALUE;
    private String lastRbw = "";
    private String lastMathOperation = "";
    private String lastProtocol = "";
    private String lastDemodulation = "";
    private long lastToggle = 0;

    @FXML
    public void initialize() {
        generator = new WaveformGenerator();
        decoder = new ProtocolDecoder();
        demodulator = new Demodulator();
        gc = canvas.getGraphicsContext2D();
        ch1Data = new double[samples];
        ch2Data = new double[samples];
        mathData = new double[samples];
        basebandData = new double[samples];
        ch1Digital = new boolean[samples];
        ch2Digital = new boolean[samples];

        // Initialize choice boxes
        ch1WaveType.getItems().addAll("Sine", "Square", "Triangle", "Sawtooth");
        ch2WaveType.getItems().addAll("Sine", "Square", "Triangle", "Sawtooth");
        ch1WaveType.setValue("Sine");
        ch2WaveType.setValue("Sine");
        ch1Coupling.getItems().addAll("AC", "DC");
        ch2Coupling.getItems().addAll("AC", "DC");
        ch1Coupling.setValue("DC");
        ch2Coupling.setValue("DC");
        triggerSource.getItems().addAll("CH1", "CH2");
        triggerSource.setValue("CH1");
        triggerSlope.getItems().addAll("Rising", "Falling");
        triggerSlope.setValue("Rising");
        fftWindowChoice.getItems().addAll("Rectangular", "Hamming", "Blackman", "Kaiser", "Gaussian");
        fftWindowChoice.setValue("Hamming");
        protocolChoice.getItems().addAll("None", "I2C", "SPI", "UART", "CAN");
        protocolChoice.setValue("None");
        demodulationChoice.getItems().addAll("None", "AM", "FM", "PM");
        demodulationChoice.setValue("None");
        freqScaleChoice.getItems().addAll("Linear", "Logarithmic");
        freqScaleChoice.setValue("Linear");
        magScaleChoice.getItems().addAll("Linear", "dB");
        magScaleChoice.setValue("Linear");
        mathOperationChoice.getItems().addAll("None", "Add", "Subtract", "Multiply",
                "Differentiate CH1", "Differentiate CH2",
                "Integrate CH1", "Integrate CH2");
        mathOperationChoice.setValue("None");
        mathColorChoice.getItems().addAll("Green", "Red", "Blue", "Yellow");
        mathColorChoice.setValue("Green");

        // Initialize RBW choices dynamically based on sample rate
        updateRbwChoices();
        timebaseSlider.valueProperty().addListener((obs, old, val) -> updateRbwChoices());
        rbwChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> lastRbw = "");

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
        if (maxFreqSlider != null) {
            maxFreqSlider.setTooltip(new Tooltip("Adjust maximum frequency displayed (Hz)"));
        }
        protocolChoice.setTooltip(new Tooltip("Select protocol to decode (CH1/CH2 signals). Use square waves for best results."));
        protocolOutput.setTooltip(new Tooltip("Displays decoded protocol messages"));
        digitalToggleButton.setTooltip(new Tooltip("Toggle digital signal display"));
        freqScaleChoice.setTooltip(new Tooltip("Select frequency axis scale (Linear or Logarithmic)"));
        magScaleChoice.setTooltip(new Tooltip("Select magnitude scale (Linear or dB)"));
        peakDetectionCheckBox.setTooltip(new Tooltip("Enable/disable peak detection and markers"));
        snrLabel.setTooltip(new Tooltip("Signal-to-noise ratio for CH1 and CH2"));
        rbwChoice.setTooltip(new Tooltip("Select resolution bandwidth for FFT (Hz)"));
        dynamicRangeSlider.setTooltip(new Tooltip("Adjust dynamic range for magnitude spectrum (dB)"));
        dynamicRangeLabel.setTooltip(new Tooltip("Dynamic range for magnitude spectrum (dB)"));
        compareSpectraButton.setTooltip(new Tooltip("Toggle between separate and combined spectrum plots"));
        mathOperationChoice.setTooltip(new Tooltip("Select math operation for CH1 and CH2"));
        mathVoltSlider.setTooltip(new Tooltip("Adjusts Math channel vertical scale (volts/div)"));
        mathPosSlider.setTooltip(new Tooltip("Adjusts Math channel vertical position"));
        mathColorChoice.setTooltip(new Tooltip("Select Math channel trace color"));
        demodulationChoice.setTooltip(new Tooltip("Select demodulation type for CH1 signal (AM, FM, PM)"));
        basebandVoltSlider.setTooltip(new Tooltip("Adjusts baseband vertical scale (volts/div)"));
        basebandPosSlider.setTooltip(new Tooltip("Adjusts baseband vertical position"));
        modulationParamsLabel.setTooltip(new Tooltip("Displays carrier frequency and modulation parameters"));
        modulationWindowButton.setTooltip(new Tooltip("Open modulation simulator window"));

        // Update labels with slider values
        updateSliderLabels();
        timebaseSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch1VoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch2VoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch1AmplitudeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        ch2AmplitudeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        if (maxFreqSlider != null) {
            maxFreqSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        }
        dynamicRangeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        mathVoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        basebandVoltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());

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

        // Digital toggle button action
        digitalToggleButton.setOnAction(e -> {
            showDigital = !showDigital;
            digitalToggleButton.setText(showDigital ? "Hide Digital" : "Show Digital");
        });

        // Compare spectra button action with debouncing
        compareSpectraButton.setOnAction(e -> {
            if (System.currentTimeMillis() - lastToggle > 200) {
                isCompareSpectra = !isCompareSpectra;
                compareSpectraButton.setText(isCompareSpectra ? "Separate Spectra" : "Compare Spectra");
                lastToggle = System.currentTimeMillis();
            }
        });

        /*
        // Modulation window button action
        modulationWindowButton.setOnAction(e -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/db/oscilloscope/app/modulation.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 600, 600);
                Stage stage = new Stage();
                stage.setTitle("Modulation Simulator");
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        */

        // Modulation window button action
        modulationWindowButton.setOnAction(e -> {
            try {
                String fxmlPath = "/db/oscilloscope/app/modulation.fxml";
                if (getClass().getResource(fxmlPath) == null) {
                    throw new IOException("Resource not found: " + fxmlPath);
                }
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
                Scene scene = new Scene(fxmlLoader.load(), 700, 600);
                Stage stage = new Stage();
                stage.setTitle("Modulation Simulation");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to open Modulation Simulator");
                alert.setContentText("Could not load modulation.fxml: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        // Eye diagram window button action
        eyeDiagramButton.setOnAction(e -> {
            try {
                String fxmlPath = "/db/oscilloscope/app/eyeDiagram.fxml";
                if (getClass().getResource(fxmlPath) == null) {
                    throw new IOException("Resource not found: " + fxmlPath);
                }
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
                Scene scene = new Scene(fxmlLoader.load(), 700, 600);
                Stage stage = new Stage();
                stage.setTitle("Eye Diagram Analysis");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to open Eye Diagram Analyzer");
                alert.setContentText("Could not load eyeDiagram.fxml: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        // Protocol choice listener to clear output
        protocolChoice.valueProperty().addListener((obs, old, val) -> protocolOutput.clear());

        // Demodulation choice listener to clear parameters
        demodulationChoice.valueProperty().addListener((obs, old, val) -> modulationParamsLabel.setText("Carrier: 0 Hz, Index: 0"));

        // Math operation listener to force waveform update
        mathOperationChoice.valueProperty().addListener((obs, old, val) -> lastMathOperation = "");

        // Start animation loop
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (isRunning) {
                    updateWaveforms();
                    drawWaveforms();
                    time += 0.016; // Approx 60 FPS
                }
            }
        }.start();
    }

    private void updateRbwChoices() {
        double sampleRate = samples / timebaseSlider.getValue();
        rbwChoice.getItems().clear();
        int[] fftSizes = {256, 512, 1024, 2048, 4096, 8192};
        for (int fftSize : fftSizes) {
            double rbw = sampleRate / fftSize;
            if (rbw >= 1 && rbw <= 100) {
                rbwChoice.getItems().add(String.format("%.1f Hz", rbw));
            }
        }
        if (!rbwChoice.getItems().isEmpty() && rbwChoice.getValue() == null) {
            rbwChoice.setValue(rbwChoice.getItems().get(0));
        }
    }

    private void updateSliderLabels() {
        timebaseLabel.setText(String.format("Timebase: %.3f s/div", timebaseSlider.getValue()));
        ch1VoltLabel.setText(String.format("CH1: %.2f V/div", ch1VoltSlider.getValue()));
        ch2VoltLabel.setText(String.format("CH2: %.2f V/div", ch2VoltSlider.getValue()));
        ch1AmplitudeLabel.setText(String.format("CH1 Amp: %.2f", ch1AmplitudeSlider.getValue()));
        ch2AmplitudeLabel.setText(String.format("CH2 Amp: %.2f", ch2AmplitudeSlider.getValue()));
        maxFreqLabel.setText(String.format("Max Freq: %.0f Hz", maxFreqSlider != null ? maxFreqSlider.getValue() : 500));
        dynamicRangeLabel.setText(String.format("Dyn Range: %.0f dB", dynamicRangeSlider.getValue()));
        mathVoltLabel.setText(String.format("Math: %.2f V/div", mathVoltSlider.getValue()));
        basebandVoltLabel.setText(String.format("Baseband: %.2f V/div", basebandVoltSlider.getValue()));
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

    private Color getMathChannelColor() {
        String color = mathColorChoice.getValue();
        switch (color) {
            case "Red": return Color.rgb(255, 0, 0, 0.9);
            case "Blue": return Color.rgb(0, 0, 255, 0.9);
            case "Yellow": return Color.rgb(255, 255, 0, 0.9);
            case "Green":
            default: return Color.rgb(0, 255, 0, 0.9);
        }
    }

    private double parseFrequency(String text, double defaultValue) {
        try {
            double freq = Double.parseDouble(text);
            return freq > 0 ? freq : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int findTriggerPoint(double[] data, double level, boolean rising) {
        for (int i = 1; i < data.length; i++) {
            if (rising && data[i - 1] <= level && data[i] > level) {
                return i;
            } else if (!rising && data[i - 1] >= level && data[i] < level) {
                return i;
            }
        }
        return 0;
    }

    private void shiftData(double[] data, int shift) {
        if (shift == 0) return;
        double[] temp = new double[data.length];
        System.arraycopy(data, shift, temp, 0, data.length - shift);
        System.arraycopy(data, 0, temp, data.length - shift, shift);
        System.arraycopy(temp, 0, data, 0, data.length);
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
        String triggerSourceVal = triggerSource.getValue();
        String triggerSlopeVal = triggerSlope.getValue();
        String rbwVal = rbwChoice.getValue() != null ? rbwChoice.getValue() : "10.0 Hz";
        String mathOperation = mathOperationChoice.getValue();
        String protocol = protocolChoice.getValue();
        String demodulation = demodulationChoice.getValue();

        // Only regenerate waveforms if parameters have changed
        boolean waveformChanged = timebase != lastTimebase || ch1Freq != lastCh1Freq || ch2Freq != lastCh2Freq ||
                !ch1Type.equals(lastCh1Type) || !ch2Type.equals(lastCh2Type) ||
                ch1AC != lastCh1AC || ch2AC != lastCh2AC ||
                ch1Amplitude != lastCh1Amplitude || ch2Amplitude != lastCh2Amplitude ||
                !triggerSourceVal.equals(lastTriggerSource) || !triggerSlopeVal.equals(lastTriggerSlope) ||
                triggerLevel != lastTriggerLevel || !rbwVal.equals(lastRbw) ||
                !mathOperation.equals(lastMathOperation);

        if (waveformChanged) {
            ch1Data = generator.generateWaveform(ch1Type, ch1Freq, timebase, samples, time, ch1AC, ch1Amplitude);
            ch2Data = generator.generateWaveform(ch2Type, ch2Freq, timebase, samples, time, ch2AC, ch2Amplitude);

            // Compute math channel
            MathChannel.Operation op;
            switch (mathOperation) {
                case "Add": op = MathChannel.Operation.ADD; break;
                case "Subtract": op = MathChannel.Operation.SUBTRACT; break;
                case "Multiply": op = MathChannel.Operation.MULTIPLY; break;
                case "Differentiate CH1": op = MathChannel.Operation.DIFFERENTIATE_CH1; break;
                case "Differentiate CH2": op = MathChannel.Operation.DIFFERENTIATE_CH2; break;
                case "Integrate CH1": op = MathChannel.Operation.INTEGRATE_CH1; break;
                case "Integrate CH2": op = MathChannel.Operation.INTEGRATE_CH2; break;
                default: op = MathChannel.Operation.NONE;
            }
            mathData = MathChannel.computeMathChannel(ch1Data, ch2Data, op, timebase, samples);

            lastTimebase = timebase;
            lastCh1Freq = ch1Freq;
            lastCh2Freq = ch2Freq;
            lastCh1Type = ch1Type;
            lastCh2Type = ch2Type;
            lastCh1AC = ch1AC;
            lastCh2AC = ch2AC;
            lastCh1Amplitude = ch1Amplitude;
            lastCh2Amplitude = ch2Amplitude;
            lastTriggerSource = triggerSourceVal;
            lastTriggerSlope = triggerSlopeVal;
            lastTriggerLevel = triggerLevel;
            lastRbw = rbwVal;
            lastMathOperation = mathOperation;
        }

        // Apply trigger with hysteresis
        boolean useCh1 = triggerSource.getValue().equals("CH1");
        boolean rising = triggerSlope.getValue().equals("Rising");
        int triggerPoint = findTriggerPoint(useCh1 ? ch1Data : ch2Data, triggerLevel, rising);
        shiftData(ch1Data, triggerPoint);
        shiftData(ch2Data, triggerPoint);
        shiftData(mathData, triggerPoint);
        shiftData(basebandData, triggerPoint);

        // Generate digital signals
        for (int i = 0; i < samples; i++) {
            ch1Digital[i] = ch1Data[i] > 0.5;
            ch2Digital[i] = ch2Data[i] > 0.5;
        }

        // Decode protocol if selected and relevant parameters have changed
        if (!"None".equals(protocol) && (waveformChanged || !protocol.equals(lastProtocol))) {
            List<ProtocolDecoder.DecodedMessage> messages = decoder.decode(ch1Data, ch2Data, timebase, samples, protocol);
            StringBuilder output = new StringBuilder();
            for (ProtocolDecoder.DecodedMessage msg : messages) {
                output.append(String.format("[%.3fs-%.3fs] %s: %s\n", msg.startTime, msg.endTime, msg.protocol, msg.message));
            }
            protocolOutput.setText(output.toString());
            lastProtocol = protocol;
        } else if ("None".equals(protocol)) {
            protocolOutput.clear();
        }

        // Demodulate CH1 if selected and relevant parameters have changed
        if (!"None".equals(demodulation) && (waveformChanged || !demodulation.equals(lastDemodulation))) {
            Demodulator.DemodulationResult result = demodulator.demodulate(ch1Data, timebase, samples, demodulation);
            basebandData = result.basebandSignal;
            String paramFormat = demodulation.equals("FM") ? "%.0f" : "%.2f";
            modulationParamsLabel.setText(String.format("Carrier: %.0f Hz, %s: " + paramFormat,
                    result.carrierFrequency, result.paramName, result.modulationParam));
            lastDemodulation = demodulation;
        } else if ("None".equals(demodulation)) {
            modulationParamsLabel.setText("Carrier: 0 Hz, Index: 0");
            for (int i = 0; i < samples; i++) {
                basebandData[i] = 0;
            }
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
            // Compute FFT for both channels and math channel
            double sampleRate = samples / timebaseSlider.getValue();
            double maxFreq = maxFreqSlider != null ? maxFreqSlider.getValue() : 500;
            String windowType = fftWindowChoice.getValue();
            boolean isDbScale = "dB".equals(magScaleChoice.getValue());
            double dynamicRange = dynamicRangeSlider.getValue();
            String mathOperation = mathOperationChoice.getValue();

            // Determine FFT size based on RBW
            double rbw = rbwChoice.getValue() != null ? Double.parseDouble(rbwChoice.getValue().replace(" Hz", "")) : 10.0;
            int fftSize = (int) Math.pow(2, Math.ceil(Math.log(sampleRate / rbw) / Math.log(2)));
            fftSize = Math.min(Math.max(fftSize, 256), 8192);

            // Prepare data for FFT
            double[] ch1FftData = new double[fftSize];
            double[] ch2FftData = new double[fftSize];
            double[] mathFftData = new double[fftSize];
            for (int i = 0; i < fftSize; i++) {
                ch1FftData[i] = i < samples ? ch1Data[i] : 0;
                ch2FftData[i] = i < samples ? ch2Data[i] : 0;
                mathFftData[i] = i < samples ? mathData[i] : 0;
            }

            // Compute FFT
            FFT.Spectrum ch1Spectrum = FFT.computeSpectrum(ch1FftData, windowType, fftSize);
            FFT.Spectrum ch2Spectrum = FFT.computeSpectrum(ch2FftData, windowType, fftSize);
            FFT.Spectrum mathSpectrum = FFT.computeSpectrum(mathFftData, windowType, fftSize);

            // Peak detection and SNR calculation (only for CH1 and CH2)
            PeakDetector.DetectionResult ch1Result = null;
            PeakDetector.DetectionResult ch2Result = null;
            if (peakDetectionCheckBox.isSelected()) {
                ch1Result = PeakDetector.detectPeaks(ch1Spectrum.magnitude, sampleRate, fftSize, maxFreq, isDbScale);
                ch2Result = PeakDetector.detectPeaks(ch2Spectrum.magnitude, sampleRate, fftSize, maxFreq, isDbScale);
                snrLabel.setText(String.format("SNR: CH1 %.1f dB, CH2 %.1f dB",
                        ch1Result.snr, ch2Result.snr));
            } else {
                snrLabel.setText("SNR: Disabled");
            }

            // Draw frequency domain grid
            gc.setStroke(Color.rgb(100, 80, 50, 0.5));
            gc.setLineWidth(0.8);
            double magHeight = canvas.getHeight() / 2;
            double phaseHeight = canvas.getHeight() / 2;
            for (int i = 0; i <= canvas.getWidth(); i += canvas.getWidth() / 10) {
                gc.strokeLine(i, 0, i, magHeight);
                gc.strokeLine(i, magHeight, i, canvas.getHeight());
            }
            for (int i = 0; i <= magHeight; i += magHeight / 4) {
                gc.strokeLine(0, i, canvas.getWidth(), i);
            }
            for (int i = (int) magHeight; i <= canvas.getHeight(); i += phaseHeight / 4) {
                gc.strokeLine(0, i, canvas.getWidth(), i);
            }

            // Draw frequency axis labels
            gc.setFill(Color.rgb(100, 80, 50));
            boolean isLogScale = "Logarithmic".equals(freqScaleChoice.getValue());
            double minFreq = isLogScale ? Math.max(1, maxFreq / 1000) : 0;
            for (int i = 0; i <= 10; i++) {
                double freq = isLogScale ?
                        minFreq * Math.pow(maxFreq / minFreq, i / 10.0) :
                        i * maxFreq / 10;
                double x = isLogScale ?
                        ((Math.log10(freq) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                        (freq / maxFreq) * canvas.getWidth();
                gc.fillText(String.format("%.0f Hz", freq), x, magHeight - 10);
                gc.fillText(String.format("%.0f Hz", freq), x, canvas.getHeight() - 10);
            }

            // Draw magnitude and phase labels
            gc.setFill(Color.rgb(100, 80, 50));
            gc.fillText(isDbScale ? "Magnitude (dB)" : "Magnitude", 10, 20);
            gc.fillText("Phase (rad)", 10, magHeight + 20);
            for (int i = -2; i <= 2; i++) {
                double y = magHeight + phaseHeight - (i + 2) * (phaseHeight / 4);
                gc.fillText(String.format("%.2fπ", i / 2.0), 10, y + 5);
            }

            // Draw spectra with phosphor-like effects
            double ch1Volt = ch1VoltSlider.getValue();
            double ch2Volt = ch2VoltSlider.getValue();
            double mathVolt = mathVoltSlider.getValue();
            double ch1Pos = ch1PosSlider.getValue();
            double ch2Pos = ch2PosSlider.getValue();
            double mathPos = mathPosSlider.getValue();
            double maxMagnitude = isDbScale ? 0 : 1;

            if (isDbScale) {
                for (int i = 0; i < ch1Spectrum.magnitude.length; i++) {
                    double mag = 20 * Math.log10(Math.max(ch1Spectrum.magnitude[i], 1e-10));
                    maxMagnitude = Math.max(maxMagnitude, mag);
                    mag = 20 * Math.log10(Math.max(ch2Spectrum.magnitude[i], 1e-10));
                    maxMagnitude = Math.max(maxMagnitude, mag);
                    if (!"None".equals(mathOperation)) {
                        mag = 20 * Math.log10(Math.max(mathSpectrum.magnitude[i], 1e-10));
                        maxMagnitude = Math.max(maxMagnitude, mag);
                    }
                }
                maxMagnitude = Math.min(maxMagnitude, 0);
            } else if (isCompareSpectra) {
                for (int i = 0; i < ch1Spectrum.magnitude.length; i++) {
                    maxMagnitude = Math.max(maxMagnitude, ch1Spectrum.magnitude[i]);
                    maxMagnitude = Math.max(maxMagnitude, ch2Spectrum.magnitude[i]);
                    if (!"None".equals(mathOperation)) {
                        maxMagnitude = Math.max(maxMagnitude, mathSpectrum.magnitude[i]);
                    }
                }
            }

            Glow glow = new Glow(0.7);
            GaussianBlur blur = new GaussianBlur(1.0);
            glow.setInput(blur);

            // Draw CH1 magnitude spectrum
            gc.setStroke(Color.rgb(255, 255, 100, 0.9));
            gc.setLineWidth(2.2);
            gc.setEffect(glow);
            gc.beginPath();
            for (int i = 0; i < ch1Spectrum.magnitude.length; i++) {
                double freq = i * sampleRate / fftSize;
                if (freq > maxFreq) break;
                double x = isLogScale ?
                        ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                        (freq / maxFreq) * canvas.getWidth();
                double mag = isDbScale ? 20 * Math.log10(Math.max(ch1Spectrum.magnitude[i], 1e-10)) : ch1Spectrum.magnitude[i];
                if (isDbScale && mag < dynamicRange) mag = dynamicRange;
                double y = isCompareSpectra ?
                        (isDbScale ?
                                magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / ch1Volt) + ch1Pos :
                                magHeight - (mag / maxMagnitude * magHeight / ch1Volt) + ch1Pos) :
                        (isDbScale ?
                                magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / ch1Volt) + ch1Pos :
                                magHeight - (mag / maxMagnitude * magHeight / ch1Volt) + ch1Pos);
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            // Draw CH2 magnitude spectrum
            gc.setStroke(Color.rgb(100, 255, 255, 0.9));
            gc.beginPath();
            for (int i = 0; i < ch2Spectrum.magnitude.length; i++) {
                double freq = i * sampleRate / fftSize;
                if (freq > maxFreq) break;
                double x = isLogScale ?
                        ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                        (freq / maxFreq) * canvas.getWidth();
                double mag = isDbScale ? 20 * Math.log10(Math.max(ch2Spectrum.magnitude[i], 1e-10)) : ch2Spectrum.magnitude[i];
                if (isDbScale && mag < dynamicRange) mag = dynamicRange;
                double y = isCompareSpectra ?
                        (isDbScale ?
                                magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / ch1Volt) + ch1Pos :
                                magHeight - (mag / maxMagnitude * magHeight / ch1Volt) + ch1Pos) :
                        (isDbScale ?
                                magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / ch2Volt) + ch2Pos :
                                magHeight - (mag / maxMagnitude * magHeight / ch2Volt) + ch2Pos);
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            // Draw Math channel magnitude spectrum (if enabled)
            if (!"None".equals(mathOperation)) {
                gc.setStroke(getMathChannelColor());
                gc.beginPath();
                for (int i = 0; i < mathSpectrum.magnitude.length; i++) {
                    double freq = i * sampleRate / fftSize;
                    if (freq > maxFreq) break;
                    double x = isLogScale ?
                            ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                            (freq / maxFreq) * canvas.getWidth();
                    double mag = isDbScale ? 20 * Math.log10(Math.max(mathSpectrum.magnitude[i], 1e-10)) : mathSpectrum.magnitude[i];
                    if (isDbScale && mag < dynamicRange) mag = dynamicRange;
                    double y = isCompareSpectra ?
                            (isDbScale ?
                                    magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / ch1Volt) + ch1Pos :
                                    magHeight - (mag / maxMagnitude * magHeight / ch1Volt) + ch1Pos) :
                            (isDbScale ?
                                    magHeight - ((mag - dynamicRange) / (0 - dynamicRange) * magHeight / mathVolt) + mathPos :
                                    magHeight - (mag / maxMagnitude * magHeight / mathVolt) + mathPos);
                    if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                }
                gc.stroke();
            }

            // Draw CH1 phase spectrum
            gc.setStroke(Color.rgb(255, 255, 100, 0.9));
            gc.beginPath();
            for (int i = 0; i < ch1Spectrum.phase.length; i++) {
                double freq = i * sampleRate / fftSize;
                if (freq > maxFreq) break;
                double x = isLogScale ?
                        ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                        (freq / maxFreq) * canvas.getWidth();
                double y = magHeight + phaseHeight - ((ch1Spectrum.phase[i] + Math.PI) / (2 * Math.PI)) * phaseHeight;
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            // Draw CH2 phase spectrum
            gc.setStroke(Color.rgb(100, 255, 255, 0.9));
            gc.beginPath();
            for (int i = 0; i < ch2Spectrum.phase.length; i++) {
                double freq = i * sampleRate / fftSize;
                if (freq > maxFreq) break;
                double x = isLogScale ?
                        ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                        (freq / maxFreq) * canvas.getWidth();
                double y = magHeight + phaseHeight - ((ch2Spectrum.phase[i] + Math.PI) / (2 * Math.PI)) * phaseHeight;
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();

            // Draw Math channel phase spectrum (if enabled)
            if (!"None".equals(mathOperation)) {
                gc.setStroke(getMathChannelColor());
                gc.beginPath();
                for (int i = 0; i < mathSpectrum.phase.length; i++) {
                    double freq = i * sampleRate / fftSize;
                    if (freq > maxFreq) break;
                    double x = isLogScale ?
                            ((Math.log10(Math.max(freq, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                            (freq / maxFreq) * canvas.getWidth();
                    double y = magHeight + phaseHeight - ((mathSpectrum.phase[i] + Math.PI) / (2 * Math.PI)) * phaseHeight;
                    if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                }
                gc.stroke();
            }

            // Draw peak markers and annotations (only for CH1 and CH2)
            if (peakDetectionCheckBox.isSelected() && ch1Result != null && ch2Result != null) {
                gc.setFont(new javafx.scene.text.Font("Courier New", 10));
                gc.setStroke(Color.rgb(255, 255, 100, 0.7));
                gc.setFill(Color.rgb(255, 255, 100, 0.9));
                for (PeakDetector.Peak peak : ch1Result.peaks) {
                    double x = isLogScale ?
                            ((Math.log10(Math.max(peak.frequency, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                            (peak.frequency / maxFreq) * canvas.getWidth();
                    gc.strokeLine(x, 0, x, magHeight);
                    String label = peak.isHarmonic ?
                            String.format("%dH: %.0fHz, %.1fdB", peak.harmonicOrder, peak.frequency, 20 * Math.log10(Math.max(peak.magnitude, 1e-10))) :
                            String.format("P: %.0fHz, %.1fdB", peak.frequency, 20 * Math.log10(Math.max(peak.magnitude, 1e-10)));
                    gc.fillText(label, x + 5, isCompareSpectra ? 30 : 30);
                }
                gc.setStroke(Color.rgb(100, 255, 255, 0.7));
                gc.setFill(Color.rgb(100, 255, 255, 0.9));
                for (PeakDetector.Peak peak : ch2Result.peaks) {
                    double x = isLogScale ?
                            ((Math.log10(Math.max(peak.frequency, minFreq)) - Math.log10(minFreq)) / (Math.log10(maxFreq) - Math.log10(minFreq))) * canvas.getWidth() :
                            (peak.frequency / maxFreq) * canvas.getWidth();
                    gc.strokeLine(x, 0, x, magHeight);
                    String label = peak.isHarmonic ?
                            String.format("%dH: %.0fHz, %.1fdB", peak.harmonicOrder, peak.frequency, 20 * Math.log10(Math.max(peak.magnitude, 1e-10))) :
                            String.format("P: %.0fHz, %.1fdB", peak.frequency, 20 * Math.log10(Math.max(peak.magnitude, 1e-10)));
                    gc.fillText(label, x + 5, isCompareSpectra ? 50 : 50);
                }
            }
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
            double mathVolt = mathVoltSlider.getValue();
            double basebandVolt = basebandVoltSlider.getValue();
            double ch1Pos = ch1PosSlider.getValue();
            double ch2Pos = ch2PosSlider.getValue();
            double mathPos = mathPosSlider.getValue();
            double basebandPos = basebandPosSlider.getValue();
            String mathOperation = mathOperationChoice.getValue();
            String demodulation = demodulationChoice.getValue();

            Glow glow = new Glow(0.7);
            GaussianBlur blur = new GaussianBlur(1.0);
            glow.setInput(blur);

            // Draw CH1 analog waveform
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

            // Draw CH2 analog waveform
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

            // Draw Math channel waveform (if enabled)
            if (!"None".equals(mathOperation)) {
                gc.setStroke(getMathChannelColor());
                gc.beginPath();
                for (int i = 0; i < samples; i++) {
                    double x = i * canvas.getWidth() / samples;
                    double y = height - (mathData[i] * height / mathVolt) + mathPos;
                    if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                }
                gc.stroke();
            }

            // Draw Baseband waveform (if enabled)
            if (!"None".equals(demodulation)) {
                gc.setStroke(Color.rgb(255, 0, 255, 0.9)); // Purple for baseband
                gc.beginPath();
                for (int i = 0; i < samples; i++) {
                    double x = i * canvas.getWidth() / samples;
                    double y = height - (basebandData[i] * height / basebandVolt) + basebandPos;
                    if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
                }
                gc.stroke();
            }

            // Draw digital signals if enabled
            if (showDigital) {
                gc.setLineWidth(1.5);
                gc.setEffect(null);

                gc.setStroke(Color.rgb(255, 255, 100, 0.7));
                double ch1DigitalY = ch1Pos + height / 4;
                gc.beginPath();
                for (int i = 0; i < samples; i++) {
                    double x = i * canvas.getWidth() / samples;
                    double y = ch1Digital[i] ? ch1DigitalY - 20 : ch1DigitalY + 20;
                    if (i == 0) {
                        gc.moveTo(x, y);
                    } else {
                        if (ch1Digital[i] != ch1Digital[i - 1]) {
                            gc.lineTo(x, ch1Digital[i - 1] ? ch1DigitalY - 20 : ch1DigitalY + 20);
                            gc.lineTo(x, y);
                        } else {
                            gc.lineTo(x, y);
                        }
                    }
                }
                gc.stroke();

                gc.setStroke(Color.rgb(100, 255, 255, 0.7));
                double ch2DigitalY = ch2Pos - height / 4;
                gc.beginPath();
                for (int i = 0; i < samples; i++) {
                    double x = i * canvas.getWidth() / samples;
                    double y = ch2Digital[i] ? ch2DigitalY - 20 : ch2DigitalY + 20;
                    if (i == 0) {
                        gc.moveTo(x, y);
                    } else {
                        if (ch2Digital[i] != ch1Digital[i - 1]) {
                            gc.lineTo(x, ch2Digital[i - 1] ? ch2DigitalY - 20 : ch2DigitalY + 20);
                            gc.lineTo(x, y);
                        } else {
                            gc.lineTo(x, y);
                        }
                    }
                }
                gc.stroke();
            }

            // Draw protocol annotations
            String protocol = protocolChoice.getValue();
            if (!"None".equals(protocol) && !isSpectrumMode) {
                List<ProtocolDecoder.DecodedMessage> messages = decoder.decode(ch1Data, ch2Data, timebaseSlider.getValue(), samples, protocol);
                gc.setFill(Color.WHITE);
                gc.setFont(new javafx.scene.text.Font("Courier New", 10));
                for (ProtocolDecoder.DecodedMessage msg : messages) {
                    double xStart = (msg.startTime / timebaseSlider.getValue()) * canvas.getWidth();
                    double y = triggerSource.getValue().equals("CH1") ? ch1Pos - 20 : ch2Pos + 20;
                    gc.fillText(msg.message, xStart, y);
                }
            }
            gc.setEffect(null);
        }
    }
}