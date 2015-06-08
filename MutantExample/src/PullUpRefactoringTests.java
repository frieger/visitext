import java.io.File;

import mutant.Mutant;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.junit.BeforeClass;
import org.junit.Test;


public class PullUpRefactoringTests {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File mutantModelPath = new File("./mutant/");
		Mutant.init(mutantModelPath);
	}
	

	@Test
	/** @InputModel EPackage pkg = 
	   
	                  +------------+                               
	                  |   Person   |                             
	                  |------------|      
	                  +------------+                               
	                      A   A                          
	             .--------'   '-------.                 
	             |                    |                 
	      +--------------+   +--------------+      
	      | Professor    |   | Student      |
	      |--------------|   |--------------|      
	      | name: String |   | name: String |      
	      +--------------+   +--------------+   
	      
	 */
	public void testExecute() {
		EPackage pkg = Mutant.getPackage("pkg");
		System.out.println(pkg);
		for (EClassifier ecl : pkg.getEClassifiers()) {
			System.out.println(ecl);
		}
		EClass person = (EClass) pkg.getEClassifier("Person");
		assert(person.getEAttributes().size()==0);
		PullUpRefactoring refactoring = new PullUpRefactoring(pkg);
		refactoring.execute();
		assert(person.getEAttributes().size()==1);
	}

}
