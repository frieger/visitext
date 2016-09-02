package visitext;
import java.io.File;
import java.io.IOException;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EFactory;
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


public class VisiText {
	
	//private static Map<String, Resource> nameToResourceMap = new HashMap<String, Resource>();
	private static File visiTextBasePath;
	private static File visiTextExternalModelsBasePath;
	
	private static Map<String, EPackage> namespaceUriToEPackageMap = new HashMap<String, EPackage>();

	
	public static void init(File visiTextModelPath) {
		System.out.println("Initializing VISITEXT" + visiTextModelPath.getAbsolutePath());
		visiTextBasePath = visiTextModelPath;
	}
	
	public static void init(File visiTextModelPath, File externalModelsPath) {
		System.out.println("Initializing VISITEXT" + visiTextModelPath.getAbsolutePath() + "  external models: " + externalModelsPath.getAbsolutePath());
		visiTextBasePath = visiTextModelPath;
		visiTextExternalModelsBasePath = externalModelsPath;
		loadAllMetaModelsFromPath(visiTextExternalModelsBasePath);
	}
	
	public static EPackage getPackage(String packageName, ModelType type) {
		System.out.println("visitext get package");
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callerMethod = ste[2];
		
		String className = callerMethod.getClassName();
		String methodName = callerMethod.getMethodName();
		
		String modelName = null;
		if (type == ModelType.INPUT_MODEL) {
			modelName = className + "_" + methodName.toLowerCase() + "_" + packageName;
		} else if (type == ModelType.OUTPUT_MODEL) {
			modelName = className + "_" + methodName.toLowerCase() + "-outputmodel" + "_" + packageName;
		}
		
		EPackage retVal = (EPackage) loadVisiTextResource(modelName).getContents().get(0);
		return retVal;
	}

	
	private static Resource loadVisiTextResource(String modelName) {
		return loadResourceFromFile(visiTextBasePath + File.separator + modelName + ".ecore", "ecore");
	}
	
	private static Resource loadVisiTextResourceAbstract(String modelName) {
		return loadResourceFromFile(visiTextBasePath + File.separator + modelName + ".xmi", "xmi");
	}
	
	private static Resource loadResourceFromFile(String filename, String extension) {
		EcoreFactory fact = EcoreFactory.eINSTANCE;
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(extension, new XMIResourceFactoryImpl());
		for (Entry<String, EPackage> entry : namespaceUriToEPackageMap.entrySet()) {
			resourceSet.getPackageRegistry().put(entry.getKey(), entry.getValue());

		}
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
	public static EObject getRoot(String name, ModelType type) {
		System.out.println("getting root: " + name);
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callerMethod = ste[2];

		String className = callerMethod.getClassName();
		className = className.substring(className.lastIndexOf('.')+1);
		String methodName = callerMethod.getMethodName();

		System.out.println("class name: " + className + "   methodName: " + methodName);
		
		String modelName = null;
		if (type == ModelType.INPUT_MODEL) {
			modelName = "visitext/" + className + "_" + methodName.toLowerCase() + "_" + name.toLowerCase();
		} else if (type == ModelType.OUTPUT_MODEL) {
			modelName = "visitext/" + className + "_" + methodName.toLowerCase() + "-outputmodel" + "_" + name.toLowerCase();
		}

		//String modelName = "visitext/" + className + "_" + methodName.toLowerCase() + "_" + name.toLowerCase();
		
		System.out.println("modelName: " + modelName);
		
		EObject retVal =  (EObject) loadVisiTextResourceAbstract(modelName).getContents().get(0);
		
		return retVal;
	}

	
	public static void loadAllMetaModelsFromPath(File basePath) {
		System.out.println(basePath);
		File[] files = basePath.listFiles();
		for (File f : files) {
			System.out.println("Trying to load " + f.getPath());
			String filename = f.getPath();
			String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
			if (fileExt.equalsIgnoreCase("ecore")) {
				System.out.println("loading " + f.getPath());
				// load metamodel
				EcoreFactory fact = EcoreFactory.eINSTANCE;
				ResourceSet resourceSet = new ResourceSetImpl();
				resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
				EcorePackage ep = EcorePackage.eINSTANCE;
				URI fileUri = URI.createFileURI(new File(filename).getAbsolutePath());
				Resource res = resourceSet.getResource(fileUri, true);
				
				EPackage metaModelPackage = (EPackage) res.getContents().get(0);
				String namespaceUri = metaModelPackage.getNsURI();
				namespaceUriToEPackageMap.put(namespaceUri, metaModelPackage);
				System.out.println("Loaded " + filename + " (" + namespaceUri + ")");
			}
		}
	}

	public static enum ModelType {
		INPUT_MODEL,
		OUTPUT_MODEL
	}
	
	/*
	 * 		EPackage pkg = namespaceUriToEPackageMap.get(info.namespaceUri);
				
		List<EObject> modelElements = new ArrayList<EObject>();
		Map<Integer, EObject> modelElementColors = new HashMap<Integer, EObject>();		// class colors of eclasses
		//Map<String, EEnum> eenums = new HashMap<String, EEnum>();				// enum name and eenum object
		
		
		// generate root object
		EFactory modelElementFactory = pkg.getEFactoryInstance();
		EClass rootClass = (EClass) pkg.getEClassifier(info.rootType);
		EObject rootObject = modelElementFactory.create(rootClass);

	 */
	
	
}
