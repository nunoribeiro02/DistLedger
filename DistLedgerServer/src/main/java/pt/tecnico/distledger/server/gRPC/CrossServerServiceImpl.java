package pt.tecnico.distledger.server.gRPC;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;


import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;

import pt.tecnico.distledger.server.domain.Timestamp;

import java.util.ArrayList;
import java.util.List;


import static pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType.*;
import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.StatusRuntimeException;

import pt.tecnico.distledger.server.domain.exception.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exception.AccountNotFoundException;
import pt.tecnico.distledger.server.domain.exception.AccountBalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exception.InsufficientFundsException;
import pt.tecnico.distledger.server.domain.exception.InvalidTransferQuantityException;
import pt.tecnico.distledger.server.domain.exception.InactiveServerException;
import pt.tecnico.distledger.server.domain.exception.NoPermissionsException;
import pt.tecnico.distledger.server.domain.exception.InvalidValueToBalanceException;
import pt.tecnico.distledger.server.domain.exception.NoServerToPropagateException;

import java.util.logging.Logger;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(CrossServerServiceImpl.class.getName());
	private ServerState server;

	// Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public CrossServerServiceImpl (ServerState server) {
        this.server=server;
    }

    @Override
	public void propagateState(CrossServerDistLedger.PropagateStateRequest request, StreamObserver<CrossServerDistLedger.PropagateStateResponse > responseObserver) {
		/* get ledgerList from propagateStateRequest */
        List<DistLedgerCommonDefinitions.Operation> ledgerList = request.getState().getLedgerList();
        int size_ledger = ledgerList.size();
		List<Operation> state = new ArrayList<>();
		for (int i=0; i< size_ledger; i++) {
			DistLedgerCommonDefinitions.Operation operation = ledgerList.get(i);
			Timestamp prev = new Timestamp(0);
			prev.setTS(operation.getPrevTSList());
			Timestamp ts = new Timestamp(0);
			ts.setTS(operation.getTSList());
			Operation op = null;
            if (operation.getTypeValue() == OperationType.OP_CREATE_ACCOUNT_VALUE) {
				op = new CreateOp(operation.getUserId(),ts,prev);
                   
			}
			if (operation.getTypeValue() == OperationType.OP_TRANSFER_TO_VALUE ) {
				op = new TransferOp(operation.getUserId(), operation.getDestUserId(), 
							operation.getAmount(), ts, prev);
        	}
			if (op!=null) {
				state.add(op);
			}   
		}

		List<Integer> responseTS = server.acceptGossip(state, request.getReplicaTSList());

        CrossServerDistLedger.PropagateStateResponse response = CrossServerDistLedger.PropagateStateResponse.newBuilder().addAllResponseTS(responseTS).build();
		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
	}
}


