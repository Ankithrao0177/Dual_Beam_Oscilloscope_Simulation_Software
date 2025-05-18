package db.oscilloscope.app;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

public class EyeDiagramController {
    @FXML private Canvas canvas;
    @FXML private RadioButton signalTypeNRZ;
    @FXML private RadioButton signalTypePAM4;
    @FXML private Slider bitRateSlider;
    @FXML private Label bitRateLabel;
    @FXML private Slider amplitudeSlider;
    @FXML private Label amplitudeLabel;
    @FXML private Slider noiseSlider;
    @FXML private Label noiseLabel;
    @FXML private Slider jitterSlider;
    @FXML private Label jitterLabel;
    @FXML private Label eyeHeightLabel;
    @FXML private Label eyeWidthLabel;
    @FXML private Label jitterRMSLabel;
    @FXML private Label jitterPPLabel;
    @FXML private Label tieLabel;
    @FXML private Label cycleJitterLabel;

    private GraphicsContext gc;
    private final int samplesPerSymbol = 100;
    private final int symbolsPerTrace = 2; // 2 UI per trace
    private final int numTraces = 1000; // Number of overlaid traces
    private double[] signalData;
    private double[] tieData;
    private double[] cycleJitterData;
    private Random random = new Random();
    private double lastBitRate = -1;
    private double lastAmplitude = -1;
    private double lastNoise = -1;
    private double lastJitter = -1;
    private String currentSignalType = "NRZ";
    private String lastSignalType = "";

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        signalData = new double[samplesPerSymbol * symbolsPerTrace];
        tieData = new double[numTraces];
        cycleJitterData = new double[numTraces];

        // Set up toggle group for signal type
        ToggleGroup signalTypeGroup = new ToggleGroup();
        signalTypeNRZ.setToggleGroup(signalTypeGroup);
        signalTypePAM4.setToggleGroup(signalTypeGroup);
        signalTypeNRZ.setUserData("NRZ");
        signalTypePAM4.setUserData("PAM4");
        signalTypeNRZ.setSelected(true);

        // Add tooltips
        signalTypeNRZ.setTooltip(new Tooltip("Select NRZ signaling"));
        signalTypePAM4.setTooltip(new Tooltip("Select PAM4 signaling"));
        bitRateSlider.setTooltip(new Tooltip("Adjust bit rate (Mbps)"));
        amplitudeSlider.setTooltip(new Tooltip("Adjust signal amplitude (V)"));
        noiseSlider.setTooltip(new Tooltip("Adjust noise level (mV RMS)"));
        jitterSlider.setTooltip(new Tooltip("Adjust jitter (ps RMS)"));

