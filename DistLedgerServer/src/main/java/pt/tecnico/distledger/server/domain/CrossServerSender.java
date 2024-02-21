package pt.tecnico.distledger.server.domain;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.*;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.namingserver.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.domain.exception.NoServerToPropagateException;

import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.HashMap;



public class CrossServerSender {

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private int trials = 0;

    private NamingServiceGrpc.NamingServiceBlockingStub namingStub;

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    final private static String NAMING_SERVER = "localhost:5001";

    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stubPropagate;
    private Map<String, DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub> targets;

    public CrossServerSender() {
        this.targets = new HashMap<>();
        namingStub = createNamingServerStub();
    }

    // createNamingServerStub: Create a Stub for comunication with NamingServer and save it to the map
    public NamingServiceGrpc.NamingServiceBlockingStub createNamingServerStub() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(NAMING_SERVER).usePlaintext().build();
        NamingServiceGrpc.NamingServiceBlockingStub stubNamingService = NamingServiceGrpc.newBlockingStub(channel);
      
        return stubNamingService;
    }

    public List<String> getAllHosts() {
        debug(String.format("Notice: New GetAllHostsRequest made."));
        NamingServer.SearchServersRequest request = NamingServer.SearchServersRequest.newBuilder().setServiceName("DistLedger").build(); request = NamingServer.SearchServersRequest.newBuilder().setServiceName("DistLedger").build();		
        NamingServer.SearchServersResponse response = namingStub.searchServers(request);
        debug(String.format("GetAllHostsResponse -> " + response.getQualifierList()));	
        return response.getQualifierList();
    }


    public List<Integer> propagateState(String qualifier, List<Operation> ledgerList, List<Integer> originTS) throws NoServerToPropagateException{

        DistLedgerCommonDefinitions.LedgerState.Builder ledgerBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();

        int size_ledger = ledgerList.size();
		DistLedgerCommonDefinitions.Operation operationMessage = null;

        debug(String.format("PropagateState -> " + qualifier));	

		//add all operations to ledger 
		for (int i=0; i<size_ledger;i++) {
			Operation operation = ledgerList.get(i);
			if (operation instanceof CreateOp) {
				operationMessage = DistLedgerCommonDefinitions.Operation.newBuilder().setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT).setUserId(operation.getAccount()).addAllPrevTS(operation.getPrev().getCopyVector()).addAllTS(operation.getTS().getCopyVector()).build();
            }
			if (operation instanceof TransferOp) {
				TransferOp transferOp = (TransferOp) operation; 
				operationMessage = DistLedgerCommonDefinitions.Operation.newBuilder().setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).setUserId(transferOp.getAccount()).
                                    setDestUserId(transferOp.getDestAccount()).setAmount(transferOp.getAmount())
                                    .addAllPrevTS(operation.getPrev().getCopyVector()).addAllTS(operation.getTS().getCopyVector()).build();
			}
			ledgerBuilder.addLedger(operationMessage);
        }

    

        if (!(targets.containsKey(qualifier))) {
            debug(String.format("Doesn't exist stub qualifier -> " + qualifier));
            lookup(qualifier);
        }

        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stubTarget = targets.get(qualifier);
        debug(String.format("PropagateState (target) Stub: " +  stubTarget));

        DistLedgerCommonDefinitions.LedgerState ledgerState = ledgerBuilder.build();
        CrossServerDistLedger.PropagateStateRequest request = CrossServerDistLedger.PropagateStateRequest.newBuilder().setState(ledgerState).addAllReplicaTS(originTS).build();
        debug(String.format("PropagateStateRequest -> " + "LedgerState:\n" + ledgerState));			
        try {
        CrossServerDistLedger.PropagateStateResponse response = stubTarget.propagateState(request);
        debug(String.format("Response sent and call completed successfully."));
        trials = 0;
        return response.getResponseTSList();
        } 
        catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE && trials<2) {
                lookup(qualifier);
                ++trials;
                propagateState(qualifier,ledgerList,originTS);
            } 
            else {
                throw new NoServerToPropagateException();
            }
        }

        return new ArrayList<>();

    }

    public void lookup(String qualifier) throws NoServerToPropagateException{
        NamingServer.LookUpRequest request = NamingServer.LookUpRequest.newBuilder()
				.setQualifier(qualifier).setServiceName("DistLedger").build();
        NamingServer.LookUpResponse response = namingStub.lookUp(request);
        debug(String.format("Response sent and call completed successfully."));
        if (response.getHostPortList().isEmpty()) {
            throw new NoServerToPropagateException();
        }
        String target = response.getHostPortList().get(0);
        debug(String.format("Lookup Host Target: " + target));
        final ManagedChannel channelpropagate = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		targets.put(qualifier,DistLedgerCrossServerServiceGrpc.newBlockingStub(channelpropagate));
    }

}
