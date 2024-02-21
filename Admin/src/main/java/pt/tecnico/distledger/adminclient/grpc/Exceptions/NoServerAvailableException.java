package pt.tecnico.distledger.adminclient.grpc.Exceptions;

public class NoServerAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = "There is no server available for this request!";

	public NoServerAvailableException() {
		super(BASE_MESSAGE);
	}
}
