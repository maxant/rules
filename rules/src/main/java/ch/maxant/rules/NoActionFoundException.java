package ch.maxant.rules;

/**
 * Every rule has an outcome.  If the engine is given actions
 * then every possible outcome must have an action.  If the {@link Engine}
 * isn't given a suitable action to an outcome, this exception is thrown.
 */
public class NoActionFoundException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NoActionFoundException(String msg) {
		super(msg);
	}
}
