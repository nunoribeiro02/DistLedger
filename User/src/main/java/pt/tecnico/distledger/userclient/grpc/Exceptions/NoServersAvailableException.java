package pt.tecnico.distledger.userclient.grpc.Exceptions;

public class NoServersAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "There is no server available for this request!";

	public NoServersAvailableException() {
		super(BASE_MESSAGE);
	}
}
