package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the account for deletion doesn't exist. */
public class InvalidTransferQuantityException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = ": Transfer amount must be positive!";

	public InvalidTransferQuantityException () {
		super(BASE_MESSAGE);
	}

}