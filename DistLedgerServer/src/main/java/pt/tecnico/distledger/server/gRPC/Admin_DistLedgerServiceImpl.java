package pt.tecnico.distledger.server.gRPC;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;

import pt.tecnico.distledger.server.domain.exception.InactiveServerException;

import java.util.ArrayList;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;
import java.util.logging.Logger;

public class Admin_DistLedgerServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

	// Set flag to true using the -Ddebug to print debug messages. 
	private static final Logger LOGGER = Logger.getLogger(User_DistLedgerServiceImpl.class.getName());
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }
	
	private ServerState server;

    public Admin_DistLedgerServiceImpl (ServerState server) {
        this.server=server;
    }

    @Override
	public void activate(AdminDistLedger.ActivateRequest request, StreamObserver<AdminDistLedger.ActivateResponse> responseObserver) {
		//activates the server
        server.activate();

		AdminDistLedger.ActivateResponse response = AdminDistLedger.ActivateResponse.newBuilder().build();

		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
		debug("Response sent and call completed successfully.\n");
	}

    @Override
	public void deactivate(AdminDistLedger.DeactivateRequest request, StreamObserver<AdminDistLedger.DeactivateResponse> responseObserver) {
		// StreamObserver is used to represent the gRPC stream between the server and
		// client in order to send the appropriate responses (or errors, if any occur).

		//deactivates the server
        server.deactivate();

		AdminDistLedger.DeactivateResponse response = AdminDistLedger.DeactivateResponse.newBuilder().build();

		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
		debug("Response sent and call completed successfully.\n");
	}

    @Override
	public void getLedgerState(AdminDistLedger.getLedgerStateRequest request, StreamObserver<AdminDistLedger.getLedgerStateResponse> responseObserver) {
		// StreamObserver is used to represent the gRPC stream between the server and
		// client in order to send the appropriate responses (or errors, if any occur).

		//creates a ledgerBuilder
		DistLedgerCommonDefinitions.LedgerState.Builder ledgerBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();
		
		//gets all operations performed on server
		List<Operation> ledgerList= server.getLedger(); 
		int size_ledger = ledgerList.size();
		DistLedgerCommonDefinitions.Operation operationMessage = null;

		//add all operations to ledger 
		for (int i=0; i<size_ledger;i++) {
			Operation operation = ledgerList.get(i);
			String account = operation.getAccount();
			if (operation instanceof CreateOp) {
				operationMessage = DistLedgerCommonDefinitions.Operation.newBuilder().setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT).setUserId(operation.getAccount()).build();
			}
			if (operation instanceof DeleteOp) {
				operationMessage = DistLedgerCommonDefinitions.Operation.newBuilder().setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT).setUserId(operation.getAccount()).build();
			}
			if (operation instanceof TransferOp) {
				TransferOp transferOp = (TransferOp) operation; 
				operationMessage = DistLedgerCommonDefinitions.Operation.newBuilder().setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).setUserId(transferOp.getAccount()).setDestUserId(transferOp.getDestAccount()).setAmount(transferOp.getAmount()).build();
			}
			ledgerBuilder.addLedger(operationMessage);
		}

		//buil ledger
		DistLedgerCommonDefinitions.LedgerState ledgerState = ledgerBuilder.build();

		//sends ledger
		AdminDistLedger.getLedgerStateResponse response = AdminDistLedger.getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build();

		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
		debug("Response sent and call completed successfully.\n");
	}

	@Override
	public void gossip(AdminDistLedger.GossipRequest request, StreamObserver<AdminDistLedger.GossipResponse> responseObserver) {
		//activates the server

		try {
        server.gossip();
	

		AdminDistLedger.GossipResponse response = AdminDistLedger.GossipResponse.newBuilder().build();

		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
		}
		catch (InactiveServerException e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}