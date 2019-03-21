package ch.maxant.rules;

/**
 * thrown when rules are added to the engine, if another rule
 * has the same fully qualified name.
 * @see Rule#getFullyQualifiedName()
 */
public class DuplicateNameException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public DuplicateNameException(String msg) {
		super(msg);
	}

}
