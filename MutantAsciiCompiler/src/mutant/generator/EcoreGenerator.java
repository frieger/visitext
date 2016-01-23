package mutant.generator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mutant.ascii.representation.AscClass;
import mutant.ascii.representation.AscEdge;
import mutant.ascii.representation.AscMethod;
import mutant.main.MutantModelInfo;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

/**
 * 
 * @author Felix Rieger
 *
 */
public class EcoreGenerator {

	private static Map<String, EPackage> namespaceUriToEPackageMap = new HashMap<String, EPackage>();
	
	private static boolean embedSchemaLocationUri = false;

	/**
	 * Configure the Ecore Generator
	 * @param embedSchemaLocation if true, the generator will embed the location of the schema in the generated xmi file
	 */
	public static void configure(boolean embedSchemaLocation) {
		embedSchemaLocationUri = embedSchemaLocation;
	}
	
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
		pkg.setNsPrefix(packageName);
		pkg.setNsURI("http://" + packageName);
		
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
		
	
		
		// this hashmap is used to defer attribute type resolution for attributes which have "class" types
		// The map is structured as follows:
		// class -> (attributeName, attributeType)
		//HashMap<AscClass, Entry<String, String>> deferredAttributeTypes = new HashMap<AscClass, Entry<String, String>>();
		
		
		// create all classes. This is later used for parameter type resolution
		for (AscClass cl : classes) {
			if (cl.isEnumClass) {
				// class is enum, already processed
			} else {
				// class is not enum
				EClass ecl = fact.createEClass();
				ecl.setName(cl.classType);
				ecl.setAbstract(cl.isAbstractClass);
				eClassColors.put(cl.classColor, ecl);
				eclasses.add(ecl);
			}
		}
		
		// now process the rest of the classes
		for(AscClass cl : classes) {
			if (cl.isEnumClass) { // class is enum
				// already processed
			} else { // class is not enum
				//EClass ecl = fact.createEClass();
				//ecl.setName(cl.classType);
				//ecl.setAbstract(cl.isAbstractClass);
				EClass ecl = getClassByName(cl.classType, eclasses);
				
				// process attributes
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
						} else {
							// attribute has class type -- not supported by ecore
							System.err.println("attribute has some other type: " + key +"->" + val);
							//attrType = getClassByName(val, eclasses);
						}
					}
					eattr.setEType(attrType);

					// XXX: Visibility is not relevant for ecore
					Visibility attributeVisibility = Visibility.DEFAULT;
					// attribute visibility
					if (key.matches("\\s*\\+\\s*\\w+")) {
						// attribute is public
						attributeVisibility = Visibility.PUBLIC;
						key = key.split("\\+\\s*", 2)[1];
					} else if (key.matches("\\s*\\-\\s*\\w+")) {
						attributeVisibility = Visibility.PRIVATE;
						key = key.split("\\-\\s*", 2)[1];
					}
					
