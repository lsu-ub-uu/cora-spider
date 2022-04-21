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
package se.uu.ub.cora.spider.password;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class PasswordExtendedFunctionality implements ExtendedFunctionality {

	private SpiderDependencyProvider dependencyProvider;
	private TextHasher textHasher;

	public static PasswordExtendedFunctionality usingDependencyProviderAndTextHasher(
			SpiderDependencyProvider dependencyProvider, TextHasher textHasher) {
		return new PasswordExtendedFunctionality(dependencyProvider, textHasher);
	}

	private PasswordExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			TextHasher textHasher) {
		this.dependencyProvider = dependencyProvider;
		this.textHasher = textHasher;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		if (plainTextPasswordIsSet(data)) {
			handleSetPassword(data);
			DataGroup systemSecret = DataProvider.createGroupUsingNameInData("systemSecret");

			DataGroup recordInfo = DataProvider.createGroupUsingNameInData("recordInfo");
			systemSecret.addChild(recordInfo);

			DataGroup usersRecordInfo = data.dataGroup.getFirstGroupWithNameInData("recordInfo");
			DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
					.getFirstChildWithNameInData("dataDivider");
			String usersDataDividerId = userDataDivider.getLinkedRecordId();

			DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("dataDivider", "system",
					usersDataDividerId);

			//
			// DataRecordGroup systemSecret =
			// DataProvider.createRecordGroupUsingNameInData("systemSecret");
			// systemSecret.setDataDivider("diva");
			// data.authToken
		}

	}

	// private void createRecord(String recordTypeToCreate, DataGroup dataGroupToCreate) {
	// RecordCreator spiderRecordCreatorOutput = SpiderInstanceProvider.getRecordCreator();
	// spiderRecordCreatorOutput.createAndStoreRecord(authToken, recordTypeToCreate,
	// dataGroupToCreate);
	// }
	private boolean plainTextPasswordIsSet(ExtendedFunctionalityData data) {
		return data.dataGroup.containsChildWithNameInData("plainTextPassword");
	}

	private void handleSetPassword(ExtendedFunctionalityData data) {
		String plainTextPassword = data.dataGroup
				.getFirstAtomicValueWithNameInData("plainTextPassword");
		data.dataGroup.removeAllChildrenWithNameInData("plainTextPassword");
		textHasher.hashText(plainTextPassword);
	}

	SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	TextHasher onlyForTestGetTextHasher() {
		return textHasher;
	}

}
