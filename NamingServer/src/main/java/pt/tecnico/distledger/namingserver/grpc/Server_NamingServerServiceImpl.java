package pt.tecnico.distledger.namingserver.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.ServerNamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.ServerNamingServer;
import pt.tecnico.distledger.namingserver.NamingServerState;
import pt.tecnico.distledger.namingserver.exception.AlreadyRegisteredServerException;
import pt.tecnico.distledger.namingserver.exception.ServerNotRegisteredException;
import static io.grpc.Status.INVALID_ARGUMENT;


import io.grpc.stub.StreamObserver;


import java.util.logging.Logger;

public class Server_NamingServerServiceImpl extends ServerNamingServiceGrpc.ServerNamingServiceImplBase{

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

	private static final Logger LOGGER = Logger.getLogger(Server_NamingServerServiceImpl.class.getName());
	private NamingServerState server;

    public Server_NamingServerServiceImpl (NamingServerState server) {
        this.server=server;
    }

    @Override
	public void register(ServerNamingServer.RegisterRequest request, StreamObserver<ServerNamingServer.RegisterResponse> responseObserver){
        
        try{
            server.registerServer(request.getServiceName(), request.getQualifier(), request.getHostPort());
            debug("RegisterRequest -> " + "service: " + request.getServiceName() + " | qualifier: " + request.getQualifier() + " | hostPort: " + request.getHostPort());
            ServerNamingServer.RegisterResponse response = ServerNamingServer.RegisterResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            debug("Response sent and call completed successfully.\n");
        }
        catch (AlreadyRegisteredServerException e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

    @Override
	public void delete(ServerNamingServer.DeleteRequest request, StreamObserver<ServerNamingServer.DeleteResponse> responseObserver) {
        try {
        server.deleteServer(request.getServiceName(),request.getHostPort());
        debug("RegisterRequest -> " + "service: " + request.getServiceName() + " | hostPort: " + request.getHostPort());
		ServerNamingServer.DeleteResponse response = ServerNamingServer.DeleteResponse.newBuilder().build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
        debug("Response sent and call completed successfully.\n");
        }
        catch (ServerNotRegisteredException e) {
			LOGGER.info(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

}