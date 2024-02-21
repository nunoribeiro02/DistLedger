package pt.tecnico.distledger.server.gRPC;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;

import pt.tecnico.distledger.server.domain.ServerState;


import static io.grpc.Status.INVALID_ARGUMENT;
import pt.tecnico.distledger.server.domain.exception.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exception.AccountNotFoundException;
import pt.tecnico.distledger.server.domain.exception.AccountBalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exception.InsufficientFundsException;
import pt.tecnico.distledger.server.domain.exception.InvalidTransferQuantityException;
import pt.tecnico.distledger.server.domain.exception.InactiveServerException;
import pt.tecnico.distledger.server.domain.exception.NoPermissionsException;
import pt.tecnico.distledger.server.domain.exception.InvalidValueToBalanceException;
import pt.tecnico.distledger.server.domain.exception.NoServerToPropagateException;
import pt.tecnico.distledger.server.domain.exception.ServerNotUpdatedException;
import pt.tecnico.distledger.server.domain.BalanceResult;


import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;



public class User_DistLedgerServiceImpl extends UserServiceGrpc.UserServiceImplBase {

	
	private static final Logger LOGGER = Logger.getLogger(User_DistLedgerServiceImpl.class.getName());
	private ServerState server;

	// Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public User_DistLedgerServiceImpl (ServerState server) {
        this.server=server;
    }

    @Override
	public void balance(UserDistLedger.BalanceRequest request, StreamObserver<UserDistLedger.BalanceResponse> responseObserver) {
		// StreamObserver is used to represent the gRPC stream between the server and
		// client in order to send the appropriate responses (or errors, if any occur).
		try {
        	BalanceResult result = server.balance(request.getUserId(),request.getPrevTSList(),false);
			UserDistLedger.BalanceResponse response = UserDistLedger.BalanceResponse.newBuilder().setValue(result.getBalance()).addAllValueTS(result.getTS()).build();

			// Send a single response through the stream.
			responseObserver.onNext(response);
			// Notify the client that the operation has been completed.
			responseObserver.onCompleted();
			debug("Response sent and call completed successfully.\n");

		}
		catch (AccountNotFoundException | InactiveServerException | ServerNotUpdatedException e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void createAccount(UserDistLedger.CreateAccountRequest request, StreamObserver<UserDistLedger.CreateAccountResponse> responseObserver) {
		// StreamObserver is used to represent the gRPC stream between the server and
		// client in order to send the appropriate responses (or errors, if any occur).
		
		try {
        List<Integer> operationTS = server.createAccountOperation(request.getUserId(),request.getPrevTSList(),false);
		UserDistLedger.CreateAccountResponse response = UserDistLedger.CreateAccountResponse.newBuilder().addAllTS(operationTS).build();
		// Send a single response through the stream.
		responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
		debug("Response sent and call completed successfully.\n");
		}
		catch (InactiveServerException | NoPermissionsException e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}



    @Override
	public void transferTo(UserDistLedger.TransferToRequest request, StreamObserver<UserDistLedger.TransferToResponse> responseObserver) {
		// StreamObserver is used to represent the gRPC stream between the server and
		// client in order to send the appropriate responses (or errors, if any occur).
		try {
        	List<Integer> operationTS = server.transferOperation(request.getAccountFrom(),request.getAccountTo(),request.getAmount(),false, request.getPrevTSList());
			UserDistLedger.TransferToResponse response = UserDistLedger.TransferToResponse.newBuilder().addAllTS(operationTS).build();
			// Send a single response through the stream.
			responseObserver.onNext(response);
			// Notify the client that the operation has been completed.
			responseObserver.onCompleted();
			debug("Response sent and call completed successfully.\n");
		}
		catch (InvalidTransferQuantityException | InactiveServerException  e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}