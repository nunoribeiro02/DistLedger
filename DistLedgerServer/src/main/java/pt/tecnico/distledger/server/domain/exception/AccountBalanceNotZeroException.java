package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the account for deletion doesn't have balance 0. */
public class AccountBalanceNotZeroException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = ": The account must have a balance of 0 before it can be deleted!";

	public AccountBalanceNotZeroException (String userId) {
		super(userId + BASE_MESSAGE);
	}

}


