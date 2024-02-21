package pt.tecnico.distledger.server.domain.exception;

public class ServerNotUpdatedException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "the server is not updated!";

	public ServerNotUpdatedException () {
		super(BASE_MESSAGE);
	}

}