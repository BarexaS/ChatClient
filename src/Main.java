import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;


//Читаем общие сообщения
class GetThread extends Thread {
    private int n;
    private int k;
    private String user;
    private String currentRoom;

    public GetThread(String user, String currentRoom) {
        this.user = user;
        this.currentRoom = currentRoom;
    }

    public void changeRoom(String roomName) {
        this.currentRoom = roomName;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                gettingPubMessage(currentRoom);
                Thread.sleep(100);
                gettingPrvtMessage(user);
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }

    private void gettingPrvtMessage(String user) throws IOException {
        URL url = new URL("http://localhost:8080/privateMess?from=" + k + "&user=" + user);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        try (InputStream is = http.getInputStream()) {
            int sz = is.available();
            if (sz > 0) {
                byte[] buf = new byte[is.available()];
                is.read(buf);
                Gson gson = new GsonBuilder().create();
                Message[] list = gson.fromJson(new String(buf), Message[].class);
                for (Message m : list) {
                    System.out.println("WHISPER -> " + m);
                    k++;
                }
            }
        }
        http.disconnect();
    }

    private void gettingPubMessage(String roomName) throws IOException {
        URL url = new URL("http://localhost:8080/get?from=" + n + "&roomName=" + roomName);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        try (InputStream is = http.getInputStream()) {

            int sz = is.available();
            if (sz > 0) {
                byte[] buf = new byte[is.available()];
                is.read(buf);

                Gson gson = new GsonBuilder().create();
                Message[] list = gson.fromJson(new String(buf), Message[].class);

                for (Message m : list) {
                    System.out.println(m);
                    n++;
                }
            }
        }
        http.disconnect();
    }
}


public class Main {
    static boolean session = false;
    static Client clt = new Client(null, null);
    static String roomName = "public";
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            loginProcess(scanner, clt);
            exitFromChat(clt.getLogin());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void chatProcess(Scanner scanner, Client clt, String roomName, GetThread th) throws IOException {
        System.out.println("Chat commands : ");
        System.out.println("To check somebody status - type : /status login");
        System.out.println("To wisper somebody - type : /w nickname message");
        System.out.println("To see all chat members - type : /list");
        System.out.println("To change chat-room - type : /switch RoomName");
        System.out.println("To exit chat - type : /exit");

        while (true) {
            String text = scanner.nextLine();
            if (text.isEmpty())
                break;
            if (text.charAt(0) == '/') {
                if (text.substring(0, 2).equalsIgnoreCase("/w")) {
                    sendPrivateMessage(clt, text);
                    continue;
                }
                if (text.substring(0, 5).equalsIgnoreCase("/list")) {
                    getClientList();
                    continue;
                }
                if (text.substring(0, 5).equalsIgnoreCase("/exit")) {
                    exitFromChat(clt.getLogin());
                    break;
                }
                if (text.substring(0, 7).equalsIgnoreCase("/switch")) {
                    if (changeRoom(text.split(" ")[1])) {
                        roomName = text.split(" ")[1];
                        th.changeRoom(roomName);
                        System.out.println("Room created!");
                    } else {
                        roomName = text.split(" ")[1];
                        th.changeRoom(roomName);
                        System.out.println("Nice to see u again!");
                    }
                    continue;
                }
                if (text.substring(0, 7).equalsIgnoreCase("/status")) {
                    String nickname = text.split(" ")[1];
                    boolean result = checkStatus(nickname);
                    if (result) {
                        System.out.println(nickname + " - online");
                    } else {
                        System.out.println(nickname + " - offline");
                    }
                    continue;
                }

            }
            sendPublicMessage(clt, text, roomName);
        }
    }

    private static boolean checkStatus(String nickname) throws IOException {
        URL url = new URL("http://localhost:8080/loginIn?login=" + nickname);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        String result;
        try (InputStream is = http.getInputStream()) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            result = new String(buf);
        }
        if (result.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    private static void exitFromChat(String login) throws IOException {
        URL url = new URL("http://localhost:8080/exit?login=" + login);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.connect();
        session = false;
        http.disconnect();
    }

    private static boolean changeRoom(String roomName) throws IOException {
        URL url = new URL("http://localhost:8080/changeRoom?roomName=" + roomName);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        String result;
        try (InputStream is = http.getInputStream()) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            result = new String(buf);
        }
        http.disconnect();
        if (result.equals("Room created!")) {
            return true;
        } else {
            return false;
        }

    }


