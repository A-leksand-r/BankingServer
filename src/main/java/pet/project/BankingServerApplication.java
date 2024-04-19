package pet.project;

import pet.project.controllers.ClientController;
import pet.project.DataBase.DataBaseConnector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BankingServerApplication {
    public static void main(String[] args) {
        startServer();
    }

    public static void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Server is listening on port 8080");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientController.handleClient(socket);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }
}