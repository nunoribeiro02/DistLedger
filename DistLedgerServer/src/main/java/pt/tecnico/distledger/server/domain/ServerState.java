package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.Timestamp;
import pt.tecnico.distledger.server.domain.LedgerRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.HashMap;

import pt.ulisboa.tecnico.distledger.contract.*;

import pt.tecnico.distledger.server.domain.exception.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exception.AccountNotFoundException;
import pt.tecnico.distledger.server.domain.exception.AccountBalanceNotZeroException;
import pt.tecnico.distledger.server.domain.exception.InvalidTransferQuantityException;
import pt.tecnico.distledger.server.domain.exception.InsufficientFundsException;
import pt.tecnico.distledger.server.domain.exception.InactiveServerException;
import pt.tecnico.distledger.server.domain.exception.NoPermissionsException;
import pt.tecnico.distledger.server.domain.exception.InvalidValueToBalanceException;
import pt.tecnico.distledger.server.domain.exception.NoServerToPropagateException;
import pt.tecnico.distledger.server.domain.exception.ServerNotUpdatedException;

public class ServerState {

    // Set flag to true using the -Ddebug to print debug messages.
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private static final String BASE_QUALIFIER = "A";

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    /* broker account attributes */
    private static final String BROKER = "broker";
    private static final int BROKER_INITIAL_BALANCE = 1000;


    /* Map with relation userID: balance */
    private Map<String, Integer> accounts;

    private List<LedgerRecord> updateLog;

    /* server state */
    private boolean active = true;

    private String qualifier;

    private Integer id;

    private Timestamp valueTS;

    private Timestamp replicaTS;

    private List<Timestamp> tableTS;

    private CrossServerSender stateSender;

    public ServerState(String qualifier) throws AccountAlreadyExistsException, InvalidValueToBalanceException {
        this.updateLog = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.qualifier = qualifier;
        this.id = calculateID(qualifier);
        this.valueTS = new Timestamp(id + 1);
        this.replicaTS = new Timestamp(id + 1);
        this.tableTS = new ArrayList<>();
        expandTS(id + 1);
        this.stateSender = new CrossServerSender();
        addAccount(BROKER, BROKER_INITIAL_BALANCE);
    }

    /* getLedger: returns a copy of the ledger */
    public synchronized List<Operation> getLedger() {
        List<Operation> ledgerCopy = new ArrayList<>();
        for (int i = 0; i< updateLog.size(); i++) {
            ledgerCopy.add(updateLog.get(i).getOperation());
        }
        return ledgerCopy;
    }

    /*
     * createLedgerRecord: creates a new LedgerRecord for given originReplica,
     * operation and timestamp calls addLedgerRecord to add it to the updateLog
     */
    public LedgerRecord createLedgerRecord(Operation operation) {
        LedgerRecord ledgerRecord = new LedgerRecord(operation);
        addLedgerRecord(ledgerRecord);
        return ledgerRecord;
    }

    /* addLedgerRecord: adds a LedgerRecord to the updateLog */
    public void addLedgerRecord(LedgerRecord ledgerRecord) {
        this.updateLog.add(ledgerRecord);
    }

    // Getters and Setters
    public void setValueTS(List<Integer> ts) {
        this.valueTS.setTS(ts);
    }

    public List<Integer> getValueTS() {
        Timestamp tsCopy = new Timestamp(0);
        tsCopy.copyTS(valueTS);
        return tsCopy.getVector();
    }

    public void setReplicaTS(List<Integer> ts) {
        this.replicaTS.setTS(ts);
    }

    public List<Integer> getReplicaTS() {
        Timestamp tsCopy = new Timestamp(0);
        tsCopy.copyTS(replicaTS);
        return tsCopy.getVector();
    }

    /*
     * conditions_addAccount: Verifies all the conditions necessary to add an
     * account throws AccountAlreadyExistsException if account is already registered
     * throws InvalidValueToBalanceException if balance is nor valid Function called
     * by addAccount()
     */
    public void conditions_addAccount(String account, Integer balance)
            throws AccountAlreadyExistsException, InvalidValueToBalanceException {

        /* verifies if account already exists */
        if (accounts.containsKey(account)) {
            debug("Throwing Exception: AccountAlreadyExists: " + account);
            throw new AccountAlreadyExistsException(account);
        } else if (balance < 0) {
            debug("Throwing Exception: Invalid Balance: " + balance);

            throw new InvalidValueToBalanceException();
        }
    }

