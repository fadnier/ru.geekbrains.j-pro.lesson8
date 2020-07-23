package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginFormController {
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label answerServer;
    @FXML
    public TextField serverIP;
    Controller controller;

    public void showRegWindow() {
        controller.showRegWindow();
    }

    public void setAnswerServer(String anw) {
        answerServer.setText(anw);
    }

    public void setLoginField(String login) {
        loginField.setText(login);
        passwordField.focusTraversableProperty();
    }

    public void setPasswordField(String pass) {
        passwordField.setText(pass);
    }

    public void tryToAuth() {
        controller.tryToAuth(loginField.getText().trim(), passwordField.getText().trim(), serverIP.getText().trim());
        passwordField.clear();
    }
}
