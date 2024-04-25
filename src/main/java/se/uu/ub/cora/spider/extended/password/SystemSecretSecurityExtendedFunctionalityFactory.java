/*
 * Copyright 2024 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.SEARCH_AFTER_AUTHORIZATION;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class SystemSecretSecurityExtendedFunctionalityFactory
		implements ExtendedFunctionalityFactory {

	private List<ExtendedFunctionalityContext> contexts = new ArrayList<>();

	@Override
	public void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		for (ExtendedFunctionalityPosition position : ExtendedFunctionalityPosition.values()) {
			possiblyAddContextWhenPositionEndsWithAfterAuthorization(position);
		}
	}

	private void possiblyAddContextWhenPositionEndsWithAfterAuthorization(
			ExtendedFunctionalityPosition position) {
		if (endsWithAfterAuthorization(position)) {
			ExtendedFunctionalityContext systemSecretContext = createContextForRecordType(position);
			contexts.add(systemSecretContext);
		}
	}

	private boolean endsWithAfterAuthorization(ExtendedFunctionalityPosition position) {
		return position.toString().endsWith("_AFTER_AUTHORIZATION");
	}

	private ExtendedFunctionalityContext createContextForRecordType(
			ExtendedFunctionalityPosition position) {
		if (isRecordSearchAfterAuthorizationPosition(position)) {
			return new ExtendedFunctionalityContext(position, "search", 0);
		}
		return new ExtendedFunctionalityContext(position, "systemSecret", 0);
	}

	private boolean isRecordSearchAfterAuthorizationPosition(
			ExtendedFunctionalityPosition position) {
		return position.equals(SEARCH_AFTER_AUTHORIZATION);
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (isRecordSearchAfterAuthorizationPosition(position)) {
			return List.of(new SystemSecretSecurityForSearchExtendedFunctionality());
		}
		return List.of(new SystemSecretSecurityExtendedFunctionality());
	}

}