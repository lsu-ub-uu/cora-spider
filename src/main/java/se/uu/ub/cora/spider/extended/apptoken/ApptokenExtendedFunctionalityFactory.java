/*
 * Copyright 2020, 2024 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.spider.extended.apptoken;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperationsImp;

public class ApptokenExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {
	private List<ExtendedFunctionalityContext> contexts = new ArrayList<>();
	private SpiderDependencyProvider dependencyProvider;
	TextHasherFactory textHasherFactory = new TextHasherFactoryImp();

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		createListOfContexts();
	}

	private void createListOfContexts() {
		createContext(UPDATE_AFTER_METADATA_VALIDATION);
		createContext(UPDATE_AFTER_STORE);
	}

	private void createContext(ExtendedFunctionalityPosition position) {
		contexts.add(new ExtendedFunctionalityContext(position, "user", 0));
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (UPDATE_AFTER_METADATA_VALIDATION == position) {
			return createExtendedFunctionalityForUpdateAfterMetadataValidation();
		}
		return createExtendedFunctionalityForUpdateAfterStore();
	}

	private List<ExtendedFunctionality> createExtendedFunctionalityForUpdateAfterMetadataValidation() {
		AppTokenHandlerExtendedFunctionality appTokenHandlerExFunc = createAppTokenHandler();
		return Collections.singletonList(appTokenHandlerExFunc);
	}

	private AppTokenHandlerExtendedFunctionality createAppTokenHandler() {
		AppTokenGeneratorImp appTokenGeneratorImp = new AppTokenGeneratorImp();
		SystemSecretOperationsImp systemSecretOperationsImp = createSystemSecretOperations();
		return AppTokenHandlerExtendedFunctionality.usingAppTokenGeneratorAndSystemSecretOperations(
				appTokenGeneratorImp, systemSecretOperationsImp);
	}

	private SystemSecretOperationsImp createSystemSecretOperations() {
		TextHasher textHasher = textHasherFactory.factor();
		return SystemSecretOperationsImp.usingDependencyProviderAndTextHasher(dependencyProvider,
				textHasher);
	}

	private List<ExtendedFunctionality> createExtendedFunctionalityForUpdateAfterStore() {
		AppTokenClearTextExtendedFuncionality clearTextExFunc = new AppTokenClearTextExtendedFuncionality();
		return Collections.singletonList(clearTextExFunc);
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

}
