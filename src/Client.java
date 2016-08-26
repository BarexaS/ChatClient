import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Client {
    private boolean status = false;
    private String login;
    private String pass;


    public Client(String login, String pass) {
        this.login = login;
        this.pass = pass;
    }

    public synchronized String toJSON() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    public static Client fromJSON(String s) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(s, Client.class);
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        return "Client{" +
                "status=" + status +
                ", login='" + login + '\'' +
                ", pass='" + pass + '\'' +
                '}';
    }
}