    /*
     * addAccount: adds given account with given balance if all the conditions are
     * met
     */
    public void addAccount(String account, Integer balance)
            throws AccountAlreadyExistsException, InvalidValueToBalanceException {
        conditions_addAccount(account, balance);
        this.accounts.put(account, balance);
    }

    /*
     * addAccount: adds given account with 0 balance if all the conditions are met
     */
    public void addAccount(String account) throws AccountAlreadyExistsException, InvalidValueToBalanceException {
        conditions_addAccount(account, 0);
        this.accounts.put(account, 0);
    }

    /*
     * conditions_removeAccount: Verifies all the conditions necessary to remove an
     * account throws AccountBalanceNotZeroException when trying to remove account
     * with balance not zero throws AccountNotFoundException when account is not in
     * the Map of accounts Function called by removeAccount()
     */
    public void conditions_removeAccount(String account)
            throws AccountBalanceNotZeroException, AccountNotFoundException {
        /* verifies if account exists */
        if (!accounts.containsKey(account)) {
            debug("Throwing Exception: AccountNotFound: " + account);
            throw new AccountNotFoundException(account);
            /* verifies if balance is 0 */
        } else if (this.accounts.get(account) != 0) {
            debug("Throwing Exception: AccountBalanceNotZero: " + account);
            throw new AccountBalanceNotZeroException(account);
        }
    }

    /*
     * removeAccount: removes account if all the conditions are met throws
     * AccountBalanceNotZeroException when trying to remove account with balance not
     * zero throws AccountNotFoundException when account is not in the Map of
     * accounts
     */
    public void removeAccount(String account) throws AccountBalanceNotZeroException, AccountNotFoundException {
        conditions_removeAccount(account);
        this.accounts.remove(account);
    }

    /*
     * getBalance: Return balance for given account throws AccountNotFoundException
     * when account is not in the Map of accounts Function called by balance()
     */
    public Integer getBalance(String account) throws AccountNotFoundException {
        /* verifies if account exists */
        if (!accounts.containsKey(account)) {
            debug("Throwing Exception: AccountNotFound: " + account);

            throw new AccountNotFoundException(account);
        } else {
            return this.accounts.get(account);
        }

    }

    /*
     * getBalance: puts given amount in given account throws
     * InvalidValueToBalanceException when balance is invalid
     */
    public void putBalance(String account, Integer balance) throws InvalidValueToBalanceException {
        if (balance < 0) {
            debug("Throwing Exception: Invalid Balance: " + balance);

            throw new InvalidValueToBalanceException();
        }

        this.accounts.put(account, balance);
    }

    /*
     * updateBalance: updates given amount in given account throws
     * InsufficientFundsException when the the account doesn't have enough funds
     * throws AccountNotFoundException when when account is not in the Map of
     * accounts Function called by transfer()
     */
    public void updateBalance(String account, Integer amount)
            throws AccountNotFoundException, InsufficientFundsException {
        debug("Update Balance: " + amount + " from Account: " + account);

        /* verifies if account exists */
        if (!accounts.containsKey(account)) {
            debug("Throwing Exception: AccountNotFound: " + account);

            throw new AccountNotFoundException(account);
        }

        int value = this.accounts.get(account) + amount;

        /* verifies if balance is not negative after update */
        if (value < 0) {
            debug("Throwing Exception: InsufficientFunds: " + account);

            throw new InsufficientFundsException(account);
        }

        this.accounts.put(account, value);
    }

    /*
     * conditions_transfer: Verifies all the conditions necessary to transfer throws
     * InvalidTransferQuantityException when trying to transfer invalid amount
     * throws AccountNotFoundException when account is not in the Map of accounts
     * Function called by transfer()
     */
    public void conditions_transfer(String account, String destAccount, int amount)
            throws AccountNotFoundException, InvalidTransferQuantityException, InsufficientFundsException {

        // verifies if transfer value is not negative
        if (amount <= 0) {
            debug("Throwing Exception: InvalidTransferQuantity " + amount);

            throw new InvalidTransferQuantityException();
        }

        // verifies if both accounts exist
        if (!accounts.containsKey(account)) {
            debug("Throwing Exception: AccountNotFound: " + account);

            throw new AccountNotFoundException(account);
        }

        if (!accounts.containsKey(destAccount)) {
            debug("Throwing Exception: AccountNotFound: " + destAccount);

            throw new AccountNotFoundException(destAccount);
        }

        int value = getBalance(account) - amount;
        /* verifies if balance is not negative after update */
        if (value < 0) {
            debug("Throwing Exception: InsufficientFunds: " + account);
            throw new InsufficientFundsException(account);
        }
    }

