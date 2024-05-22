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
package se.uu.ub.cora.spider.extended.systemsecret;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
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

	private static final String SEARCH = "search";
	private static final String WORK_ORDER = "workOrder";
	private static final String SYSTEM_SECRET = "systemSecret";
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
			createContextForRecordType(position);
		}
	}

	private boolean endsWithAfterAuthorization(ExtendedFunctionalityPosition position) {
		return position.toString().endsWith("_AFTER_AUTHORIZATION");
	}

	private void createContextForRecordType(ExtendedFunctionalityPosition position) {
		if (position.equals(SEARCH_AFTER_AUTHORIZATION)) {
			contexts.add(new ExtendedFunctionalityContext(position, SEARCH, 0));
		} else if (position.equals(CREATE_AFTER_AUTHORIZATION)) {
			contexts.add(new ExtendedFunctionalityContext(position, WORK_ORDER, 0));
			contexts.add(new ExtendedFunctionalityContext(position, SYSTEM_SECRET, 0));
		} else {
			contexts.add(new ExtendedFunctionalityContext(position, SYSTEM_SECRET, 0));
		}
	}

	@Override
	public List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts() {
		return contexts;
	}

	@Override
	public List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position,
			String recordType) {
		if (position.equals(SEARCH_AFTER_AUTHORIZATION)) {
			return List.of(new SystemSecretSecurityForSearchExtendedFunctionality());
		}
		if (isWorkOrderAfterAuthorizationPosition(position, recordType)) {
			return List.of(new SystemSecretSecurityForWorkOrderExtendedFunctionality());
		}
		return List.of(new SystemSecretSecurityExtendedFunctionality());
	}

	private boolean isWorkOrderAfterAuthorizationPosition(ExtendedFunctionalityPosition position,
			String recordType) {
		return CREATE_AFTER_AUTHORIZATION.equals(position) && WORK_ORDER.equals(recordType);
	}

}