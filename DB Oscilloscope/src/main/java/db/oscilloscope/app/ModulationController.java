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

public class ModulationController {
    @FXML private Canvas canvas;
    @FXML private RadioButton modTypeAM;
    @FXML private RadioButton modTypeFM;
    @FXML private RadioButton modTypePM;
    @FXML private Slider carrierFreqSlider;
    @FXML private Label carrierFreqLabel;
    @FXML private Slider modulatingFreqSlider;
    @FXML private Label modulatingFreqLabel;
    @FXML private Slider modParamSlider;
    @FXML private Label modParamLabel;
    @FXML private Slider timebaseSlider;
    @FXML private Label timebaseLabel;
    @FXML private Slider voltSlider;
    @FXML private Label voltLabel;

    private GraphicsContext gc;
    private double[] signalData;
    private final int samples = 800;
    private double time = 0;
    private double lastCarrierFreq = -1;
    private double lastModulatingFreq = -1;
    private double lastModParam = -1;
    private double lastTimebase = -1;
    private double lastVolt = -1;
    private String currentModType = "AM";
    private String lastModType = "";

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        signalData = new double[samples];

        // Set up toggle group for modulation type radio buttons
        ToggleGroup modTypeGroup = new ToggleGroup();
        modTypeAM.setToggleGroup(modTypeGroup);
        modTypeFM.setToggleGroup(modTypeGroup);
        modTypePM.setToggleGroup(modTypeGroup);
        modTypeAM.setUserData("AM");
        modTypeFM.setUserData("FM");
        modTypePM.setUserData("PM");
        modTypeAM.setSelected(true); // Default to AM

        // Add tooltips
        modTypeAM.setTooltip(new Tooltip("Select Amplitude Modulation"));
        modTypeFM.setTooltip(new Tooltip("Select Frequency Modulation"));
        modTypePM.setTooltip(new Tooltip("Select Phase Modulation"));
        carrierFreqSlider.setTooltip(new Tooltip("Adjust carrier frequency (Hz)"));
        modulatingFreqSlider.setTooltip(new Tooltip("Adjust modulating frequency (Hz)"));
        modParamSlider.setTooltip(new Tooltip("Adjust modulation index or deviation"));
        timebaseSlider.setTooltip(new Tooltip("Adjust horizontal time scale (seconds/div)"));
        voltSlider.setTooltip(new Tooltip("Adjust vertical scale (volts/div)"));

        // Update labels with slider values
        updateSliderLabels();
        carrierFreqSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        modulatingFreqSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        modParamSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        timebaseSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());
        voltSlider.valueProperty().addListener((obs, old, val) -> updateSliderLabels());

        // Modulation type selection listener
        modTypeGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle != null) {
                String modType = (String) toggle.getUserData();
                updateModType(modType);
            }
        });

        // Initialize modulation parameters
        updateModType("AM");

        // Start animation loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateWaveform();
                drawWaveform();
                time += 0.016; // Approx 60 FPS
            }
        }.start();
    }

    private void updateModType(String modType) {
        currentModType = modType;
        switch (modType) {
            case "AM":
                modParamSlider.setMin(0);
                modParamSlider.setMax(1);
                modParamSlider.setValue(0.5);
                modParamLabel.setText("Index: 0.5");
                break;
            case "FM":
                modParamSlider.setMin(0);
                modParamSlider.setMax(500);
                modParamSlider.setValue(50);
                modParamLabel.setText("Deviation: 50 Hz");
                break;
            case "PM":
                modParamSlider.setMin(0);
                modParamSlider.setMax(Math.PI);
                modParamSlider.setValue(Math.PI / 2);
                modParamLabel.setText("Phase Dev: 1.57 rad");
                break;
        }
        lastModType = ""; // Force waveform update
    }

    private void updateSliderLabels() {
        carrierFreqLabel.setText(String.format("Carrier: %.0f Hz", carrierFreqSlider.getValue()));
        modulatingFreqLabel.setText(String.format("Modulating: %.1f Hz", modulatingFreqSlider.getValue()));
        switch (currentModType) {
            case "AM":
                modParamLabel.setText(String.format("Index: %.2f", modParamSlider.getValue()));
                break;
            case "FM":
                modParamLabel.setText(String.format("Deviation: %.0f Hz", modParamSlider.getValue()));
                break;
            case "PM":
                modParamLabel.setText(String.format("Phase Dev: %.2f rad", modParamSlider.getValue()));
                break;
        }
        timebaseLabel.setText(String.format("Timebase: %.3f s/div", timebaseSlider.getValue()));
        voltLabel.setText(String.format("Voltage: %.2f V/div", voltSlider.getValue()));
    }

    private void updateWaveform() {
        double carrierFreq = carrierFreqSlider.getValue();
        double modulatingFreq = modulatingFreqSlider.getValue();
        double modParam = modParamSlider.getValue();
        double timebase = timebaseSlider.getValue();
        String modType = currentModType;

        // Only regenerate waveform if parameters have changed
        if (carrierFreq != lastCarrierFreq || modulatingFreq != lastModulatingFreq ||
                modParam != lastModParam || timebase != lastTimebase || !modType.equals(lastModType)) {
            double totalTime = timebase * 10; // 10 divisions
            double dt = totalTime / samples;

            for (int i = 0; i < samples; i++) {
                double t = i * dt + time;
                double carrier = Math.sin(2 * Math.PI * carrierFreq * t);
                double modulator = Math.sin(2 * Math.PI * modulatingFreq * t);

                switch (modType) {
                    case "AM":
                        // AM: s(t) = (1 + m * cos(2π fmt)) * cos(2π fct)
                        signalData[i] = (1 + modParam * modulator) * carrier;
                        break;
                    case "FM":
                        // FM: s(t) = cos(2π fct + β sin(2π fmt)), β = Δf/fm
                        double beta = modParam / modulatingFreq;
                        signalData[i] = Math.cos(2 * Math.PI * carrierFreq * t + beta * modulator);
                        break;
                    case "PM":
                        // PM: s(t) = cos(2π fct + kp * sin(2π fmt))
                        signalData[i] = Math.cos(2 * Math.PI * carrierFreq * t + modParam * modulator);
                        break;
                }
            }

            lastCarrierFreq = carrierFreq;
            lastModulatingFreq = modulatingFreq;
            lastModParam = modParam;
            lastTimebase = timebase;
            lastModType = modType;
        }
    }

    private void drawWaveform() {
        // Set textured background with subtle gradient
        Stop[] stops = new Stop[] {
                new Stop(0, Color.rgb(20, 20, 30)),
                new Stop(1, Color.rgb(40, 40, 50))
        };
        LinearGradient bgGradient = new LinearGradient(0, 0, 0, canvas.getHeight(), false, CycleMethod.NO_CYCLE, stops);
        gc.setFill(bgGradient);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw grid
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

        // Draw modulated signal
        double height = canvas.getHeight() / 2;
        double volt = voltSlider.getValue();

        Glow glow = new Glow(0.7);
        GaussianBlur blur = new GaussianBlur(1.0);
        glow.setInput(blur);

        gc.setStroke(Color.rgb(255, 255, 100, 0.9)); // Yellow for modulated signal
        gc.setLineWidth(2.2);
        gc.setEffect(glow);
        gc.beginPath();
        for (int i = 0; i < samples; i++) {
            double x = i * canvas.getWidth() / samples;
            double y = height - (signalData[i] * height / volt);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();
        gc.setEffect(null);
    }
}