    /*
     * transfer: transfers given amount from given account to given destAccount if
     * all conditions are met
     */
    public void transfer(String account, String destAccount, int amount)
            throws AccountNotFoundException, InvalidTransferQuantityException, InsufficientFundsException {
        conditions_transfer(account, destAccount, amount);
        // update the two balances
        updateBalance(account, (int) -amount);
        updateBalance(destAccount, amount);
    }

    /*
     * conditions_server: Verifies if the server working and has permissions throws
     * InactiveServerException when the server isnt active throws
     * NoPermissionsException when the server has no permission
     */
    public void conditions_server(boolean server) throws InactiveServerException {
        /* verifies if server is active */
        if (!isActive()) {
            debug("Throwing Exception: InactiveServer");
            throw new InactiveServerException();
        }
    }

    /* balance: gets balance from given account */
    public synchronized BalanceResult balance(String account, List<Integer> ts, boolean server)
            throws AccountNotFoundException, InactiveServerException, ServerNotUpdatedException {
        /* verifies if server is active */
        if (!isActive() && (!server)) {
            debug("Throwing Exception: InactiveServer");
            throw new InactiveServerException();
        }
        Timestamp prev = new Timestamp(ts.size());
        prev.setTS(ts);
        if (prev.getSize() > tableTS.size()) {
            expandTS(prev.getSize());
        }

        try {
            while (!valueTS.greaterOrEqual(prev)) {
                wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); 
            System.err.println("Thread Interrupted");
        }

        BalanceResult result = new BalanceResult();

        int balance = getBalance(account);
        result.setBalance(balance);
        result.setTS(valueTS.getCopyVector());

        return result;

    }

    /* activates server: the server can accept requests */
    public synchronized void activate() {
        this.active = true;
    }

    /* deactivates server: the server can not accept requests */
    public synchronized void deactivate() {
        this.active = false;
    }

    public synchronized boolean isActive() {
        return this.active;
    }

    /*
     * createAccountOperation: given server creates a new account with given UserId
     * and propagates it
     */
    public synchronized List<Integer> createAccountOperation(String userId, List<Integer> prevTS, boolean server)
            throws InactiveServerException, NoPermissionsException {
        debug(String.format("createAccountOperation -> " + "username: " + userId + " | timestamp (prevTS):" + prevTS));

        /* create timestamps for operation */
        conditions_server(server);
        Timestamp prev = new Timestamp(prevTS.size());
        prev.setTS(prevTS);
        if (prevTS.size() > tableTS.size()) {
            expandTS(prevTS.size());
        }
        /* assert prev with valueTS */
        prev.assertSize(valueTS);
        Timestamp ts = new Timestamp(prevTS.size());
        ts.copyTS(prev);
        /* increment replicateTS value */
        int replicaTS_value = replicaTS.getValue(this.id);
        debug(String.format("replicaTS_value:" + replicaTS_value));
        replicaTS.updateValue(this.id, replicaTS_value + 1);
        /* update TS value */
        ts.updateValue(this.id, replicaTS_value + 1);
        /* create the new operation */
        CreateOp operation = new CreateOp(userId, ts, prev);
        /* create new LedgerRecord */
        LedgerRecord record = createLedgerRecord(operation);
        debug(String.format("LedgerRecord -> operation: CreateAccount" + " | username: " + userId + " | prevTS: " + prev + " | TS: " + ts + " | stable: " + record.isStable()));

        if (valueTS.greaterOrEqual(prev)) {

            /* set the operation stable */
            record.setStable();

            try {
                /* add account to accounts */
                valueTS.merge(operation.getTS());
                addAccount(operation.getAccount());
                /* add operation to ledger */
                debug(String.format("Create Account Operation: Successful"));
            } catch (AccountAlreadyExistsException | InvalidValueToBalanceException e) {
                debug(String.format("Exception:" + e.getMessage()));
                return operation.getTS().getCopyVector();
            }
        }
        return operation.getTS().getCopyVector();
    }

