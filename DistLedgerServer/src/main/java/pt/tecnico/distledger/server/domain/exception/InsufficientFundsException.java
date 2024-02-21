package pt.tecnico.distledger.server.domain.exception;


/** Thrown when the account for transfer doesn't have enough funds. */
public class InsufficientFundsException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String BASE_MESSAGE = ": This account doesn't have enough funds!";

	public InsufficientFundsException (String userId) {
		super(userId + BASE_MESSAGE);
	}

}
