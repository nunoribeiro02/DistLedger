package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the server is inactive. */
public class InactiveServerException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "The server is not receiving requests!";

	public InactiveServerException() {
		super(BASE_MESSAGE);
	}

}