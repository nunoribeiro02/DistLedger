package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.List;

import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;


public class LedgerRecord implements Comparable<LedgerRecord>{

    private boolean stable = false;

    private Operation operation;

    public LedgerRecord(Operation operation) {
        this.operation = operation;
    }

    // Getters and Setters
    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public void setStable() {
        stable = true;
    }

    public boolean isStable() {
        return stable;
    }

    @Override
    public int compareTo(LedgerRecord other) {
        return (this.getOperation()).compareTo(other.getOperation());
    }
}
