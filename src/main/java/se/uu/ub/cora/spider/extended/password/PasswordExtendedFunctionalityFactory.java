/*
 * Copyright 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.password;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class PasswordExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {
	private static final String USER_RECORD_TYPE = "user";
	List<ExtendedFunctionalityContext> contexts = new ArrayList<>();
	private SpiderDependencyProvider dependencyProvider;
	TextHasherFactory textHasherFactory = new TextHasherFactoryImp();

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		contexts.add(new ExtendedFunctionalityContext(UPDATE_BEFORE_STORE, USER_RECORD_TYPE, 0));
		contexts.add(new ExtendedFunctionalityContext(UPDATE_AFTER_STORE, USER_RECORD_TYPE, 0));
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (position == UPDATE_BEFORE_STORE) {
			return Collections.singletonList(
					PasswordExtendedFunctionality.usingDependencyProviderAndTextHasher(
							dependencyProvider, textHasherFactory.factor()));
		}
		return Collections.singletonList(PasswordSystemSecretRemoverExtendedFunctionality
				.usingDependencyProvider(dependencyProvider));
	}
}
