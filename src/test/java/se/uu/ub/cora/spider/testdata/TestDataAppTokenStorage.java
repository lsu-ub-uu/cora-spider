/*
 * Copyright 2015, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorageInMemoryStub;

public class TestDataAppTokenStorage {

	public static RecordStorageInMemoryStub createRecordStorageInMemoryWithTestData() {
		RecordStorageInMemoryStub recordsInMemory = new RecordStorageInMemoryStub();
		addRecordType(recordsInMemory);
		addRecordTypeRecordType(recordsInMemory);
		addRecordTypeUser(recordsInMemory);
		addRecordTypeSystemOneUser(recordsInMemory);
		addRecordTypeSystemTwoUser(recordsInMemory);
		addRecordTypeWithParent(recordsInMemory);
		addRecordTypeAppToken(recordsInMemory);
		addRecordTypeImage(recordsInMemory);

		DataGroup dummyUser1 = DataGroup.withNameInData("user");
		recordsInMemory.create("systemOneUser", "dummy1", dummyUser1,
				null, DataGroup.withNameInData("collectedLinksList"), "systemOne");

		DataGroup dummyUser2 = DataGroup.withNameInData("user");
		recordsInMemory.create("systemTwoUser", "dummy2", dummyUser2,
				null, DataGroup.withNameInData("collectedLinksList"), "systemOne");

		// DataGroup dummyUser2 = DataGroup.withNameInData("user");
		// String inactiveUserJson =
		// "{\"name\":\"user\",\"children\":[{\"name\":\"recordInfo\",\"children\":[{\"name\":\"id\",\"value\":\"inactiveUser\"},{\"name\":\"type\",\"value\":\"systemTwoUser\"},{\"name\":\"createdBy\",\"value\":\"131313\"},{\"name\":\"dataDivider\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"system\"},{\"name\":\"linkedRecordId\",\"value\":\"systemOne\"}]}]},{\"name\":\"userId\",\"value\":\"dummy@ub.uu.se\"},{\"name\":\"userFirstname\",\"value\":\"Dummy\"},{\"name\":\"userLastname\",\"value\":\"Dumsson\"},{\"name\":\"userRole\",\"children\":[{\"name\":\"userRole\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"permissionRole\"},{\"name\":\"linkedRecordId\",\"value\":\"nothing\"}]},{\"name\":\"userRoleRulePart\",\"children\":[{\"name\":\"permissionRulePart\",\"children\":[{\"name\":\"permissionRulePartValue\",\"value\":\"system.\",\"repeatId\":\"0\"}],\"attributes\":{\"type\":\"organisation\"}}]}],\"repeatId\":\"0\"},{\"name\":\"activeStatus\",\"value\":\"inactive\"},{\"name\":\"userAppTokenGroup\",\"children\":[{\"name\":\"appTokenLink\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"appToken\"},{\"name\":\"linkedRecordId\",\"value\":\"appTokenJson\"}]},{\"name\":\"note\",\"value\":\"My
		// phone\"}],\"repeatId\":\"1\"}]}";
		// DataGroup inactiveUser =
		// convertJsonStringToDataGroup(inactiveUserJson);
		// recordsInMemory.create("systemTwoUser", "inactiveUser", inactiveUser,
		// DataGroup.withNameInData("collectedLinksList"), "systemTwo");

		// String noAppTokenUserJson =
		// "{\"name\":\"user\",\"children\":[{\"name\":\"recordInfo\",\"children\":[{\"name\":\"id\",\"value\":\"noAppTokenUser\"},{\"name\":\"type\",\"value\":\"systemTwoUser\"},{\"name\":\"createdBy\",\"value\":\"131313\"},{\"name\":\"dataDivider\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"system\"},{\"name\":\"linkedRecordId\",\"value\":\"systemOne\"}]}]},{\"name\":\"userId\",\"value\":\"dummy@ub.uu.se\"},{\"name\":\"userFirstname\",\"value\":\"Dummy\"},{\"name\":\"userLastname\",\"value\":\"Dumsson\"},{\"name\":\"userRole\",\"children\":[{\"name\":\"userRole\",\"children\":[{\"name\":\"linkedRecordType\",\"value\":\"permissionRole\"},{\"name\":\"linkedRecordId\",\"value\":\"nothing\"}]},{\"name\":\"userRoleRulePart\",\"children\":[{\"name\":\"permissionRulePart\",\"children\":[{\"name\":\"permissionRulePartValue\",\"value\":\"system.\",\"repeatId\":\"0\"}],\"attributes\":{\"type\":\"organisation\"}}]}],\"repeatId\":\"0\"},{\"name\":\"activeStatus\",\"value\":\"inactive\"}]}";
		// DataGroup noAppTokenUser =
		// convertJsonStringToDataGroup(noAppTokenUserJson);
		// recordsInMemory.create("systemTwoUser", "noAppTokenUser",
		// noAppTokenUser,
		// DataGroup.withNameInData("collectedLinksList"), "systemTwo");

		return recordsInMemory;
	}

	private static void addRecordType(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withNameInData(recordType);

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(recordType,
				"metadata");
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
		recordsInMemory.create(recordType, "metadata", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeRecordType(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("recordType", "true", "false", "false");
		recordsInMemory.create(recordType, "recordType", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeImage(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndParentId("image", "true", "binary");
		recordsInMemory.create(recordType, "image", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeUser(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("user",
				"true", "true", "false");
		recordsInMemory.create(recordType, "user", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeSystemOneUser(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(
				"systemOneUser", "true", "user");
		recordsInMemory.create(recordType, "systemOneUser", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeSystemTwoUser(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(
				"systemTwoUser", "true", "user");
		recordsInMemory.create(recordType, "systemTwoUser", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeWithParent(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(
				"systemTwoUser", "true", "user");
		recordsInMemory.create(recordType, "recordTypeWithParent", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");
	}

	private static void addRecordTypeAppToken(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("appToken", "false", "false", "false");
		recordsInMemory.create(recordType, "appToken", dataGroup,
				null, DataGroup.withNameInData("collectedLinksList"), "cora");

	}
}
