package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.user.*;
import java.util.Map;
import java.util.HashMap;

import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.UnknownHostException;

import java.util.Set;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.tecnico.distledger.userclient.grpc.Exceptions.NoServersAvailableException;
import pt.tecnico.distledger.userclient.grpc.Exceptions.InvalidTransferQuantityException;


import pt.tecnico.distledger.userclient.domain.Timestamp;

public class UserService{

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private int trials = 0;

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    private static final String SUCCESS = "OK";

    final private String targetNamingServer = "localhost:5001";
    final private String serviceName = "DistLedger";
    final NamingServiceGrpc.NamingServiceBlockingStub stubNamingService;

    /*Map with relation qualifier: stub*/
    private Map<String, UserServiceGrpc.UserServiceBlockingStub > targets;
    private Timestamp timestamp;

    public UserService() {
        this.targets = new HashMap<>();
        this.timestamp = new Timestamp(0);
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
                        } catch (NoServersAvailableException e) {
    
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

    // Create a Stub and save it to the map
    public UserServiceGrpc.UserServiceBlockingStub createStub(String target, String qualifier) {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
        
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
    public synchronized UserServiceGrpc.UserServiceBlockingStub requestStub(String qualifier) throws NoServersAvailableException{
        NamingServer.LookUpRequest request = NamingServer.LookUpRequest.
            newBuilder().setServiceName(serviceName).setQualifier(qualifier).build();

        NamingServer.LookUpResponse response = stubNamingService.lookUp(request);
        if (response.getHostPortList().isEmpty()) {
            throw new NoServersAvailableException(); 
        }		
        String host_port =  response.getHostPortList().get(0);
        UserServiceGrpc.UserServiceBlockingStub stub = createStub(host_port, qualifier);
        return stub;
    }

    /* searchStub: search thr Map targets for a Stub, requestStub() it isn't there 
    Function called by activate(), deactivate(), getLedgerState() */
    public UserServiceGrpc.UserServiceBlockingStub searchStub(String qualifier)  throws NoServersAvailableException{
        // Get Stub from the map, return it if it exists
        UserServiceGrpc.UserServiceBlockingStub stub = targets.get(qualifier);
        if (stub != null){
            return stub;
        }
        
        // In case it doesn't exist, make a request to the NamingServer for a host_port
        // make a new stub and return it
        return requestStub(qualifier);
    }

    /*createAccount: requests the server with given qualifeier to
        create an account with id given username */
    public void createAccount(String qualifier, String username){
        UserServiceGrpc.UserServiceBlockingStub stub = null;

        // Search Stub
        try {
            stub = searchStub(qualifier); }
        catch (NoServersAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }
        
        try{
            // Request to create
            UserDistLedger.CreateAccountRequest request = UserDistLedger.CreateAccountRequest.
                newBuilder().setUserId(username).addAllPrevTS(timestamp.getTS()).build();
            debug(String.format("CreateAccountRequest -> " + "username: " + username + " | timestamp (prevTS):" + timestamp));			
            UserDistLedger.CreateAccountResponse response = stub.createAccount(request);
            trials = 0;
            debug(String.format("Response sent and call completed successfully. Created new account."));
            // Print Success
            System.out.println(SUCCESS);
            // Update clientTS from the response
            // Retrieve responseTS 
            Timestamp responseTS = new Timestamp(0);
            responseTS.setTS(response.getTSList());
            // Merge responseTS to prevTS
            debug(String.format("Notice: New timestamp received from server. Starting merge of prevTS = " + timestamp + " with responseTS."));
            timestamp.merge(responseTS);
            debug(String.format("Client's timestamp after merge -> newTS = " + timestamp)); 
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServersAvailableException u) {
                        System.err.println("Notice: No server is available.");
                        return;
                    }
                    // Retry the account creation
                    ++trials;
                    createAccount(qualifier, username);
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



    /*balance: requests the server with given qualifeier to
        show balance from the account with id given username */
    public void balance(String qualifier, String username){
        UserServiceGrpc.UserServiceBlockingStub stub = null;
        
        // Search Stub
        try {
            stub = searchStub(qualifier); }
        catch (NoServersAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }

        try{
            // Request balance
            UserDistLedger.BalanceRequest request = UserDistLedger.BalanceRequest.
                newBuilder().setUserId(username).addAllPrevTS(timestamp.getTS()).build();
            debug(String.format("BalanceRequest  -> " + "username: " + username, " | timestamp (prevTS):" + timestamp));			
            UserDistLedger.BalanceResponse response = stub.balance(request);
            trials = 0;
            debug(String.format("Response sent and call completed successfully."));
            // Print Success
            System.out.println(SUCCESS);
            // Print balance value
            System.out.println(response.getValue());
            // Update clientTS from the response
            // Retrieve responseTS 
            Timestamp responseTS = new Timestamp(0);
            responseTS.setTS(response.getValueTSList());
            // Merge responseTS to prevTS
            debug(String.format("Notice: New timestamp received from server. Starting merge of prevTS = " + timestamp + " with responseTS."));
            timestamp.merge(responseTS);
            debug(String.format("Client's timestamp after merge -> newTS = " + timestamp)); 
        }
        catch (StatusRuntimeException e) {
            // Error when server becomes unavailable
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (trials<2) {
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServersAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry balance 
                    ++trials;
                    balance(qualifier, username);
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

    /*transferTo: requests the server with given qualifeier to
        transfer given 'amount' from the account with id given 'from' 
        to the account with id given 'dest' */
    public void transferTo(String qualifier, String from, String dest, int amount){
        UserServiceGrpc.UserServiceBlockingStub stub = null;
        // Search Stub
        try {
            stub = searchStub(qualifier); }
        catch (NoServersAvailableException e) {
            System.out.println("Notice: No server is available.");
            return;
        }
        try{
            // check amount for transferTo
            if (amount <= 0) {
            debug("Throwing Exception: InvalidTransferQuantity " + amount);
            throw new InvalidTransferQuantityException();
            }
            // Request tranfesTo
            UserDistLedger.TransferToRequest request = UserDistLedger.TransferToRequest.
                newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).addAllPrevTS(timestamp.getTS()).build();
            debug(String.format("TransferToRequest -> " + "from: " + from + " | to: " + dest + " | amount: " + amount + " | timestamp (prevTS):" + timestamp));			
            UserDistLedger.TransferToResponse response = stub.transferTo(request);
            trials = 0;
            
            debug(String.format("Response sent and call completed successfully."));
            // Print Success
            System.out.println(SUCCESS);
            // Update clientTS from the response
            // Retrieve responseTS 
            Timestamp responseTS = new Timestamp(0);
            responseTS.setTS(response.getTSList());
            // Merge responseTS to prevTS
            debug(String.format("Notice: New timestamp received from server. Starting merge of prevTS = " + timestamp + " with responseTS."));
            timestamp.merge(responseTS);
            debug(String.format("Client's timestamp after merge -> newTS = " + timestamp)); 
        }
        catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                // Error when server becomes unavailable
                if (trials<2) {
                    // New Stub necessary, so request it
                    try {
                        requestStub(qualifier);
                    }
                    catch (NoServersAvailableException u) {
                        System.out.println("Notice: No server is available.");
                        return;
                    }
                    // Retry transferTo
                    ++trials;
                    transferTo(qualifier, from, dest, amount);
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
        catch (InvalidTransferQuantityException e) {
            System.out.println("[ERROR] - Invalid transfer amount!");
        }
    }
}
