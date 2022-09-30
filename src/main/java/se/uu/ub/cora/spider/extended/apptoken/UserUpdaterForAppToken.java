/*
 * Copyright 2017, 2020, 2021, 2022 Uppsala University Library
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

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;

/**
 * UserUpdaterForAppToken updates the user record in storage for the current user with information
 * about the appToken that is currently beeing created. The added information consist of the note
 * information and a link to the appToken beeing created.
 */
public final class UserUpdaterForAppToken implements ExtendedFunctionality {
	private SpiderDependencyProvider dependencyProvider;
	private RecordStorage recordStorage;

	private UserUpdaterForAppToken(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		recordStorage = dependencyProvider.getRecordStorage();
	}

	public static UserUpdaterForAppToken usingSpiderDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new UserUpdaterForAppToken(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String authToken = data.authToken;
		DataGroup appTokenDataGroup = data.dataGroup;
		DataGroup userAppTokenGroup = createUserAppTokenGroup(appTokenDataGroup);

		String userId = data.user.id;
		DataGroup spiderUserDataGroup = readUserFromStorage(userId);
		spiderUserDataGroup.addChild(userAppTokenGroup);
		updateUserInStorage(authToken, userId, spiderUserDataGroup);
	}

	private DataGroup readUserFromStorage(String userId) {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler("user");
		return recordStorage.read(recordTypeHandler.getListOfRecordTypeIdsToReadFromStorage(),
				userId);
	}

	private DataGroup createUserAppTokenGroup(DataGroup appTokenDataGroup) {
		DataGroup userAppTokenGroup = DataProvider.createGroupUsingNameInData("userAppTokenGroup");

		DataRecordLink appTokenLink = createLinkPointingToHandledAppToken(appTokenDataGroup);
		userAppTokenGroup.addChild(appTokenLink);

		String note = appTokenDataGroup.getFirstAtomicValueWithNameInData("note");
		DataAtomic noteAtomic = DataProvider.createAtomicUsingNameInDataAndValue("note", note);
		userAppTokenGroup.addChild(noteAtomic);
		userAppTokenGroup.setRepeatId(String.valueOf(System.nanoTime()));
		return userAppTokenGroup;
	}

	private DataRecordLink createLinkPointingToHandledAppToken(DataGroup appTokenDataGroup) {
		DataGroup recordInfo = appTokenDataGroup.getFirstGroupWithNameInData("recordInfo");
		String id = recordInfo.getFirstAtomicValueWithNameInData("id");
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("appTokenLink", "appToken",
				id);
	}

	private void updateUserInStorage(String authToken, String userId,
			DataGroup spiderUserDataGroup) {
		DataGroup recordInfo = spiderUserDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink type = (DataRecordLink) recordInfo.getFirstChildWithNameInData("type");
		String recordType = type.getLinkedRecordId();

		RecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getRecordUpdater();
		spiderRecordUpdater.updateRecord(authToken, recordType, userId, spiderUserDataGroup);
	}

	SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