    private static void sendPrivateMessage(Client clt, String text) throws IOException {
        Message m = new Message();
        String messFor = text.split(" ")[1];
        m.setTo(messFor);
        m.setText(text.substring(text.indexOf(messFor) + messFor.length()));
        m.setFrom(clt.getLogin());
        try {
            int res = m.send("http://localhost:8080/privateMess", session);
            if (res != 200) {
                System.out.println("HTTP error: " + res);
                loginProcess(scanner, clt);
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void sendPublicMessage(Client clt, String text, String roomName) {
        Message m = new Message();
        m.setText(text);
        m.setFrom(clt.getLogin());
        m.setRoomName(roomName);

        try {
            int res = m.send("http://localhost:8080/add", session);

            if (res != 200) {
                System.out.println("HTTP error: " + res);
                loginProcess(scanner, clt);
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void getClientList() {
        try {
            URL url = new URL("http://localhost:8080/getList");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Cookie", "Session=" + session);
            try (InputStream is = http.getInputStream()) {
                byte[] buf = new byte[is.available()];
                is.read(buf);
                String logResult = new String(buf);
                if ("Please login".equalsIgnoreCase(logResult)){
                    loginProcess(scanner,clt);
                } else {
                    System.out.println(logResult);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }

    private static void loginProcess(Scanner scanner, Client clt) throws IOException {
        while (session == false) {
            System.out.println("Enter login: ");
            String login = scanner.nextLine();

            System.out.println("Enter password: ");
            String pass = scanner.nextLine();

//                  Отсылаем логин и пароль для проверки
            HttpURLConnection http = sendLogAndPass(login, pass);

            String headerName = null;

//                Вычитываем результат логина
            try (InputStream is = http.getInputStream()) {
                byte[] buf = new byte[is.available()];
                is.read(buf);

                for (int i = 1; (headerName = http.getHeaderFieldKey(i)) != null; i++) {
                    if (headerName.equals("Set-Cookie")) {
                        String cookie = http.getHeaderField(i);
                        if (cookie.split("=")[1].equals("true")) {
                            session = true;
                        }
                    }
                }

                String logResult = new String(buf);
                System.out.println(logResult.split("\n")[0]);
                switch (logResult.split("\n")[0]) {
                    case "Welcome!!!": {
                        clt = Client.fromJSON(logResult.split("\n")[1]);
                        break;
                    }
                    case "Wrong pass": {
                        continue;
                    }
                    case "No such user": {
                        System.out.println("Want to sign up with this login : " + login + ", and password : " + pass + "?");
                        System.out.println("Input Y/N");
                        while (true) {
                            String ans = scanner.nextLine();
                            if ((ans.equalsIgnoreCase("y")) || (ans.equalsIgnoreCase("n"))) {
                                clt = regClient(login, pass);
                                break;
                            } else {
                                System.out.println("Wrong input");
                            }
                        }
                        break;
                    }
                }
            }
            http.disconnect();
        }
        GetThread th = new GetThread(clt.getLogin(), roomName);
        th.setDaemon(true);
        th.start();

        chatProcess(scanner, clt, roomName, th);
    }


    private static Client regClient(String login, String pass) throws IOException {
        Client clt = new Client(login, pass);
        URL url = new URL("http://localhost:8080/RegClient");
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        OutputStream os = http.getOutputStream();
        os.write((clt.toJSON()).getBytes());
        os.flush();
        os.close();

        try (InputStream is = http.getInputStream()) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            String logResult = new String(buf);
            System.out.println(logResult);
        }
        http.disconnect();
        return clt;
    }

    private static HttpURLConnection sendLogAndPass(String login, String pass) throws IOException {
        URL url = new URL("http://localhost:8080/loginIn");
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        OutputStream os = http.getOutputStream();
        os.write((login + "\n" + pass).getBytes());
        os.flush();
        os.close();
        return http;
    }
}
