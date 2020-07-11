package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.logging.*;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ExecutorService service;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private String nick;
    private String login;
    private int id;

    public ClientHandler(Server server, Socket socket, ExecutorService service) {
        this.server = server;
        this.socket = socket;
        this.service = service;
        try {
            logger.addHandler(server.getFileHandler());
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            service.execute(() -> {
                try {
                    //Если в течении 5 секунд не будет сообщений по сокету то вызовится исключение
                    socket.setSoTimeout(3000);

                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        logger.fine(str);
                        if (str.startsWith("/reg ")) {
                            String[] token = str.split(" ");

                            if (token.length < 4) {
                                continue;
                            }

                            boolean succeed = server
                                    .getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (succeed) {
                                sendMsg("Регистрация прошла успешно");
                            } else {
                                sendMsg("Регистрация  не удалась. \n" +
                                        "Возможно логин уже занят, или данные содержат пробел");
                            }
                        }

                        if (str.startsWith("/auth ")) {
                            String[] token = str.split(" ");

                            if (token.length < 3) {
                                continue;
                            }

                            String[] authParam = server.getAuthService()
                                    .getAuthByLoginAndPassword(token[1], token[2]);

                            login = token[1];

                            if (authParam != null) {
                                String newNick = authParam[1];

                                if (!server.isLoginAuthorized(login)) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    id = Integer.parseInt(authParam[2]);
                                    server.subscribe(this);
                                    logger.info("Клиент: " + nick + " подключился"+ socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже прошли аутентификацию");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        logger.fine(str);
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                break;
                            }
                            if (str.startsWith("/changename ")) {
                                String[] token = str.split(" ", 2);

                                if (token.length < 2) {
                                    continue;
                                }

                                if(server.getAuthService().changeNickname(id,token[1])) {
                                    this.nick = token[1];
                                    server.broadcastClientList();
                                } else {
                                    sendMsg("Не удалось изменить имя, возможно уже используется");
                                }
                            }
                            if (str.startsWith("/w ")) {
                                String[] token = str.split(" ", 3);

                                if (token.length < 3) {
                                    continue;
                                }

                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else {
                            server.broadcastMsg(nick, str, this);
                        }
                    }
                }catch (SocketTimeoutException e){
                    sendMsg("/end");
                }
                ///////
                catch (IOException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    logger.info("Клиент отключился");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                    }
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            logger.fine(id+" "+msg);
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public String getLogin() {
        return login;
    }

    public int getId() {
        return id;
    }
}
