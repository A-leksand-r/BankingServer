package pet.project.controllers;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pet.project.DTO.ClientDTO;
import pet.project.DTO.RequestDTO;
import pet.project.DTO.ResponseDTO;
import pet.project.DataBase.DataBaseConnector;
import pet.project.utils.JwtService;
import pet.project.utils.LogController;

import javax.xml.crypto.Data;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientController {

    private static final SimpleDateFormat timeRequest = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line = reader.readLine();
            if (line == null) return;

            RequestDTO request = new RequestDTO();

            String[] requestLine = line.split(" ");
            request.setMethod(requestLine[0]);
            request.setUrl(requestLine[1]);

            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    request.addHeader(line.split(": ")[0], line.split(": ")[1]);
                }
                if (line.startsWith("Authorization:")) {
                    request.addHeader(line.split(": ")[0], line.split(": ")[1]);
                }
            }
            if (request.getHeaders().containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(request.getHeaders().get("Content-Length"));
                char[] buffer = new char[contentLength];
                reader.read(buffer, 0, contentLength);
                ClientDTO client = new ObjectMapper().readValue(new String(buffer), ClientDTO.class);
                request.setBody(client);
            }

            String response = processRequest(request.getMethod(), request.getUrl(), request);
            if (!response.isEmpty()) {
                writer.write(response);
                writer.flush();
            }
            else {
                LogController.log("Неверный запрос", timeRequest);
                writer.write(new ResponseDTO("HTTP/1.1 400 Bad request", new HashMap<>(){{
                    put("Content-Length", String.valueOf("Неправильный запрос".getBytes().length));
                }}, "Неправильный запрос").toString());
                writer.flush();
            }
            socket.close();
        }  catch (JsonEOFException exception) {
            exception.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static String processRequest(String method, String path, RequestDTO requestDTO) {
        switch (path) {
            case "/signup":
                return handleSignUp(requestDTO);
            case "/signin":
                return handleSignIn(requestDTO);
            case "/money":
                if (method.equals("POST")) {
                    return handleTransfer(requestDTO);
                } else if (method.equals("GET")) {
                    return handleBalance(requestDTO);
                }
            default:
                return "";
        }
    }

    private static String handleSignUp(RequestDTO requestDTO) {
        String login = requestDTO.getBody().getLogin();
        String password = requestDTO.getBody().getPassword();

        if (DataBaseConnector.connectToDataBase()) {
            if (DataBaseConnector.existsByLogin(login)) {
                LogController.log("Пользователь с логином " + login + " уже существует.", timeRequest);
                return new ResponseDTO("HTTP/1.1 409 Already exists", new HashMap<>() {{
                    put("Content-Length", String.valueOf(("Пользователь с логином " + login + " уже существует").getBytes().length));
                }}, "Пользователь с логином " + login + " уже существует").toString();
            }
            if (DataBaseConnector.saveClient(login, password, timeRequest)) {
                return new ResponseDTO("HTTP/1.1 200 OK", new HashMap<>() {{
                    put("Content-Length", String.valueOf(("Регистрация клиента с логином " + login + " успешно завершена").getBytes().length));
                }}, "Регистрация клиента с логином " + login + " успешно завершена").toString();
            }
            DataBaseConnector.closeConnectToDataBase();
        }
        return "";
    }

    private static String handleSignIn(RequestDTO requestDTO) {
        String login = requestDTO.getBody().getLogin();
        String password = requestDTO.getBody().getPassword();

        if (DataBaseConnector.connectToDataBase()) {
            try (ResultSet client = DataBaseConnector.clientByLogin(login)) {
                if (client != null) {
                    client.next();
                    if (login.equals(client.getString("login")) && password.equals(client.getString("password"))) {
                        String jwt = JwtService.generateToken(login);
                        DataBaseConnector.updateJwt(login, jwt);
                        LogController.log("Авторизация пользователя с логином " + login + " прошла успешно", timeRequest);
                        return new ResponseDTO("HTTP/1.1 200 OK", new HashMap<>() {{
                            put("Authorization", " Bearer " + jwt);
                            put("Content-Length", String.valueOf("Авторизация прошла успешно".getBytes().length));
                        }}, "Авторизация прошла успешно").toString();
                    }
                }
                else {
                    LogController.log(login + ": неверные учетные данные", timeRequest);
                    return new ResponseDTO("HTTP/1.1 401 Unauthorized", new HashMap<>() {{
                        put("Content-Length", String.valueOf("Неверные учетные данные".getBytes().length));
                    }}, "Неверные учетные данные").toString();
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
            DataBaseConnector.closeConnectToDataBase();
        }
        return "";
    }

    private static String handleTransfer(RequestDTO requestDTO) {
        String jwt;
        if (!requestDTO.getHeaders().containsKey("Authorization")) {
            return "";
        }
        jwt = requestDTO.getHeaders().get("Authorization").split("Bearer ")[1];
        Object[] loginAndBalance = getBalance(jwt);
        BigDecimal balanceSender = (BigDecimal) loginAndBalance[0];
        if (Objects.equals(balanceSender, BigDecimal.valueOf(403))){
            LogController.log("Попытка перевода средств без авторизации", timeRequest);
            return new ResponseDTO("HTTP/1.1 403 Forbidden", new HashMap<>() {{
                put("Content-Length", String.valueOf("Доступ не авторизированным пользователям запрещен".getBytes().length));
            }}, "Доступ не авторизированным пользователям запрещен").toString();
        }

        if (Objects.equals(balanceSender, BigDecimal.valueOf(-1)))
            return "";
        BigDecimal amount = requestDTO.getBody().getAmount();
        String loginRecipient = requestDTO.getBody().getTo();
        String loginSender = (String) loginAndBalance[1];
        BigDecimal balanceRecipient = DataBaseConnector.getBalanceByLogin(loginRecipient);
        if (amount != null && amount.compareTo(BigDecimal.valueOf(0)) > 0 && loginRecipient != null && loginSender != null && !loginSender.equals(loginRecipient)) {
            if (balanceSender.compareTo(amount) > -1) {
                boolean resultSenderUpdateBalance = false;
                boolean resultRecipientUpdateBalance = false;
                while (!resultSenderUpdateBalance) {
                    resultSenderUpdateBalance = DataBaseConnector.updateBalance(loginSender, balanceSender.subtract(amount));
                }
                while (!resultRecipientUpdateBalance) {
                    resultRecipientUpdateBalance = DataBaseConnector.updateBalance(loginRecipient, balanceRecipient.add(amount));
                }
                LogController.log("Пользователь " + loginSender + " перевел " + amount + "$ " + loginRecipient, timeRequest);
                return new ResponseDTO("HTTP/1.1 200 OK", new HashMap<>() {{
                    put("Content-Length", String.valueOf("Перевод осуществлён".getBytes().length));
                }}, "Перевод осуществлён").toString();
            }
            else {
                LogController.log(loginSender + ": недостаточно средств", timeRequest);
                return new ResponseDTO("HTTP/1.1 403 Forbidden", new HashMap<>() {{
                    put("Content-Length", String.valueOf("Недостаточно средств".getBytes().length));
                }}, "Недостаточно средств").toString();
            }
        }
        return "";
    }

    private static String handleBalance(RequestDTO requestDTO) {
        String jwt = requestDTO.getHeaders().get("Authorization").split("Bearer ")[1];
        Object[] loginAndBalance = getBalance(jwt);
        BigDecimal balance = (BigDecimal) loginAndBalance[0];
        if (Objects.equals(balance, BigDecimal.valueOf(403))) {
            LogController.log("Попытка доступа к балансу без авторизации", timeRequest);
            return new ResponseDTO("HTTP/1.1 403 Forbidden", new HashMap<>() {{
                put("Content-Length", String.valueOf("Доступ не авторизированным пользователям запрещен".getBytes().length));
            }}, "Доступ не авторизированным пользователям запрещен").toString();
        }
        if (Objects.equals(balance, BigDecimal.valueOf(-1)))
            return "";
        String login = (String) loginAndBalance[1];
        LogController.log(login + ": баланс " + balance, timeRequest);
        return new ResponseDTO("HTTP/1.1 200 OK", new HashMap<>() {{
            put("Content-Length", String.valueOf(("Ваш баланс: " + balance + "$").getBytes().length));
        }}, "Ваш баланс: " + balance + "$").toString();
    }

    public static Object[] getBalance(String jwt) {
        if (DataBaseConnector.connectToDataBase()) {
            try {
                String login = DataBaseConnector.getLoginByToken(jwt);
                if (JwtService.isTokenValid(jwt, login)) {
                    ResultSet client = DataBaseConnector.clientByLogin(login);
                    if (client != null) {
                        client.next();
                        BigDecimal balance = client.getBigDecimal("balance");
                        client.close();
                        return new Object[]{balance, login};
                    }
                }
                return new Object[]{403, login};
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
            DataBaseConnector.closeConnectToDataBase();
        }
        return new Object[]{-1};
    }
}