        // Update labels with slider values
        updateSliderLabels();
        bitRateSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        amplitudeSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        noiseSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        jitterSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());

        // Signal type selection listener
        signalTypeGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle != null) {
                currentSignalType = (String) toggle.getUserData();
                lastSignalType = ""; // Force update
            }
        });

        // Start animation loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateEyeDiagram();
                drawEyeDiagram();
            }
        }.start();
    }

    private void updateSliderLabels() {
        bitRateLabel.setText(String.format("Bit Rate: %.0f Mbps", bitRateSlider.getValue()));
        amplitudeLabel.setText(String.format("Amplitude: %.2f V", amplitudeSlider.getValue()));
        noiseLabel.setText(String.format("Noise: %.0f mV RMS", noiseSlider.getValue()));
        jitterLabel.setText(String.format("Jitter: %.0f ps RMS", jitterSlider.getValue()));
    }

    private void updateEyeDiagram() {
        double bitRate = bitRateSlider.getValue() * 1e6; // Mbps to bps
        double amplitude = amplitudeSlider.getValue();
        double noiseRMS = noiseSlider.getValue() * 1e-3; // mV to V
        double jitterRMS = jitterSlider.getValue() * 1e-12; // ps to s
        String signalType = currentSignalType;

        if (bitRate != lastBitRate || amplitude != lastAmplitude || noiseRMS != lastNoise ||
                jitterRMS != lastJitter || !signalType.equals(lastSignalType)) {
            double symbolPeriod = 1.0 / bitRate; // seconds
            double dt = symbolPeriod / samplesPerSymbol;

            // Clear metrics
            double eyeHeight = 0;
            double eyeWidth = 0;
            //double jitterRMS = 0;
            double jitterPP = 0;
            double tieRMS = 0;
            double cycleJitterRMS = 0;

            // Generate multiple traces for eye diagram
            double[][] allTraces = new double[numTraces][samplesPerSymbol * symbolsPerTrace];
            for (int trace = 0; trace < numTraces; trace++) {
                // Generate random bits
                int[] bits = new int[symbolsPerTrace + 1]; // +1 for transition
                for (int i = 0; i < bits.length; i++) {
                    bits[i] = signalType.equals("NRZ") ? random.nextInt(2) : random.nextInt(4);
                }

                // Generate signal with jitter and noise
                double lastEdgeTime = 0;
                for (int i = 0; i < symbolsPerTrace; i++) {
                    double jitter = random.nextGaussian() * jitterRMS;
                    double edgeTime = (i + 1) * symbolPeriod + jitter;
                    tieData[trace] += Math.abs(jitter) * 1e12; // ps for TIE
                    if (i > 0) {
                        cycleJitterData[trace] += Math.abs(edgeTime - lastEdgeTime - symbolPeriod) * 1e12; // ps
                    }
                    lastEdgeTime = edgeTime;

                    for (int j = 0; j < samplesPerSymbol; j++) {
                        double t = i * symbolPeriod + j * dt;
                        double value;
                        if (signalType.equals("NRZ")) {
                            value = bits[i] == 1 ? amplitude : -amplitude;
                        } else { // PAM4
                            value = switch (bits[i]) {
                                case 0 -> -amplitude;
                                case 1 -> -amplitude / 3;
                                case 2 -> amplitude / 3;
                                case 3 -> amplitude;
                                default -> 0;
                            };
                        }
                        // Add noise
                        value += random.nextGaussian() * noiseRMS;
                        allTraces[trace][i * samplesPerSymbol + j] = value;
                    }
                }
            }

            // Compute metrics
            // Eye height: Min difference between levels at midpoint
            double midpoint = samplesPerSymbol / 2;
            double[] midValues = new double[numTraces];
            for (int trace = 0; trace < numTraces; trace++) {
                midValues[trace] = allTraces[trace][(int) midpoint];
            }
            if (signalType.equals("NRZ")) {
                double high = Double.MAX_VALUE, low = -Double.MAX_VALUE;
                for (double v : midValues) {
                    if (v > 0 && v < high) high = v;
                    if (v < 0 && v > low) low = v;
                }
                eyeHeight = high - low;
            } else { // PAM4
                double[] levels = new double[4];
                for (int i = 0; i < 4; i++) {
                    double sum = 0;
                    int count = 0;
                    for (double v : midValues) {
                        double expected = switch (i) {
                            case 0 -> -amplitude;
                            case 1 -> -amplitude / 3;
                            case 2 -> amplitude / 3;
                            case 3 -> amplitude;
                            default -> 0;
                        };
                        if (Math.abs(v - expected) < amplitude / 6) {
                            sum += v;
                            count++;
                        }
                    }
                    levels[i] = count > 0 ? sum / count : 0;
                }
                eyeHeight = Double.MAX_VALUE;
                for (int i = 0; i < 3; i++) {
                    double diff = Math.abs(levels[i + 1] - levels[i]);
                    if (diff < eyeHeight) eyeHeight = diff;
                }
            }

            // Eye width: Time between crossings at zero
            double[] crossings = new double[numTraces];
            int crossingCount = 0;
            for (int trace = 0; trace < numTraces; trace++) {
                for (int i = 1; i < samplesPerSymbol * symbolsPerTrace - 1; i++) {
                    if (allTraces[trace][i] * allTraces[trace][i + 1] <= 0) {
                        crossings[crossingCount++] = i * dt;
                        break;
                    }
                }
            }
            if (crossingCount > 1) {
                double sum = 0;
                for (int i = 1; i < crossingCount; i++) {
                    sum += crossings[i] - crossings[i - 1];
                }
                eyeWidth = (sum / (crossingCount - 1)) * 1e9; // ns
            }

            // Jitter metrics
            double tieSum = 0, tieSumSq = 0;
            double cycleSum = 0, cycleSumSq = 0;
            for (int i = 0; i < numTraces; i++) {
                tieSum += tieData[i];
                tieSumSq += tieData[i] * tieData[i];
                cycleSum += cycleJitterData[i];
                cycleSumSq += cycleJitterData[i] * cycleJitterData[i];
            }
            tieRMS = Math.sqrt(tieSumSq / numTraces - (tieSum / numTraces) * (tieSum / numTraces));
            cycleJitterRMS = Math.sqrt(cycleSumSq / numTraces - (cycleSum / numTraces) * (cycleSum / numTraces));
            jitterRMS = tieRMS; // RMS jitter from TIE
            double tieMin = Double.MAX_VALUE, tieMax = -Double.MAX_VALUE;
            for (double tie : tieData) {
                if (tie < tieMin) tieMin = tie;
                if (tie > tieMax) tieMax = tie;
            }
            jitterPP = tieMax - tieMin;

            // Update labels
            eyeHeightLabel.setText(String.format("Eye Height: %.3f V", eyeHeight));
            eyeWidthLabel.setText(String.format("Eye Width: %.2f ns", eyeWidth));
            jitterRMSLabel.setText(String.format("Jitter RMS: %.2f ps", jitterRMS));
            jitterPPLabel.setText(String.format("Jitter P-P: %.2f ps", jitterPP));
            tieLabel.setText(String.format("TIE RMS: %.2f ps", tieRMS));
            cycleJitterLabel.setText(String.format("Cycle Jitter: %.2f ps", cycleJitterRMS));

            // Store traces for drawing
            signalData = allTraces[0]; // For drawing, we'll overlay all traces
            lastBitRate = bitRate;
            lastAmplitude = amplitude;
            lastNoise = noiseRMS;
            lastJitter = jitterRMS;
            lastSignalType = signalType;
        }
    }

    private void drawEyeDiagram() {
        // Background
        Stop[] stops = new Stop[] {
                new Stop(0, Color.rgb(20, 20, 30)),
                new Stop(1, Color.rgb(40, 40, 50))
        };
        LinearGradient bgGradient = new LinearGradient(0, 0, 0, canvas.getHeight(), false, CycleMethod.NO_CYCLE, stops);
        gc.setFill(bgGradient);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Grid
        gc.setStroke(Color.rgb(100, 80, 50, 0.5));
        gc.setLineWidth(0.8);
        for (int i = 0; i <= canvas.getWidth(); i += canvas.getWidth() / 4) { // 4 UI
            gc.strokeLine(i, 0, i, canvas.getHeight());
        }
        for (int i = 0; i <= canvas.getHeight(); i += canvas.getHeight() / 8) {
            gc.strokeLine(0, i, canvas.getWidth(), i);
        }
        gc.setStroke(Color.rgb(100, 80, 50));
        gc.setLineWidth(1.0);
        gc.setEffect(new Glow(0.3));
        gc.strokeLine(0, canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight() / 2);
        gc.setEffect(null);

        // Draw eye diagram
        double height = canvas.getHeight() / 2;
        double voltScale = height / (currentSignalType.equals("NRZ") ? amplitudeSlider.getValue() : amplitudeSlider.getValue() * 1.5);
        Glow glow = new Glow(0.7);
        GaussianBlur blur = new GaussianBlur(1.0);
        glow.setInput(blur);
        gc.setStroke(Color.rgb(255, 255, 100, 0.05)); // Faint yellow for overlay
        gc.setLineWidth(1.0);
        gc.setEffect(glow);

        double bitRate = bitRateSlider.getValue() * 1e6;
        double symbolPeriod = 1.0 / bitRate;
        double dt = symbolPeriod / samplesPerSymbol;

        for (int trace = 0; trace < numTraces; trace++) {
            gc.beginPath();
            for (int i = 0; i < samplesPerSymbol * symbolsPerTrace; i++) {
                double x = (i % samplesPerSymbol + (i / samplesPerSymbol) * samplesPerSymbol) * canvas.getWidth() / (samplesPerSymbol * 2);
                double y = height - signalData[i] * voltScale;
                if (i == 0) {
                    gc.moveTo(x, y);
                } else {
                    gc.lineTo(x, y);
                }
            }
            gc.stroke();
        }
        gc.setEffect(null);
    }
}