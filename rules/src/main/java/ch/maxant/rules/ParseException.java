package ch.maxant.rules;

/**
 * When replacing {@link SubRule} placeholders (the '#' character) in rules, this exception may
 * be thrown if no suitable subrule can be found.  
 * 
 * @see Engine#Engine(java.util.Collection, boolean)
 */
public class ParseException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ParseException(String msg) {
		super(msg);
	}

}
