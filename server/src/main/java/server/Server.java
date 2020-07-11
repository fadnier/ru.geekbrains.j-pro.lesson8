package server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;


public class Server {
    private List<ClientHandler> clients;
    private AuthService authService;
    private MessageService messageService;
    private ExecutorService service;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static Handler fileHandler;

    public Server() {
        clients = new Vector<>();
        authService = new SqliteAuthService();
        messageService = new SqliteMessageService();
        ServerSocket server = null;
        Socket socket;


        final int PORT = 8189;

        try {
            fileHandler = new FileHandler("server-log_%g.log",10*1024,5,true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.INFO);
            logger.addHandler(fileHandler);
            server = new ServerSocket(PORT);
            logger.info("Старт сервера");
            SqliteService.connect();
            while (true) {
                socket = server.accept();
                logger.info("Клиент подключился ");
                service = Executors.newFixedThreadPool(4);
                new ClientHandler(this, socket, service);
            }

        } catch (IOException | ClassNotFoundException | SQLException e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                SqliteService.disconnect();
                server.close();
                service.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(String nick, String msg, ClientHandler sender) {
        messageService.addMessage(msg,0,sender.getId());
        for (ClientHandler c : clients) {
            c.sendMsg(nick + ": " + msg);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] private [ %s ] : %s",
                sender.getNick(), receiver, msg);

        for (ClientHandler c : clients) {
            if (c.getNick().equals(receiver)) {
                c.sendMsg(message);
                messageService.addMessage(message,c.getId(),sender.getId());
                if (!sender.getNick().equals(receiver)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }

        sender.sendMsg("not found user: " + receiver);
    }


    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();

        /*
        Map<Integer,String> msg = messageService.getAllMessage(clientHandler.getId());

        for (Map.Entry<Integer,String> entry: msg.entrySet()) {
            clientHandler.sendMsg(entry.getValue());
        }*/
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthorized(String login){
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");

        for (ClientHandler c : clients) {
            sb.append(c.getNick()).append(" ");
        }
        String msg = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

    public Handler getFileHandler() {
        return fileHandler;
    }
}
