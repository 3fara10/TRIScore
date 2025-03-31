module org.example.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires ProiectJava;
    requires org.apache.logging.log4j.core;
    requires java.logging;
    requires jdk.accessibility;

    opens org.example.frontend to javafx.fxml;
    exports org.example.frontend;
}