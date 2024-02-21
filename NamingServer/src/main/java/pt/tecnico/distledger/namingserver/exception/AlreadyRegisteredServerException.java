package pt.tecnico.distledger.namingserver.exception;


/** Thrown when registering a server that is already registered */
public class AlreadyRegisteredServerException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "This server is already registered!";

	public AlreadyRegisteredServerException() {
		super(BASE_MESSAGE);
	}

}