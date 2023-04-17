/*
 * Copyright 2022 Uppsala University Library
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class PasswordExtendedFunctionalityFactory implements ExtendedFunctionalityFactory {

	List<ExtendedFunctionalityContext> contexts = new ArrayList<>();
	private SpiderDependencyProvider dependencyProvider;
	TextHasherFactory textHasherFactory = new TextHasherFactoryImp();

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

		return Collections
				.singletonList(PasswordExtendedFunctionality.usingDependencyProviderAndTextHasher(
						dependencyProvider, textHasherFactory.factor()));
	}

}
