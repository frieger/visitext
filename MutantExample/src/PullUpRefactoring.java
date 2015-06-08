import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

public class PullUpRefactoring {
	EPackage package_;

	public PullUpRefactoring(EPackage package_) {
		super();
		this.package_ = package_;
	}
	
	public void execute() {
		for (EClassifier cl :package_.getEClassifiers()) {
			if (cl instanceof EClass) {
				EClass class1 = (EClass)cl;
				for (EClassifier cl2 :package_.getEClassifiers()) {
					if (cl2 != cl && cl2 instanceof EClass) {
						EClass class2 = (EClass)cl;
						pullUpCommonAttributes(class1, class2);
					}
				}
			}
		}
	}

	private void pullUpCommonAttributes(EClass class1, EClass class2) {
		for (EClass superType : class1.getEAllSuperTypes()) {
			if (class2.getEAllSuperTypes().contains(superType)) {
				pullUpCommonAttributes(class1, class2, superType);
			}
		}
	}

	private void pullUpCommonAttributes(EClass class1, EClass class2,
			EClass superType) {
		List<EAttribute> toDelete = new ArrayList<EAttribute>();
		List<EAttribute> toPullUp = new ArrayList<EAttribute>();

		for (EStructuralFeature feat1 : class1.getEStructuralFeatures()) {
			if (feat1 instanceof EAttribute) {
				EAttribute attribute = (EAttribute)feat1;
				EStructuralFeature feat2 = class2.getEStructuralFeature(attribute.getName());
				if (feat2!=null && feat2 instanceof EAttribute && feat2.getEType() == attribute.getEType()) {
					toDelete.add((EAttribute) feat2);
					toPullUp.add((EAttribute) feat1);
				}
			}
		}
		
		for (EAttribute ea : toDelete) {
			class2.getEStructuralFeatures().remove(ea);
		}
		
		for (EAttribute ea : toPullUp) {
			// The following line ensures that 'attribute' is also removed
			// from class1's eStructuralFeatures (due to containment property)
			superType.getEStructuralFeatures().add(ea);
		}
	}

}
