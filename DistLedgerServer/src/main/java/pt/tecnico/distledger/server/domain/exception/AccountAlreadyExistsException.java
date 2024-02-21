package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the account already exists. */
public class AccountAlreadyExistsException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = ": An account with this UserId already exists!";

	public AccountAlreadyExistsException(String userId) {
		super(userId + BASE_MESSAGE);
	}

}