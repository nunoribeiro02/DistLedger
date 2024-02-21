package pt.tecnico.distledger.server.domain.operation;
import pt.tecnico.distledger.server.domain.Timestamp;

public class Operation implements Comparable<Operation> {
    private String account;
    private Timestamp ts;
    private Timestamp prev;

    public Operation(String fromAccount, Timestamp ts, Timestamp prev) {
        this.account = fromAccount;
        this.ts = ts;
        this.prev = prev;
    }

    // Getters and setters
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Timestamp getTS() {
        return ts;
    }

    public void setTS(Timestamp ts) {
        this.ts = ts;
    }

     public Timestamp getPrev() {
        return prev;
    }

    public void setPrev(Timestamp prev) {
        this.prev = prev;
    }

    // Compare to
    @Override
    public int compareTo(Operation other) {
        return (this.getTS()).compareTo(other.getTS());
    }
}
