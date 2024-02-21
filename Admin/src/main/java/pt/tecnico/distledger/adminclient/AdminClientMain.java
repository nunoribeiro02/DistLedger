package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

import static io.grpc.Status.INVALID_ARGUMENT;

public class AdminClientMain {

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }
    
    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new AdminService());
        parser.parseInput();

    }
}
