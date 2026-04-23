module pos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    opens pos to javafx.fxml;
    exports pos;
}
