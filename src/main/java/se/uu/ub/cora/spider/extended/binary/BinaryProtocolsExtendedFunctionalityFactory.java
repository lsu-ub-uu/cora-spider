package se.uu.ub.cora.spider.extended.binary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class BinaryProtocolsExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {

	List<ExtendedFunctionalityContext> contexts = new ArrayList<>();

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		contexts.add(new ExtendedFunctionalityContext(
				ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE, "binary", 0));
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		return Collections.singletonList(new BinaryProtocolsExtendedFunctionality());
	}

}
