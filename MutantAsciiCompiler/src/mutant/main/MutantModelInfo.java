package mutant.main;

/**
 * 
 * @author Felix Rieger
 *
 */
public class MutantModelInfo {
	public final MutantType mutantType;
	
	public final String namespaceUri;
	public final String rootType;
	public final String rootName;
	
	public MutantModelInfo(MutantType mutantType, String namespaceUri, String rootType, String rootName) {
		this.mutantType = mutantType;
		this.namespaceUri = namespaceUri;
		this.rootType = rootType;
		this.rootName = rootName;
	}
	
	public enum MutantType {
		CLASS,
		ABSTRACT,
		UML
	}
	
	
}
