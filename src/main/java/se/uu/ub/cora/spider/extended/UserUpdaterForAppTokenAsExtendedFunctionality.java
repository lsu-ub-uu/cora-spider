/*
 * Copyright 2017 Uppsala University Library
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

package se.uu.ub.cora.spider.extended;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.storage.RecordStorage;

public final class UserUpdaterForAppTokenAsExtendedFunctionality implements ExtendedFunctionality {
	private SpiderDependencyProvider dependencyProvider;
	private RecordStorage recordStorage;

	private UserUpdaterForAppTokenAsExtendedFunctionality(
			SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		recordStorage = dependencyProvider.getRecordStorage();
	}

	public static UserUpdaterForAppTokenAsExtendedFunctionality usingSpiderDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new UserUpdaterForAppTokenAsExtendedFunctionality(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(String authToken, SpiderDataGroup appTokenDataGroup) {
		SpiderDataGroup userAppTokenGroup = createUserAppTokenGroup(appTokenDataGroup);

		User user = getUserFromAuthToken(authToken);
		SpiderDataGroup spiderUserDataGroup = readUserFromStorage(user);
		spiderUserDataGroup.addChild(userAppTokenGroup);

		updateUserInStorage(authToken, user, spiderUserDataGroup);
	}

	private User getUserFromAuthToken(String authToken) {
		return dependencyProvider.getAuthenticator().getUserForToken(authToken);
	}

	private SpiderDataGroup readUserFromStorage(User user) {
		DataGroup userDataGroup = findUser(user.id);
		return SpiderDataGroup.fromDataGroup(userDataGroup);
	}

	private DataGroup findUser(String userId) {
		return recordStorage.read("user", userId);
	}

	private SpiderDataGroup createUserAppTokenGroup(SpiderDataGroup appTokenDataGroup) {
		SpiderDataGroup userAppTokenGroup = SpiderDataGroup.withNameInData("userAppTokenGroup");
		SpiderDataGroup appTokenLink = createAppTokenLink(appTokenDataGroup);
		userAppTokenGroup.addChild(appTokenLink);
		userAppTokenGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("note",
				appTokenDataGroup.extractAtomicValue("note")));
		userAppTokenGroup.setRepeatId(String.valueOf(System.nanoTime()));
		return userAppTokenGroup;
	}

	private SpiderDataGroup createAppTokenLink(SpiderDataGroup appTokenDataGroup) {
		SpiderDataGroup appTokenLink = SpiderDataGroup.withNameInData("appTokenLink");
		appTokenLink
				.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "appToken"));
		appTokenLink.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				appTokenDataGroup.extractGroup("recordInfo").extractAtomicValue("id")));
		return appTokenLink;
	}

	private void updateUserInStorage(String authToken, User user,
			SpiderDataGroup spiderUserDataGroup) {
		SpiderDataGroup recordInfo = spiderUserDataGroup.extractGroup("recordInfo");
		SpiderDataGroup type = recordInfo.extractGroup("type");
		String recordType = type.extractAtomicValue("linkedRecordId");

		SpiderRecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getSpiderRecordUpdater();
		spiderRecordUpdater.updateRecord(authToken, recordType, user.id, spiderUserDataGroup);
	}
}