					eattr.setName(key);
					ecl.getEStructuralFeatures().add(eattr);
					
				}
					
					
					
				// process operations
				System.err.println("process operations. class " + cl.classType + " has " + cl.methods.size() + " methods");
				for (AscMethod met : cl.methods) {
					EOperation eop = fact.createEOperation();
					
					// operation name
					eop.setName(met.getMethodName());
					
					// operation return type
					EClassifier returnType = null;
					if (met.getMethodReturnType() != null && met.getMethodReturnType().trim().length() != 0) {
						returnType = getEClassifierByName(met.getMethodReturnType().trim(), eclasses, eenums);
					}
					eop.setEType(returnType);
					
					// operation parameters
					if (met.getMethodParams() != null) {
						for (Entry<String, String> param : met.getMethodParams().entrySet()) {
							EParameter ep = fact.createEParameter();
							ep.setName(param.getKey());
							ep.setEType(getEClassifierByName(param.getValue(), eclasses, eenums));
							eop.getEParameters().add(ep);
						}
					}
					
					System.err.println("++++++++++++++++++++++ adding operation" + eop);
					ecl.getEOperations().add(eop);
				}
					
					
				
				

				
							
				//eClassColors.put(cl.classColor, ecl);
				//eclasses.add(ecl);
				pkg.getEClassifiers().add(ecl);		// class is complete, add to ePackage
			}
		}		
		
		
		// process edges
		Map<AscEdge, EReference> tmpAllBidiEdges = new HashMap<AscEdge, EReference>();
		for(AscEdge ae : edges) {
			if (!ae.isInheritance) {
				EReference ref = fact.createEReference();
				ref.setName(ae.label);
				EClass sourceClass = eClassColors.get(ae.startColor);
				EClass targetClass = eClassColors.get(ae.endColor);
				ref.setEType(targetClass);
				ref.setContainment(ae.isContainment);
				
				System.out.println("edge " + ae.label + " start multiplicity: " + ae.startMultiplicity + " end multiplicity: " + ae.endMultiplicity);
				
				
				
				/*if (ae.endMultiplicity.equals("*")) {
					ref.setUpperBound(EStructuralFeature.UNBOUNDED_MULTIPLICITY);
				} else {
					ref.setUpperBound(1);
				}*/
				

				int[] multiplicities = getMultiplicityBounds(ae.endMultiplicity);
				ref.setLowerBound(multiplicities[0]);
				ref.setUpperBound(multiplicities[1]);
				
				int[] muls2 = getMultiplicityBounds(ae.startMultiplicity);
				ref.setName(ref.getName() + "__" + muls2[0] + "_" + muls2[1]);
				
				if (ae.oppositeEdge != null) {
					if (tmpAllBidiEdges.containsKey(ae.oppositeEdge)) {
						EReference oppositeEdge = tmpAllBidiEdges.get(ae.oppositeEdge);
						ref.setEOpposite(oppositeEdge);
						oppositeEdge.setEOpposite(ref);
					} else {
						tmpAllBidiEdges.put(ae, ref);
					}
				}
				
				try {
				sourceClass.getEStructuralFeatures().add(ref);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			} if (ae.isInheritance) {
				System.out.println("inheritance edge " + ae);
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

	private static EClassifier getEClassifierByName(String type, Collection<EClass> classes, Map<String,EEnum> eenums) {
		EClassifier eclassifier = null;
		
		if (type != null) {
			if (type.equalsIgnoreCase("String") || (type.equalsIgnoreCase("EString"))) {
				eclassifier = EcorePackage.eINSTANCE.getEString();
			} else if ((type.equalsIgnoreCase("int")) || (type.equalsIgnoreCase("Integer")) || (type.equalsIgnoreCase("EInt"))) {
				eclassifier = EcorePackage.eINSTANCE.getEInt();
			} else if ((type.equalsIgnoreCase("double")) || (type.equalsIgnoreCase("EDouble"))) {
				eclassifier = EcorePackage.eINSTANCE.getEDouble();
			} else if ((type.equalsIgnoreCase("float")) || (type.equalsIgnoreCase("EFloat"))) {
				eclassifier = EcorePackage.eINSTANCE.getEFloat();
			} else if ((type.equalsIgnoreCase("char")) || (type.equalsIgnoreCase("EChar"))) {
				eclassifier = EcorePackage.eINSTANCE.getEChar();
			} else if ((type.equalsIgnoreCase("Object")) || (type.equalsIgnoreCase("EObject"))) {
				eclassifier = EcorePackage.eINSTANCE.getEObject();
			} else if ((type.equalsIgnoreCase("boolean")) || (type.equalsIgnoreCase("EBoolean"))) {
				eclassifier = EcorePackage.eINSTANCE.getEBoolean();
			} else if ((eenums.keySet().contains(type))){
				eclassifier = eenums.get(type);
			} else {
				//class type
				System.err.println("some other type: >" + type + "<");
				eclassifier = getClassByName(type, classes);
			}
		}
		return eclassifier;

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
	 * @throws TypeException 
	 */
	public static void generateEcoreAbstractModel(MutantModelInfo info, List<AscClass> classes, ArrayList<AscEdge> edges, URI modelUri) throws IOException, TypeException {
		configure(true);
		
		
		EcoreFactory fact = EcoreFactory.eINSTANCE;
		
		System.out.println("Namespace uri to epackage map contains " + namespaceUriToEPackageMap.size() + " entries");
		for (Entry<String, EPackage> e : namespaceUriToEPackageMap.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
		
		System.out.println(info.namespaceUri+"<");
		EPackage pkg = namespaceUriToEPackageMap.get(info.namespaceUri);
		System.out.println(pkg);
		//EPackage.Registry.INSTANCE.put(arg0, arg1)
				
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
				System.out.println("current class: requested " + cl.classType + " got " + currentClass);
				
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
					
					if (eattr == null) {
						throw new TypeException("Attribute " + attrName + " does not exist for " + currentClass);
					}
					
					System.out.println("EAttribute: Requested " + attrName + " got " + eattr);
					System.out.println(eattr);
					System.out.println(eattr.getEType());
					
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
					} else { // possibly EENum?
						EEnum eenum = ((EEnum) eattr.getEType());
						if (eenum == null) {
							throw new TypeException("Could not find enum " + eattr.getEType());
						} else {
							attributeValueObject = eenum.getEEnumLiteralByLiteral(attrVal);
						}
						System.out.println("eenum");
					}
					
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
				if (sourceObject == null) {
					
					throw new TypeException("source not in scope: " + sourceObject + "  (target: " + targetObject + ",   edge: " + ae.label + ":" + ae.startColor + " " + ae.endColor + ")");
				}
				if (targetObject == null) {
					throw new TypeException("target not in scope");
				}
				
				EStructuralFeature reference = sourceObject.eClass().getEStructuralFeature(ae.label);	// get reference type
				
				if (reference == null) {
					throw new TypeException("reference " + ae.label + " does not exist for " + sourceObject);
				}
				
				System.out.println("Reference: Requested " + ae.label + " got " + reference);
				System.out.println("Source: " + sourceObject + " target " + targetObject);
				if (reference.isMany()) { // 0..* reference
					((List) sourceObject.eGet(reference)).add(targetObject);	// add reference to source object
				} else { // 0..1 reference
					sourceObject.eSet(reference, targetObject);
				}
				
				System.out.println("edge " + ae.label + " from " + sourceObject + " to " + targetObject + " containment: " + ae.isContainment);
				
				if (reference.isMany()) {
					for (Object e : ((List) sourceObject.eGet(reference))) {
						System.out.println("..." + e);
					}
				}
			} else {
				throw new TypeException("Inheritance can not be used in abstract syntax");
			}

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
		
		
		HashMap<String, Object> xmiExportOptions = new HashMap<String, Object>();
		if (embedSchemaLocationUri) {
			xmiExportOptions.put(XMIResource.OPTION_SCHEMA_LOCATION, true);
		}
		
		ResourceSet mrs = new ResourceSetImpl();
		mrs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMLResourceFactoryImpl());
		Resource mres = mrs.createResource(modelUri);
		mres.getContents().add(rootObject);
		
		mres.save(xmiExportOptions);

	}
	
	private static EClass getClassByName(String name, Collection<EClass> classes) {
		for (EClass ecl : classes) {
			if (name.equals(ecl.getName())) {
				return ecl;
			}
		}
		return null;
	}
	
	
	/**
	 * Strips the first and last quote " from a String
	 * @param s
	 * @return
	 */
	private static String stripQuotes(final String s) {
		int firstQuote = s.indexOf('"');
		int lastQuote = s.lastIndexOf('"');
		if (firstQuote != -1 && lastQuote != -1) {
			return s.substring(firstQuote+1, lastQuote);
		}
		return s;
	}
	
	
	/**
	 * Gets the multiplicity bounds encoded by a string of the form x..y where x = number, y = number or *
	 * @param s String
	 * @return [0]: lower bound, [1]: upper bound. Defaults to 0,-1 (0..*)
	 */
	private static int[] getMultiplicityBounds(final String s) {
		//String tmp = s.trim();
		//String[] split = tmp.split("\\.\\.");
		//split[0].
		
		int[] ret = new int[]{0, -1};
		
		Pattern regex = Pattern.compile("[0-9]+(\\.\\.)([0-9]+|\\*)");
		Matcher matcher = regex.matcher(s);
		if (matcher.find()) {
			String extracted = matcher.group();
			String[] split = extracted.split("\\.\\.");
			
			ret[0] = Integer.parseInt(split[0]);
			
			if (split[1].equals("*")) {
				ret[1] = -1;
			} else {
				ret[1] = Integer.parseInt(split[1]);
			}
		} else {
			System.err.println("problem with multiplicity: have >" + s + "<");
		}
		
		return ret;
	}
	
	private static class TypeException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2483652720758633516L;

		public TypeException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public TypeException(String arg0, Throwable arg1, boolean arg2,
				boolean arg3) {
			super(arg0, arg1, arg2, arg3);
			// TODO Auto-generated constructor stub
		}

		public TypeException(String arg0, Throwable arg1) {
			super(arg0, arg1);
			// TODO Auto-generated constructor stub
		}

		public TypeException(String arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}

		public TypeException(Throwable arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
		
		
	}
	
	private static enum Visibility {
		DEFAULT,
		PRIVATE,
		PROTECTED,
		PUBLIC
	}

}
