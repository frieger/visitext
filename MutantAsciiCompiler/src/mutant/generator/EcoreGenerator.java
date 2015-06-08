package mutant.generator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mutant.ascii.representation.AscClass;
import mutant.ascii.representation.AscEdge;
import mutant.main.MutantModelInfo;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

/**
 * 
 * @author Felix Rieger
 *
 */
public class EcoreGenerator {

	private static Map<String, EPackage> namespaceUriToEPackageMap = new HashMap<String, EPackage>();
	
	/**
	 * Generates an ecore class model from the input
	 * @param classes	list of classes
	 * @param edges		list of edges
	 * @param outputUri	URI of the generated model 
	 * @throws IOException
	 */
	public static void generateEcoreClassModel(String packageName, List<AscClass> classes, List<AscEdge> edges, URI outputUri) throws IOException {
		EcoreFactory fact = EcoreFactory.eINSTANCE;
		EPackage pkg = fact.createEPackage();
		pkg.setName(packageName);
		
		List<EClass> eclasses = new ArrayList<EClass>();
		Map<Integer, EClass> eClassColors = new HashMap<Integer, EClass>();		// class colors of eclasses
		Map<String, EEnum> eenums = new HashMap<String, EEnum>();				// enum name and eenum object
		
		// we first need to process all enums
		for (AscClass en : classes) {
			if (en.isEnumClass) {
				EEnum een = fact.createEEnum();
				een.setName(en.classType);
			
				int nextEnumValue = 0;
				for (Entry<String, String> enumLiteral : en.attributes.entrySet()) {
					String enumLiteralString = enumLiteral.getKey();
					EEnumLiteral lit = fact.createEEnumLiteral();
					lit.setName(enumLiteralString);
					lit.setValue(nextEnumValue);
					nextEnumValue++;
					
					een.getELiterals().add(lit);
				}
				
				eenums.put(en.classType, een);
				pkg.getEClassifiers().add(een);
			}
		}
		
	
		// now process the rest of the classes
		for(AscClass cl : classes) {
			if (cl.isEnumClass) { // class is enum
				// already processed
			} else { // class is not enum
				EClass ecl = fact.createEClass();
				ecl.setName(cl.classType);
				ecl.setAbstract(cl.isAbstractClass);
				
				for (Entry<String, String> attr : cl.attributes.entrySet()) {
					String key = attr.getKey();
					String val = attr.getValue();
					EAttribute eattr = fact.createEAttribute();
					EClassifier attrType = null;
					
					if (val != null) {
						if (val.equalsIgnoreCase("String") || (val.equalsIgnoreCase("EString"))) {
							attrType = EcorePackage.eINSTANCE.getEString();
						} else if ((val.equalsIgnoreCase("int")) || (val.equalsIgnoreCase("Integer")) || (val.equalsIgnoreCase("EInt"))) {
							attrType = EcorePackage.eINSTANCE.getEInt();
						} else if ((val.equalsIgnoreCase("double")) || (val.equalsIgnoreCase("EDouble"))) {
							attrType = EcorePackage.eINSTANCE.getEDouble();
						} else if ((val.equalsIgnoreCase("float")) || (val.equalsIgnoreCase("EFloat"))) {
							attrType = EcorePackage.eINSTANCE.getEFloat();
						} else if ((val.equalsIgnoreCase("char")) || (val.equalsIgnoreCase("EChar"))) {
							attrType = EcorePackage.eINSTANCE.getEChar();
						} else if ((val.equalsIgnoreCase("Object")) || (val.equalsIgnoreCase("EObject"))) {
							attrType = EcorePackage.eINSTANCE.getEObject();
						} else if ((val.equalsIgnoreCase("boolean")) || (val.equalsIgnoreCase("EBoolean"))) {
							attrType = EcorePackage.eINSTANCE.getEBoolean();
						} else if ((eenums.keySet().contains(val))){
							attrType = eenums.get(val);
						}
					}
					eattr.setEType(attrType);
					eattr.setName(key);
					ecl.getEStructuralFeatures().add(eattr);
				}
							
				eClassColors.put(cl.classColor, ecl);
				eclasses.add(ecl);
				pkg.getEClassifiers().add(ecl);
			}
		}
		
		
		
		
		// process edges
		for(AscEdge ae : edges) {
			if (!ae.isInheritance) {
				EReference ref = fact.createEReference();
				ref.setName(ae.label);
				EClass sourceClass = eClassColors.get(ae.startColor);
				EClass targetClass = eClassColors.get(ae.endColor);
				ref.setEType(targetClass);
				ref.setContainment(ae.isContainment);
				
				System.out.println("edge " + ae.label + " start multiplicity: " + ae.startMultiplicity + " end multiplicity: " + ae.endMultiplicity);
				
				if (ae.endMultiplicity.equals("*")) {
					ref.setUpperBound(EStructuralFeature.UNBOUNDED_MULTIPLICITY);
				} else {
					ref.setUpperBound(1);
				}
				
				sourceClass.getEStructuralFeatures().add(ref);
			} if (ae.isInheritance) {
				EClass childClass = eClassColors.get(ae.startColor);
				EClass superClass = eClassColors.get(ae.endColor);
				childClass.getESuperTypes().add(superClass);
			}
		}
		
		
		for (EClass ecl: eclasses) {
			System.out.println(ecl);
		}
		
		for (EClassifier ecl : pkg.getEClassifiers()) {
			System.out.println(ecl);
		}
		
		ResourceSet mrs = new ResourceSetImpl();
		mrs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMLResourceFactoryImpl());
		Resource mres = mrs.createResource(outputUri);
		mres.getContents().add(pkg);
		
