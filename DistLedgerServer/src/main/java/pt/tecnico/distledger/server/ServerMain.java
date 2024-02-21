package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.gRPC.User_DistLedgerServiceImpl;
import pt.tecnico.distledger.server.gRPC.Admin_DistLedgerServiceImpl;
import pt.tecnico.distledger.server.gRPC.CrossServerServiceImpl;

import pt.ulisboa.tecnico.distledger.contract.namingserver.*;

import java.io.IOException;

import pt.tecnico.distledger.server.domain.exception.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exception.AccountNotFoundException;
import pt.tecnico.distledger.server.domain.exception.AccountBalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exception.InsufficientFundsException;
import pt.tecnico.distledger.server.domain.exception.InvalidTransferQuantityException;
import pt.tecnico.distledger.server.domain.exception.InvalidValueToBalanceException;

public class ServerMain {
	// Set flag to true using the -Ddebug to print debug messages.
	private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

	final private static String NAMING_SERVER = "localhost:5001";

	final private static String LOCALHOST = "localhost";

	final private static String SERVICE = "DistLedger";

	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

	public static void delete(ServerNamingServiceGrpc.ServerNamingServiceBlockingStub stub, String host_port) {
		try {
			ServerNamingServer.DeleteRequest request_delete = ServerNamingServer.DeleteRequest.newBuilder()
					.setServiceName(SERVICE).setHostPort(host_port).build();
			debug(String.format("DeleteRequest -> service: " + SERVICE + " | hots_port: "  + host_port));

			ServerNamingServer.DeleteResponse response_delete = stub.delete(request_delete);
			debug(String.format("Response sent and call completed successfully."));

		} catch (StatusRuntimeException e) {
			System.out.println("Notice: Naming Server is unavailable");
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException, AccountAlreadyExistsException,
			AccountNotFoundException, AccountBalanceNotZeroException, InvalidTransferQuantityException,
			InsufficientFundsException, InvalidValueToBalanceException {

		// receive and print arguments
		debug(String.format("Received %d arguments", args.length));
		for (int i = 0; i < args.length; i++) {
			debug(String.format("arg[%d] = %s", i, args[i]));
		}

		// Create a stub for naming server to register and delete the server
		System.out.println("target" + NAMING_SERVER);
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(NAMING_SERVER).usePlaintext().build();
		ServerNamingServiceGrpc.ServerNamingServiceBlockingStub stub = ServerNamingServiceGrpc.newBlockingStub(channel);

		// check arguments
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		final int port = Integer.parseInt(args[0]);
		final String qualifier = args[1];

		// Create a new server state
		ServerState serverState = new ServerState(qualifier);

		// Create User and Admin services with a shared server state
		final BindableService userService = new User_DistLedgerServiceImpl(serverState);
		final BindableService adminService = new Admin_DistLedgerServiceImpl(serverState);
		final BindableService crossService = new CrossServerServiceImpl(serverState);

		String host_port = "localhost:" + port;

		// Create a new server to listen on port
		try {
			Server server = ServerBuilder.forPort(port).addService(userService).addService(adminService)
					.addService(crossService).build();

			// Start the server
			server.start();

			// Register this server on naming server
			ServerNamingServer.RegisterRequest request_register = ServerNamingServer.RegisterRequest.newBuilder()
					.setServiceName(SERVICE).setHostPort(host_port).setQualifier(qualifier).build();
			debug(String.format("RegisterRequest  -> service: " + SERVICE + " | hots_port: "  + host_port + " | qualifier: "  + qualifier));

			ServerNamingServer.RegisterResponse response_register = stub.register(request_register);
			debug(String.format("Response sent and call completed successfully."));

			// Server threads are running in the background.
			debug(String.format("Server threads are running in the background\n"));

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					server.shutdown();
					delete(stub, host_port);
				}
			});
			System.out.println("Press enter to shutdown");
			System.in.read();
			System.exit(0);
		} catch (IOException e) {
			System.out.println("[ERROR] - request port already in use");
			System.exit(1);
		} catch (StatusRuntimeException e) {
			System.out.println("[ERROR] - " + e.getStatus().getDescription());
		}

	}
}
