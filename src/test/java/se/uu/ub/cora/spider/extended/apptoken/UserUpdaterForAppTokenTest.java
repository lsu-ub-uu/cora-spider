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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataFactory;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLinkProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.DataRecordLinkFactorySpy;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.testspies.data.DataFactorySpy;

public class UserUpdaterForAppTokenTest {

	private UserUpdaterForAppToken extendedFunctionality;

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	private SpiderInstanceFactorySpy2 spiderInstanceFactory;
	private DataFactory dataFactory;

	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactory dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		DataRecordLinkProvider.setDataRecordLinkFactory(new DataRecordLinkFactorySpy());

		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

		dependencyProvider = new SpiderDependencyProviderSpy(Collections.emptyMap());
		authenticator = new AuthenticatorSpy();
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		extendedFunctionality = UserUpdaterForAppToken
				.usingSpiderDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;

		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
	}

	@Test
	public void init() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void useExtendedFunctionality() {
		DataGroup minimalAppTokenGroup = new DataGroupSpy("appToken");
		minimalAppTokenGroup.addChild(
				DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("appToken",
						"someAppTokenId", "cora"));
		minimalAppTokenGroup.addChild(new DataAtomicSpy("note", "my device!"));

		callExtendedFunctionalityWithGroup(minimalAppTokenGroup);

		SpiderRecordUpdaterSpy spiderRecordUpdaterSpy = spiderInstanceFactory.createdUpdaters
				.get(0);
		DataGroup updatedUserDataGroup = spiderRecordUpdaterSpy.record;
		DataGroup userAppTokenGroup = (DataGroup) updatedUserDataGroup
				.getFirstChildWithNameInData("userAppTokenGroup");
		assertEquals(userAppTokenGroup.getFirstAtomicValueWithNameInData("note"), "my device!");
		DataGroup apptokenLink = userAppTokenGroup.getFirstGroupWithNameInData("appTokenLink");
		assertEquals(apptokenLink.getFirstAtomicValueWithNameInData("linkedRecordType"),
				"appToken");
		assertEquals(apptokenLink.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"someAppTokenId");
		assertNotNull(userAppTokenGroup.getRepeatId());
	}

	private void callExtendedFunctionalityWithGroup(DataGroup minimalGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "dummy1Token";
		data.dataGroup = minimalGroup;
		extendedFunctionality.useExtendedFunctionality(data);
	}
}