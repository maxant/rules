package ch.maxant.rules;

/**
 * Thrown if a {@link Rule}s expression cannot be compiled when the rule is 
 * added to the engine.
 */
public class CompileException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public CompileException(String msg){
		super(msg);
	}

}
