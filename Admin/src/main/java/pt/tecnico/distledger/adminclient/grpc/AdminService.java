package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import pt.ulisboa.tecnico.distledger.contract.admin.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.tecnico.distledger.adminclient.grpc.Exceptions.NoServerAvailableException;


public class AdminService {

    /* TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private int trials = 0;

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    final private String targetNamingServer = "localhost:5001";
    final private String serviceName = "DistLedger";
    private NamingServiceGrpc.NamingServiceBlockingStub stubNamingService;

    /*Map with relation qualifier: stub*/
    private Map<String, AdminServiceGrpc.AdminServiceBlockingStub > targets;

    private static final String OK_SUCESS = "OK";

    public AdminService() {
        this.targets = new HashMap<>();
        stubNamingService = createNamingServerStub();
        lookupThread();
    }   
    
    /*lookupThread: looks up periodically  */
    public void lookupThread() {
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                while(true) {
                    Set<String> qualifiers = targets.keySet();
                    for (String qualifier : qualifiers) {
                        try {
                            requestStub(qualifier);
                        } catch (NoServerAvailableException e) {
    
                        }
                    }
                    try {
                        Thread.sleep(10000l);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    // createStub: Create a Stub and save it to the map
    public AdminServiceGrpc.AdminServiceBlockingStub createStub(String target, String qualifier) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);
        
        targets.put(qualifier, stub);

        return stub;
    }

    // createNamingServerStub: Create a Stub for comunication with NamingServer and save it to the map
    public NamingServiceGrpc.NamingServiceBlockingStub createNamingServerStub() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(targetNamingServer).usePlaintext().build();
        NamingServiceGrpc.NamingServiceBlockingStub stubNamingService = NamingServiceGrpc.newBlockingStub(channel);
      
        return stubNamingService;
    }

    /* requestStub: LookUp a Stub and return it 
        Function called by searchStub() */
    public synchronized AdminServiceGrpc.AdminServiceBlockingStub requestStub(String qualifier) throws NoServerAvailableException {
        NamingServer.LookUpRequest request = NamingServer.LookUpRequest.
            newBuilder().setServiceName(serviceName).setQualifier(qualifier).build();

        NamingServer.LookUpResponse response = stubNamingService.lookUp(request);	
        /* Throw Exception in case no server is available */		
        if (response.getHostPortList().isEmpty()) {
            throw new NoServerAvailableException(); 
        }
        String host_port =  response.getHostPortList().get(0);
        AdminServiceGrpc.AdminServiceBlockingStub stub = createStub(host_port, qualifier);

        
        return stub;
    }

    /* searchStub: search thr Map targets for a Stub, requestStub() it isn't there 
        Function called by activate(), deactivate(), getLedgerState() */
    public AdminServiceGrpc.AdminServiceBlockingStub searchStub(String qualifier)  throws NoServerAvailableException{
        // Get Stub from the map, return it if it exists
        AdminServiceGrpc.AdminServiceBlockingStub stub = targets.get(qualifier);
        if (stub != null){
            return stub;
        }
        
        // In case it doesn't exist, make a request to the NamingServer for a host_port
        // make a new stub and return it
        return requestStub(qualifier);
    }

    /* activate: requests to activate server with given qualifier */
    public void activate(String qualifier) {

        AdminServiceGrpc.AdminServiceBlockingStub stub = null;

        // Search Stub
        try {
        stub = searchStub(qualifier); }
        catch (NoServerAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }

        try{
            // Request to activate
            AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
            debug(String.format("Notice: New ActivateRequest made."));			
            AdminDistLedger.ActivateResponse response = stub.activate(request);
            debug(String.format("Response sent and call completed successfully."));
            trials = 0;
            // Print sucess
            System.out.println(OK_SUCESS);
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServerAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry activate
                    ++trials;
                    activate(qualifier);
                }
                // After 2 failed tries, exit
                else {
                    System.err.println("Critical Error: The server is currently unavailable.");
                    System.exit(1);
                }
            }
            else {
                System.out.println("[ERROR] - " + 
                    e.getStatus().getDescription());
            }
        }
    }

    /* deactivate: requests to deactivate server with given qualifier */
    public void deactivate(String qualifier) {
        AdminServiceGrpc.AdminServiceBlockingStub stub = null;
        
        // Search Stub        
        try {
            stub = searchStub(qualifier); 
        }
        catch (NoServerAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }

        try{
            // Request to deactivate
            AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
            debug(String.format("Notice: New DeactivateRequest made.")); 
            AdminDistLedger.DeactivateResponse response = stub.deactivate(request); 
            debug(String.format("Response sent and call completed successfully."));
            trials = 0;
            // Print sucess
            System.out.println(OK_SUCESS);
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServerAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry deactivate
                    ++trials;
                    deactivate(qualifier);
                }
                // After 2 failed tries, exit
                else {
                    System.err.println("Critical Error: The server is currently unavailable.");
                    System.exit(1);
                }
            }
            else {
                System.out.println("[ERROR] - " + 
                    e.getStatus().getDescription());
            }
        }
    }

    /* getLedgerState: requests Ledger State of server with given qualifier
        and prints it, if successful */
    public void getLedgerState(String qualifier) {

        AdminServiceGrpc.AdminServiceBlockingStub stub = null;
        // Search Stub
        try {
            stub = searchStub(qualifier); }
        catch (NoServerAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }

        try{
            // Request the LedgerState
            AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
            debug(String.format("Notice: New getLedgerStateRequest made.")); 
            AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
            debug(String.format("Response sent and call completed successfully."));
            trials = 0;
            // Print sucess
            System.out.println(OK_SUCESS);
            // Print the LedgerState
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServerAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry getLedgerState
                    ++trials;
                    getLedgerState(qualifier);
                }
                // After 2 failed tries, exit
                else {
                    System.err.println("Critical Error: The server is currently unavailable.");
                    System.exit(1);
                }
            }
            else {
                System.out.println("[ERROR] - " + 
                    e.getStatus().getDescription());
            }
        }
    }

    
    
    public void gossip(String qualifier) {

        AdminServiceGrpc.AdminServiceBlockingStub stub = null;

        // Search Stub
        try {
        stub = searchStub(qualifier); }
        catch (NoServerAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }

        try{
            // Request to activate
            AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
            debug(String.format("Notice: New GossipRequest made."));	
            AdminDistLedger.GossipResponse response = stub.gossip(request);
            debug(String.format("Response sent and call completed successfully."));
            trials = 0;
            // Print sucess
            System.out.println(OK_SUCESS);
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServerAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry activate
                    ++trials;
                    gossip(qualifier);
                }
                // After 2 failed tries, exit
                else {
                    System.err.println("Critical Error: The server is currently unavailable.");
                    System.exit(1);
                }
            }
            else {
                System.out.println("[ERROR] - " + 
                    e.getStatus().getDescription());
            }
        }
    }
    

}

