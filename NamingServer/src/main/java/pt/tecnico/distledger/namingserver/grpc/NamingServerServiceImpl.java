package pt.tecnico.distledger.namingserver.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.ServerNamingServer;
import pt.tecnico.distledger.namingserver.NamingServerState;

import io.grpc.stub.StreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

public class NamingServerServiceImpl extends NamingServiceGrpc.NamingServiceImplBase{

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

	private static final Logger LOGGER = Logger.getLogger(Server_NamingServerServiceImpl.class.getName());
	private NamingServerState server;

    public NamingServerServiceImpl (NamingServerState server) {
        this.server=server;
    }

    @Override
	public void lookUp(NamingServer.LookUpRequest request, StreamObserver<NamingServer.LookUpResponse> responseObserver) {
        List<String> servers = server.lookUpServer(request.getServiceName(), request.getQualifier());
        debug("LookUpRequest -> " + "service: " + request.getServiceName() + " | qualifier: " + request.getQualifier());
		NamingServer.LookUpResponse response = NamingServer.LookUpResponse.newBuilder().addAllHostPort(servers).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
        debug("Response sent and call completed successfully.\n");
	}

    @Override
	public void searchServers(NamingServer.SearchServersRequest request, StreamObserver<NamingServer.SearchServersResponse> responseObserver) {
        List<String> servers = server.searchServers(request.getServiceName());
        debug("searchServers -> " + "service: " + request.getServiceName());
		NamingServer.SearchServersResponse response = NamingServer.SearchServersResponse.newBuilder().addAllQualifier(servers).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
        debug("Response sent and call completed successfully.\n");
	}
}