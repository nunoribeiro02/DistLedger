package pt.tecnico.distledger.server.domain.exception;

/** Thrown when the account for deletion doesn't exist. */
public class InvalidValueToBalanceException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final String BASE_MESSAGE = "Balance must be 0 or positive!";

    public InvalidValueToBalanceException() {
        super(BASE_MESSAGE);
    }

}