package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.print.attribute.HashAttributeSet;
import java.io.IOException;

import pt.tecnico.distledger.namingserver.grpc.Server_NamingServerServiceImpl;
import pt.tecnico.distledger.namingserver.grpc.NamingServerServiceImpl;

public class NamingServer {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);


    private static final int PORT = 5001;

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        // receive and print arguments
        debug(String.format("Received %d arguments", args.length));
        for (int i = 0; i < args.length; i++) {
            debug(String.format("arg[%d] = %s\n", i, args[i]));
        }

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", NamingServer.class.getName());
            return;
        }

        final int port = PORT;
        
        NamingServerState state = new NamingServerState();
        final BindableService serverService = new Server_NamingServerServiceImpl(state); 
        final BindableService lookupService = new NamingServerServiceImpl(state); 
        // Create a new server to listen on port Server server =
        Server server  = ServerBuilder.forPort(port).addService(serverService).addService(lookupService).build();
         
        // Start the server server.start(); 
        server.start();
        // Server threads are running in the background.
        debug(String.format("NamingServer Status: Started Successfully\n"));
         
        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();  

    }

}
