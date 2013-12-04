package cntoplolicon.seasar2assist.util;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class JavaModelUtil {

	public static IPackageFragmentRoot getPackageFragmentRoot(IPackageFragment pf) {
		if (!pf.exists()) {
			return null;
		}
		IJavaElement ret = pf;
		while (ret != null) {
			ret = ret.getParent();
			if (ret instanceof IPackageFragmentRoot) {
				return (IPackageFragmentRoot) ret;

			}
		}
		return null;
	}
}
