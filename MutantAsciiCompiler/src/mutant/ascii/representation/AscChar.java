package mutant.ascii.representation;
/**
 * 
 * @author Felix Rieger
 *
 */
public class AscChar {
	/**
	 * Character with color
	 */
	public final char c;
	public int color = 0;
	
	public AscChar(char c) {
		this.c = c;
	}
	
	@Override
	public String toString() {
		return ""+c;
	}
}