    /*
     * transferOperation: given server tranfers given amount from given account to
     * given destAccount and propagates it
     */
    public synchronized List<Integer> transferOperation(String account, String destAccount, int amount, boolean server,
            List<Integer> prevTS) throws InvalidTransferQuantityException,
            InactiveServerException {

        debug(String.format("Transfer Operation -> from: %s | to: %s | amount: %d | timestamp (prevTS):%s", account, destAccount,
                amount, prevTS));

        conditions_server(server);
        Timestamp prev = new Timestamp(prevTS.size());
        prev.setTS(prevTS);
        /* check for new server replicate, update TS if new one appears */
        if (prevTS.size() > tableTS.size()) {
            expandTS(prevTS.size());
        }
        /* assert prev with valueTS */
        prev.assertSize(valueTS);
        debug(String.format("prev:" + prev));
        Timestamp ts = new Timestamp(prevTS.size());
        ts.copyTS(prev);
        /* increment replicateTS value */
        int replicaTS_value = replicaTS.getValue(this.id);
        replicaTS.updateValue(this.id, replicaTS_value + 1);
        /* update TS value */
        ts.updateValue(this.id, replicaTS_value + 1);
        /* create the new operation */
        TransferOp operation = new TransferOp(account, destAccount, amount, ts, prev);
        /* create new LedgerRecord */
        LedgerRecord record = createLedgerRecord(operation);
        debug(String.format("LedgerRecord -> operation: TransferTo" + " | account: " + account + " | destAccount: " + destAccount  + " | amount: " + amount + " | prevTS: " + prev + " | TS: " + ts + " | stable: " + record.isStable()));

        if (valueTS.greaterOrEqual(prev)) {
            /* do operation if the conditions are met */
            /* set the operation stable */
            record.setStable();
            
            try {
                valueTS.merge(operation.getTS());
                transfer(operation.getAccount(), operation.getDestAccount(), operation.getAmount());
                /* add operation to ledger */
                debug(String.format("Transfer Operation: Successful"));
            } catch (AccountNotFoundException | InvalidTransferQuantityException | InsufficientFundsException e) {
                debug(String.format("Exception:" + e.getMessage()));
                return operation.getTS().getCopyVector();
            }
        }
        return operation.getTS().getCopyVector();

    }

    /*gossip: determines what servers need to the sent the propagation
     * of the state and orders said propagation to them*/
    public synchronized void gossip() throws InactiveServerException{
        conditions_server(false);

        List<String> qualifiers = stateSender.getAllHosts();
        int nReplicas = qualifiers.size();
        for (int i=0; i<nReplicas; i++) {
            String replica = qualifiers.get(i);
            int replicaID=calculateID(replica);
            if (replicaID!=this.id) {
                // Update ts size
                if (replicaID>(valueTS.getSize()-1)) {
                    expandTS(replicaID+1);
                }
                //Propagate to target replica
                propagateState(replica);
            }

        }
    }

    /*propagateState: propagates state to target replica*/
    public void propagateState(String replica) {
        int replicaID = calculateID(replica);
        List<Operation> stateToPropagate = new ArrayList<>();
        int nRecords = updateLog.size();
        // Select operations to send
        for (int i=0;i<nRecords;i++) {
            LedgerRecord lr = updateLog.get(i);
            if (!tableTS.get(replicaID).greaterOrEqual(lr.getOperation().getTS())) {
                stateToPropagate.add(lr.getOperation());
            }
        }
        try {
            if (!stateToPropagate.isEmpty()) {
                // Propagates state to target replica
                List<Integer> tsReplica = stateSender.propagateState(replica,stateToPropagate, this.replicaTS.getCopyVector());
                // Update tableTS entry with sent timestamp
                if (!tsReplica.isEmpty()) {
                    tableTS.get(replicaID).setTS(tsReplica);
                }
                
            }
        }
        catch (NoServerToPropagateException e) {}
    }

