package se.uu.ub.cora.spider.extended2;

import java.util.ServiceLoader;

import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;

public class ExtendedFunctionalityStarterSpy implements ExtendedFunctionalityStarter {

	private ExtendedFunctionalityProviderSpy extendedFunctionalityProviderSpy = new ExtendedFunctionalityProviderSpy();
	private ServiceLoader<ExtendedFunctionalityForCreateBeforeMetadataValidation> createBeforeMetadataValidation;;

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return extendedFunctionalityProviderSpy;
	}

	public Iterable<ExtendedFunctionalityForCreateBeforeMetadataValidation> getCreateBeforeMetadataValidation() {
		return createBeforeMetadataValidation;
	}

	@Override
	public void setExtendedFunctionalityForCreateBeforeMetadataValidation(
			ServiceLoader<ExtendedFunctionalityForCreateBeforeMetadataValidation> load) {
		this.createBeforeMetadataValidation = load;

	}

}
