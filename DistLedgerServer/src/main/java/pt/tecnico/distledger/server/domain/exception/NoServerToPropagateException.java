package pt.tecnico.distledger.server.domain.exception;

public class NoServerToPropagateException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "the server does not have other server to propagate state!";

	public NoServerToPropagateException () {
		super(BASE_MESSAGE);
	}

}