    /*acceptGossip: receives propagated state  */
    public synchronized List<Integer> acceptGossip(List<Operation> gossipState, List<Integer> receivedTsVector) {
        
        if (!this.isActive()) {
            return new ArrayList<>();
        }
        
        int nUpdates = gossipState.size();
        debug(String.format("Notice: Received new GossipRequest with %d updates", nUpdates));

        Timestamp receivedTs = new Timestamp(0);
        receivedTs.setTS(receivedTsVector);
        
        // Update ts size
        if (receivedTs.getSize()>valueTS.getSize()) {
            debug(String.format("Expand timestamps with for" + valueTS.getSize() + "replicas."));
            expandTS(receivedTs.getSize());
        }
        
        // For every update, create ledger record if doens't exist on the updateLog
        for (int i=0; i<nUpdates;i++) {
            Operation op = gossipState.get(i);
            if ((!this.replicaTS.greaterOrEqual(op.getTS()) && (!operationAlreadyExists(op.getTS())))) {
                createLedgerRecord(op);
            }
        }
        debug(String.format("Notice: Added %d new LedgerRecords to LedgerState", nUpdates));
        
        //Sort operations
        this.updateLog.sort(null);

        replicaTS.merge(receivedTs);
  
        this.executeLedgerOp();
        debug(String.format("All operations in Ledger were exectuted: valueTS:" + valueTS +  " | replicaTS: " + replicaTS));

        return replicaTS.getCopyVector();
    }

    /*executeLedgerOp: executes the operations on the updateLog*/
    public void executeLedgerOp () {
        boolean changed = false;
        int sizeLedger = updateLog.size();
        //debug(String.format("ExecuteLedgerOp with Logsize:" + sizeLedger));
        for (int i=0; i<sizeLedger; i++) {
            LedgerRecord lr = updateLog.get(i);
            Operation op = lr.getOperation();
            if (valueTS.greaterOrEqual(op.getPrev()) && !(lr.isStable())) {
                changed = true;
                this.executeOperation(lr);
            }
        }

        if (changed) {
            this.executeLedgerOp();
        }
    }

    /*executeOperation: execute operation in given ledgerRecord
     * Function called by executeLedgerOp() */
    public void executeOperation(LedgerRecord lr) {
        Operation operation = lr.getOperation();
        // Execute CreateAccount operation
        if (operation instanceof CreateOp) {
            lr.setStable();
            CreateOp createOp = (CreateOp) operation;
            try {
                valueTS.merge(createOp.getTS());
                this.addAccount(createOp.getAccount());
                notifyAll();
            }
            catch (AccountAlreadyExistsException | InvalidValueToBalanceException e) {
                notifyAll();
            }
        }
        // Execute TransferTo operation
        if (operation instanceof TransferOp) {
            lr.setStable();
            TransferOp transferOp = (TransferOp) operation;
            try {
                valueTS.merge(transferOp.getTS());
                transfer(transferOp.getAccount(), transferOp.getDestAccount(), transferOp.getAmount());
                notifyAll();
            }
            catch (AccountNotFoundException | InvalidTransferQuantityException | InsufficientFundsException e) {
                notifyAll();
            }
        }
    }

    /*calculateID: calculates ID for server based on given qualifier and returns it */
    public Integer calculateID(String qualifier) {
        return ((int) qualifier.toCharArray()[0] - BASE_QUALIFIER.toCharArray()[0]);
    }

    /*expandTS: expands both valueTS and replicaTS to given size
    and every entry in the TableTS */
    public void expandTS(int nReplicas) {
        valueTS.expand(nReplicas);
        replicaTS.expand(nReplicas);
        int size = tableTS.size();
        for (int i = 0; i < size; i++) {
            tableTS.get(i).expand(nReplicas);
        }
        for (int i = size; i < nReplicas; i++) {
            if (i == this.id) {
                tableTS.add(this.valueTS);
            } else {
                Timestamp newTs = new Timestamp(nReplicas);
                tableTS.add(newTs);
            }
        }
    }

    /*operationAlreadyExists: determines wheter an operation already exists
     * in the updateLog; returns true if operation exists*/
    public boolean operationAlreadyExists(Timestamp ts) {
        for (LedgerRecord lr: updateLog) {
            if (lr.getOperation().getTS().equals(ts)) {
                return true;
            }
        }
        return false;
    }

    /*copyTS: copies list of values from given Timestamp object */
    public List<Integer> copyTS(Timestamp ts) {
        List<Integer> ts_return = new ArrayList<>();
        int size = ts.getSize();
        for (int i = 0; i < size; i++) {
            ts_return.add(ts.getValue(i));
        }
        return ts_return;
    }
}
