package se.uu.ub.cora.spider.password;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;

public class PasswordExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {

	List<ExtendedFunctionalityContext> contexts = new ArrayList<>();
	private SpiderDependencyProvider dependencyProvider;

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		List<String> knownUserTypes = findImplementingUserTypes();

		createContextForEachUserType(knownUserTypes);
	}

	private List<String> findImplementingUserTypes() {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler("user");
		return recordTypeHandler.getListOfImplementingRecordTypeIds();
	}

	private void createContextForEachUserType(List<String> knownUserTypes) {
		for (String userType : knownUserTypes) {
			contexts.add(new ExtendedFunctionalityContext(
					ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE, userType, 0));
		}
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		// TODO: test dependencyProvider to constructor
		// return Collections.singletonList(new PasswordExtendedFunctionality());
		// return Collections.singletonList(
		// PasswordExtendedFunctionality.usingDependencyProvider(dependencyProvider));
		return null;
	}

}
