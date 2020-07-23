package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nickField;
    @FXML
    public Label answerServer;
    Controller controller;

    public void setAnswerServer(String anw) {
        answerServer.setText(anw);
    }

    public void clickOk() {
        controller.tryRegistration(loginField.getText().trim(), passwordField.getText().trim(), nickField.getText().trim());
    }
}
