package mutant.ascii.representation;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Felix Rieger
 *
 */
public class AscSig {
	/**
	 * Information about signals (abbreviated edges) in the ASCII representation
	 */
	public String signalName;				// name of the signal
	public List<Integer> incomingColors;	// colors of the classes that feed into the signal
	public List<Integer> outgoingColors;	// colors of the classes that go out of the signal
	
	public AscSig(String signalName) {
		this.signalName = signalName;
		incomingColors = new ArrayList<Integer>();
		outgoingColors = new ArrayList<Integer>();
	}
	
	
}