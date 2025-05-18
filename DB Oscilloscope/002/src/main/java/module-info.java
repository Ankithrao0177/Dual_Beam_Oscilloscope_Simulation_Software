module db.oscilloscope.app.dboscilloscope {
    requires javafx.controls;
    requires javafx.fxml;


    opens db.oscilloscope.app to javafx.fxml;
    exports db.oscilloscope.app;
}