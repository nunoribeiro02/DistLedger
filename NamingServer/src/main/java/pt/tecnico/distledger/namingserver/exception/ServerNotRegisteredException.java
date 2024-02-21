package pt.tecnico.distledger.namingserver.exception;


/** Thrown when registering a server that is already registered */
public class ServerNotRegisteredException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "This server is not registered!";

	public ServerNotRegisteredException() {
		super(BASE_MESSAGE);
	}

}