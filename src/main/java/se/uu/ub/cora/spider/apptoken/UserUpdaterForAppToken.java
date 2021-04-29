/*
 * Copyright 2017, 2020, 2021 Uppsala University Library
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

package se.uu.ub.cora.spider.apptoken;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.storage.RecordStorage;

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
	public void useExtendedFunctionality(String authToken, DataGroup appTokenDataGroup) {
		DataGroup userAppTokenGroup = createUserAppTokenGroup(appTokenDataGroup);

		String userId = getUserIdFromAuthToken(authToken);
		DataGroup spiderUserDataGroup = readUserFromStorage(userId);
		spiderUserDataGroup.addChild(userAppTokenGroup);

		updateUserInStorage(authToken, userId, spiderUserDataGroup);
	}

	private String getUserIdFromAuthToken(String authToken) {
		Authenticator authenticator = dependencyProvider.getAuthenticator();
		User userForToken = authenticator.getUserForToken(authToken);
		return userForToken.id;
	}

	private DataGroup readUserFromStorage(String userId) {
		return recordStorage.read("user", userId);
	}

	private DataGroup createUserAppTokenGroup(DataGroup appTokenDataGroup) {
		DataGroup userAppTokenGroup = DataGroupProvider
				.getDataGroupUsingNameInData("userAppTokenGroup");
		DataGroup appTokenLink = createAppTokenLink(appTokenDataGroup);
		userAppTokenGroup.addChild(appTokenLink);
		userAppTokenGroup.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("note",
				appTokenDataGroup.getFirstAtomicValueWithNameInData("note")));
		userAppTokenGroup.setRepeatId(String.valueOf(System.nanoTime()));
		return userAppTokenGroup;
	}

	private DataGroup createAppTokenLink(DataGroup appTokenDataGroup) {
		DataGroup appTokenLink = DataGroupProvider.getDataGroupUsingNameInData("appTokenLink");
		appTokenLink.addChild(DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("linkedRecordType", "appToken"));
		appTokenLink.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(
				"linkedRecordId", appTokenDataGroup.getFirstGroupWithNameInData("recordInfo")
						.getFirstAtomicValueWithNameInData("id")));
		return appTokenLink;
	}

	private void updateUserInStorage(String authToken, String userId,
			DataGroup spiderUserDataGroup) {
		DataGroup recordInfo = spiderUserDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup type = recordInfo.getFirstGroupWithNameInData("type");
		String recordType = type.getFirstAtomicValueWithNameInData("linkedRecordId");

		RecordUpdater spiderRecordUpdater = SpiderInstanceProvider
				.getSpiderRecordUpdater(recordType);
		spiderRecordUpdater.updateRecord(authToken, recordType, userId, spiderUserDataGroup);
	}

	public SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}
}
