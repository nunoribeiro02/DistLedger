package pt.tecnico.distledger.server.domain.operation;
import pt.tecnico.distledger.server.domain.Timestamp;

public class DeleteOp extends Operation {

    public DeleteOp(String account, Timestamp ts, Timestamp prev) {
        super(account, ts, prev);
    }

}
