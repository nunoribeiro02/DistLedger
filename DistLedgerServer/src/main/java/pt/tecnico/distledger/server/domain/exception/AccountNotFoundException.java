package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the account doesn't exist. */
public class AccountNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = ": Account not found!!";

	public AccountNotFoundException(String userId) {
		super(userId + BASE_MESSAGE);
	}

}