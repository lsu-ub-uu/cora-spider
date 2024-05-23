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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.RecordStorage;

public class PasswordSystemSecretRemoverExtendedFunctionality implements ExtendedFunctionality {

	public static PasswordSystemSecretRemoverExtendedFunctionality usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new PasswordSystemSecretRemoverExtendedFunctionality(dependencyProvider);
	}

	private SpiderDependencyProvider dependencyProvider;
	private DataGroup previousDataGroup;
	private DataGroup currentDataGroup;

	private PasswordSystemSecretRemoverExtendedFunctionality(
			SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		previousDataGroup = data.previouslyStoredTopDataGroup;
		currentDataGroup = data.dataGroup;
		if (systemSecretShouldBeRemoved()) {
			removeSystemSecret();
		}
	}

	private void removeSystemSecret() {
		DataRecordLink passwordLink = getPasswordLinkFromPreviousRecord();
		removeSystemSecretUsingLink(passwordLink);
	}

	private DataRecordLink getPasswordLinkFromPreviousRecord() {
		return previousDataGroup.getFirstChildOfTypeAndName(DataRecordLink.class, "passwordLink");
	}

	private void removeSystemSecretUsingLink(DataRecordLink passwordLink) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		recordStorage.deleteByTypeAndId(passwordLink.getLinkedRecordType(),
				passwordLink.getLinkedRecordId());
	}

	private boolean systemSecretShouldBeRemoved() {
		String previousUsePassword = previousDataGroup
				.getFirstAtomicValueWithNameInData("usePassword");
		String currentUsePassword = currentDataGroup
				.getFirstAtomicValueWithNameInData("usePassword");
		return "true".equals(previousUsePassword) && "false".equals(currentUsePassword);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