		mres.save(null);
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
	
	
	
	/**
	 * Generates a xmi model from the input
	 * @param info	MutantModelInfo containing information about the namespace uri and root node
	 * @param classes	model elements from the ASCII representation
	 * @param edges		model edges fromt the ASCII representation
	 * @param modelUri	URI of saved model
	 * @throws IOException
	 */
	public static void generateEcoreAbstractModel(MutantModelInfo info, List<AscClass> classes, ArrayList<AscEdge> edges, URI modelUri) throws IOException {
		EcoreFactory fact = EcoreFactory.eINSTANCE;
		
		System.out.println("Namespace uri to epackage map contains " + namespaceUriToEPackageMap.size() + " entries");
		for (Entry<String, EPackage> e : namespaceUriToEPackageMap.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
		
		System.out.println(info.namespaceUri+"<");
		EPackage pkg = namespaceUriToEPackageMap.get(info.namespaceUri);
		System.out.println(pkg);
				
		List<EObject> modelElements = new ArrayList<EObject>();
		Map<Integer, EObject> modelElementColors = new HashMap<Integer, EObject>();		// class colors of eclasses
		//Map<String, EEnum> eenums = new HashMap<String, EEnum>();				// enum name and eenum object
		
		
		// generate root object
		EFactory modelElementFactory = pkg.getEFactoryInstance();
		EClass rootClass = (EClass) pkg.getEClassifier(info.rootType);
		EObject rootObject = modelElementFactory.create(rootClass);
		
		
		// we first need to process all enums
		/*
		for (AscClass en : classes) {
			if (en.isEnumClass) {
				EEnum een = fact.createEEnum();
				een.setName(en.classType);
			
				int nextEnumValue = 0;
				for (Entry<String, String> enumLiteral : en.attributes.entrySet()) {
					String enumLiteralString = enumLiteral.getKey();
					EEnumLiteral lit = fact.createEEnumLiteral();
					lit.setName(enumLiteralString);
					lit.setValue(nextEnumValue);
					nextEnumValue++;
					
					een.getELiterals().add(lit);
				}
				
				eenums.put(en.classType, een);
				pkg.getEClassifiers().add(een);
			}
		} */
		
		
			
		// now process the rest of the classes
		for(AscClass cl : classes) {
			if (cl.isEnumClass) { // class is enum
				// already processed
			} else { // class is not enum
				EClass currentClass = (EClass) pkg.getEClassifier(cl.classType);
				EObject currentObject;
				
				if (cl.classType.equals(info.rootType) && (cl.instanceName.equals(info.rootName))) {
						// this is the root object
					currentObject = rootObject;
				} else {
					currentObject = modelElementFactory.create(currentClass);
				}
					
				for (Entry<String, String> attr : cl.attributes.entrySet()) {
					String attrName = attr.getKey();
					String attrVal = stripQuotes(attr.getValue());
					//EAttribute eattr = fact.createEAttribute();
					EAttribute eattr = (EAttribute) currentClass.getEStructuralFeature(attrName);

					System.out.println(eattr);
					System.out.println(eattr.getEType());

					System.out.println("is string: " + eattr.getEType().equals(EcorePackage.eINSTANCE.getEInt()));
					
					Object attributeValueObject = null;
					
					if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEString())) {
						attributeValueObject = attrVal;
					} else if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEInt())) {
						attributeValueObject = Integer.parseInt(attrVal);
					} else if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEDouble())) {
						attributeValueObject = Double.parseDouble(attrVal);
					} else if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEFloat())) {
						attributeValueObject = Float.parseFloat(attrVal);
					} else if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEChar())) {
						attributeValueObject = attrVal.charAt(0);
					} else if (eattr.getEType().equals(EcorePackage.eINSTANCE.getEBoolean())) {
						attributeValueObject = Boolean.parseBoolean(attrVal);
					}
					// TODO: enums
					
					currentObject.eSet(eattr, attributeValueObject);
					
					/*if (val != null) {
						if (val.equalsIgnoreCase("String") || (val.equalsIgnoreCase("EString"))) {
							attrType = EcorePackage.eINSTANCE.getEString();
						} else if ((val.equalsIgnoreCase("int")) || (val.equalsIgnoreCase("Integer")) || (val.equalsIgnoreCase("EInt"))) {
							attrType = EcorePackage.eINSTANCE.getEInt();
						} else if ((val.equalsIgnoreCase("double")) || (val.equalsIgnoreCase("EDouble"))) {
							attrType = EcorePackage.eINSTANCE.getEDouble();
						} else if ((val.equalsIgnoreCase("float")) || (val.equalsIgnoreCase("EFloat"))) {
							attrType = EcorePackage.eINSTANCE.getEFloat();
						} else if ((val.equalsIgnoreCase("char")) || (val.equalsIgnoreCase("EChar"))) {
							attrType = EcorePackage.eINSTANCE.getEChar();
						} else if ((val.equalsIgnoreCase("Object")) || (val.equalsIgnoreCase("EObject"))) {
							attrType = EcorePackage.eINSTANCE.getEObject();
						} else if ((val.equalsIgnoreCase("boolean")) || (val.equalsIgnoreCase("EBoolean"))) {
							attrType = EcorePackage.eINSTANCE.getEBoolean();
						} else if ((eenums.keySet().contains(val))){
							attrType = eenums.get(val);
						}
					}*/
					/*eattr.setEType(attrType);
					eattr.setName(key);
					currentObject.getEStructuralFeatures().add(eattr);*/
				}
							
				modelElementColors.put(cl.classColor, currentObject);
				modelElements.add(currentObject);
				//pkg.getEClassifiers().add(ecl);
			}
		}
		
		
		
		
		// process edges
		for(AscEdge ae : edges) {
			if (!ae.isInheritance) {
				EObject sourceObject = modelElementColors.get(ae.startColor);
				EObject targetObject = modelElementColors.get(ae.endColor);
				
				EStructuralFeature reference = sourceObject.eClass().getEStructuralFeature(ae.label);	// get reference type
				((List) sourceObject.eGet(reference)).add(targetObject);	// add reference to source object
				
				
				System.out.println("edge " + ae.label + " from " + sourceObject + " to " + targetObject + " containment: " + ae.isContainment);
				
				
				for (Object e : ((List) sourceObject.eGet(reference))) {
					System.out.println("..." + e);
				}
			} /*if (ae.isInheritance) {
				EClass childClass = modelElementColors.get(ae.startColor);
				EClass superClass = modelElementColors.get(ae.endColor);
				childClass.getESuperTypes().add(superClass);
			} */
		}
		
		
		for (EObject ecl: modelElements) {
			System.out.println(ecl);
		}
		
		for (EClassifier ecl : pkg.getEClassifiers()) {
			System.out.println(ecl);
		}
		
		
		for (EObject eo : rootObject.eContents()) {
			System.out.println(":" + eo);
		}
		
		
		ResourceSet mrs = new ResourceSetImpl();
		mrs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMLResourceFactoryImpl());
		Resource mres = mrs.createResource(modelUri);
		mres.getContents().add(rootObject);
		
		mres.save(null);

	}
	
	private static String stripQuotes(final String s) {
		int firstQuote = s.indexOf('"');
		int lastQuote = s.lastIndexOf('"');
		if (firstQuote != -1 && lastQuote != -1) {
			return s.substring(firstQuote+1, lastQuote);
		}
		return s;
	}

}
