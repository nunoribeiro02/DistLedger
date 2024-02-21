package pt.tecnico.distledger.server.domain.operation;
import pt.tecnico.distledger.server.domain.Timestamp;

public class CreateOp extends Operation {

    public CreateOp(String account, Timestamp ts, Timestamp prev) {
        super(account,ts,prev);
    }

}
