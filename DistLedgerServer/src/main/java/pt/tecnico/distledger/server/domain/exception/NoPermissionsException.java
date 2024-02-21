package pt.tecnico.distledger.server.domain.exception;

public class NoPermissionsException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "the server does not have permissions to perform this action!";

	public NoPermissionsException () {
		super(BASE_MESSAGE);
	}

}