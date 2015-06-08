package mutant;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;


public class Mutant {
	
	//private static Map<String, Resource> nameToResourceMap = new HashMap<String, Resource>();
	private static File mutantBasePath;
	private static File mutantExternalModelsBasePath;
	
	public static void init(File mutantModelPath) {
		mutantBasePath = mutantModelPath;
	}
	
	public static void init(File mutantModelPath, File externalModelsPath) {
		mutantBasePath = mutantModelPath;
		mutantExternalModelsBasePath = externalModelsPath;
	}
	
	public static EPackage getPackage(String packageName) {
		System.out.println("mutant get package");
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callerMethod = ste[2];
		
		String className = callerMethod.getClassName();
		String methodName = callerMethod.getMethodName();
		
		String modelName = className + "_" + methodName.toLowerCase() + "_" + packageName;
		
		EPackage retVal = (EPackage) loadMutantResource(modelName).getContents().get(0);
		return retVal;
	}
	
	private static Resource loadMutantResource(String modelName) {
		return loadResourceFromFile(mutantBasePath + File.separator + modelName + ".ecore");
	}
	
	private static Resource loadResourceFromFile(String filename) {
		EcoreFactory fact = EcoreFactory.eINSTANCE;
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
		EcorePackage ep = EcorePackage.eINSTANCE;
		URI fileUri = URI.createFileURI(new File(filename).getAbsolutePath());
		Resource res = resourceSet.getResource(fileUri, true);
		
		if (res == null) {
			System.out.println("Could not load resource " + filename);
		} else {
			System.out.println("Loaded resource");
		}
		
		return res;
	}
	
	
	
	// abstract syntax:
	public static EObject getRoot(String name) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callerMethod = ste[2];
		
		String className = callerMethod.getClassName();
		String methodName = callerMethod.getMethodName();

		String modelName = "mutant/" + className + "_" + methodName.toLowerCase() + "_" + name;
		return null;
	}

}
