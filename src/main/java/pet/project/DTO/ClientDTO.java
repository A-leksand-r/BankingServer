package pet.project.DTO;

import java.math.BigDecimal;

public class ClientDTO {
    private String login;
    private String password;
    private String to;
    private BigDecimal amount;

    public String getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
