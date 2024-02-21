package pt.tecnico.distledger.userclient;


import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new UserService());
        parser.parseInput();

    }
}
