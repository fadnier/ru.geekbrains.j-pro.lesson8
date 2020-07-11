package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clientList;
    @FXML
    public TextField nickNameArea;

    Stage regStage;
    Stage authStage;

    LoginFormController loginFormController;
    RegController regController;

    private static final Logger logger = Logger.getLogger(Controller.class.getName());

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    private String IP_ADDRESS = "localhost";
    final int PORT = 8189;

    private boolean authenticated;
    private String nick;
    private String login;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        msgPanel.setManaged(authenticated);
        msgPanel.setVisible(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        nickNameArea.setVisible(authenticated);
        nickNameArea.setManaged(authenticated);
        if (!authenticated) {
            nick = "";
        }
        textArea.clear();
        setTitle(nick);
        nickNameArea.setText(nick);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);

        regStage = createRegWindow();
        authStage = createAuthWindow();

        authStage.show();

        Platform.runLater(() -> {
            Stage stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    logger.info("bue");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        logger.fine(str);

                        if (str.equals("/end")) {
                            throw new RuntimeException();
                        }

                        if (str.startsWith("/authok ")) {
                            nick = str.split(" ")[1];
                            setAuthenticated(true);
                            //HistoryChat.load(login, textArea);
                            Platform.runLater(()->{
                                authStage.hide();
                                Main.getPrimaryStage().show();
                            });

                            break;
                        }
                        if (str.startsWith("/regok ")) {
                            String nickname = str.split(" ")[1];
                            Platform.runLater(()->{
                                regStage.hide();
                                loginFormController.setLoginField(nickname);
                            });
                        }
                        if (str.startsWith("/regfalse ")) {
                            Platform.runLater(()-> regController.setAnswerServer(str.split(" ",2)[1]));
                        }
                        if (str.startsWith("/authfalse ")) {
                            Platform.runLater(()-> loginFormController.setAnswerServer(str.split(" ",2)[1]));
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }

                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split(" ");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }

                        } else {
                            textArea.appendText(str + "\n");
                            HistoryChat.save(login,str+"\n");
                        }
                    }
                }catch (RuntimeException e){
                    logger.warning("Ошибка ");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    logger.info("мы отключились");
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(String login, String password, String host) {
        this.IP_ADDRESS = host;

        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            logger.info("/auth " + login + " " + password);
            out.writeUTF("/auth " + login + " " + password);
            this.login = login;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nick) {
        Platform.runLater(() -> {
            ((Stage) textField.getScene().getWindow()).setTitle("Super chat " + nick);
        });

    }

    public void clickClientList(MouseEvent mouseEvent) {
        logger.info(clientList.getSelectionModel().getSelectedItem());
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText("/w " + receiver + " ");
    }

    private Stage createRegWindow() {
        Stage stage = null;

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            stage = new Stage();
            stage.setTitle("Registration ");
            stage.setScene(new Scene(root, 350, 200));
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.controller = this;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stage;
    }

    private Stage createAuthWindow() {
        Stage stage = null;

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = fxmlLoader.load();

            stage = new Stage();
            stage.setTitle("Авторизация");
            stage.setScene(new Scene(root, 350, 200));
            stage.initStyle(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);

            loginFormController = fxmlLoader.getController();
            loginFormController.controller = this;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stage;
    }

    public void showRegWindow() {
        regStage.show();
    }

    public void tryRegistration(String login, String password ,String nickname){
        String msg = String.format("/reg %s %s %s", login, password ,nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendChangeNickName(ActionEvent actionEvent) {
        String msg = String.format("/changename %s", nickNameArea.getText().trim());

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
