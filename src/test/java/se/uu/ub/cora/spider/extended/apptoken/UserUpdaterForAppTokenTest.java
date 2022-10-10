/*
 * Copyright 2017, 2019, 2022 Uppsala University Library
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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.RecordStorage;

public class UserUpdaterForAppTokenTest {

	private UserUpdaterForAppToken extendedFunctionality;

	private SpiderDependencyProviderSpy dependencyProvider;

	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private DataFactorySpy dataFactory;

	private DataGroupSpy userGroupFromStorage;

	private RecordTypeHandlerSpy recordTypeHandler;

	@BeforeMethod
	public void setUp() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);
		dependencyProvider = new SpiderDependencyProviderSpy();

		setUpRecordTypeHandler();

		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
		userGroupFromStorage = setupReturnUserFromStorage();

		extendedFunctionality = UserUpdaterForAppToken
				.usingSpiderDependencyProvider(dependencyProvider);
	}

	private void setUpRecordTypeHandler() {
		recordTypeHandler = new RecordTypeHandlerSpy();
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier(
				"getListOfRecordTypeIdsToReadFromStorage",
				(Supplier<List<String>>) () -> List.of("user"));
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				(Supplier<RecordTypeHandler>) () -> recordTypeHandler);

	}

	@Test
	public void init() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void testUserAppTokenGroupToBeAddedToUserCreatedCorrectly() {
		DataGroupSpy tokenGroup = setUpAppTokenGroupWithTypeAndIdAndNote("someType", "someId",
				"some note");
		long before = System.nanoTime();

		callExtendedFunctionalityWithGroup(tokenGroup);

		long after = System.nanoTime();
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, "userAppTokenGroup");
		DataGroupSpy userAppTokenGroup = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		DataRecordLinkSpy appTokenLink = assertAndFetchLinkPointsToHandledAppToken(tokenGroup);
		userAppTokenGroup.MCR.assertParameters("addChild", 0, appTokenLink);

		DataAtomicSpy noteGroup = assertAndFetchNoteCreatedFromHandledAppToken(tokenGroup);
		userAppTokenGroup.MCR.assertParameters("addChild", 1, noteGroup);

		assertRepeatIdIsSetToTime(before, after, userAppTokenGroup);
	}

	private void assertRepeatIdIsSetToTime(long before, long after,
			DataGroupSpy userAppTokenGroup) {
		userAppTokenGroup.MCR.assertMethodWasCalled("setRepeatId");
		String repeatId = (String) userAppTokenGroup.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("setRepeatId", 0, "repeatId");
		long created = Long.valueOf(repeatId);
		assertTrue(before < created);
		assertTrue(created < after);
	}

	private DataAtomicSpy assertAndFetchNoteCreatedFromHandledAppToken(DataGroupSpy tokenGroup) {
		tokenGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "note");
		String note = (String) tokenGroup.MCR.getReturnValue("getFirstAtomicValueWithNameInData",
				0);
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "note", note);
		DataAtomicSpy noteGroup = (DataAtomicSpy) dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		return noteGroup;
	}

	private DataRecordLinkSpy assertAndFetchLinkPointsToHandledAppToken(DataGroupSpy tokenGroup) {
		tokenGroup.MCR.assertParameters("getFirstGroupWithNameInData", 0, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) tokenGroup.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);
		recordInfo.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "id");
		String id = (String) recordInfo.MCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);

		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"appTokenLink", "appToken", id);
		return (DataRecordLinkSpy) dataFactory.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
	}

	private DataGroupSpy setUpAppTokenGroupWithTypeAndIdAndNote(String type, String id,
			String note) {
		DataGroupSpy appToken = new DataGroupSpy();

		DataGroupSpy recordInfo = new DataGroupSpy();
		appToken.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfo, "recordInfo");
		recordInfo.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(id), "id");

		appToken.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(note), "note");
		return appToken;
	}

	private void callExtendedFunctionalityWithGroup(DataGroup minimalGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someAuthToken";
		data.dataGroup = minimalGroup;
		User user = new User("someUserId");
		data.user = user;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testUserUpdatedInStorageWithAddedAppTokenGroup() {
		DataGroupSpy tokenGroup = setUpAppTokenGroupWithTypeAndIdAndNote("someType", "someId",
				"some note");
		callExtendedFunctionalityWithGroup(tokenGroup);

		DataGroupSpy userAppTokenGroup = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.MCR
				.getReturnValue("getRecordStorage", 0);

		var types = recordTypeHandler.MCR.getReturnValue("getListOfRecordTypeIdsToReadFromStorage",
				0);
		recordStorage.MCR.assertParameters("read", 0, types, "someUserId");

		DataGroupSpy userFromStorage = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		userFromStorage.MCR.assertParameters("addChild", 0, userAppTokenGroup);

		spiderInstanceFactory.MCR.assertParameters("factorRecordUpdater", 0);
		RecordUpdaterSpy recordUpdater = (RecordUpdaterSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordUpdater", 0);

		userGroupFromStorage.MCR.assertParameters("getFirstGroupWithNameInData", 0, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) userGroupFromStorage.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);
		recordInfo.MCR.assertParameters("getFirstChildWithNameInData", 0, "type");
		DataRecordLinkSpy typeLink = (DataRecordLinkSpy) recordInfo.MCR
				.getReturnValue("getFirstChildWithNameInData", 0);

		typeLink.MCR.assertParameters("getLinkedRecordId", 0);
		String type = (String) typeLink.MCR.getReturnValue("getLinkedRecordId", 0);

		recordUpdater.MCR.assertParameters("updateRecord", 0, "someAuthToken", type, "someUserId",
				userGroupFromStorage);
	}

	private DataGroupSpy setupReturnUserFromStorage() {
		DataGroupSpy user = new DataGroupSpy();
		DataGroupSpy recordInfo = new DataGroupSpy();
		user.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfo, "recordInfo");
		DataRecordLinkSpy typeLink = new DataRecordLinkSpy();
		recordInfo.MRV.setReturnValues("getFirstChildWithNameInData", List.of(typeLink), "type");
		typeLink.MRV.setReturnValues("getLinkedRecordId", List.of("someType"));

		RecordStorageSpy recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				(Supplier<RecordStorage>) () -> recordStorage);

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", (Supplier<DataGroup>) () -> user);
		return user;
	}

}
