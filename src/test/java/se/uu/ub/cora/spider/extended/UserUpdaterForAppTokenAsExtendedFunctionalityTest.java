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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.storage.RecordStorage;

public class UserUpdaterForAppTokenAsExtendedFunctionalityTest {

	private UserUpdaterForAppTokenAsExtendedFunctionality extendedFunctionality;

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	private SpiderInstanceFactorySpy2 spiderInstanceFactory;

	@BeforeMethod
	public void setUp() {
		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

		dependencyProvider = new SpiderDependencyProviderSpy(Collections.emptyMap());
		authenticator = new AuthenticatorSpy();
		// recordStorage =
		// TestDataAppTokenStorage.createRecordStorageInMemoryWithTestData();
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		extendedFunctionality = UserUpdaterForAppTokenAsExtendedFunctionality
				.usingSpiderDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);

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
		SpiderDataGroup minimalAppTokenGroup = SpiderDataGroup.withNameInData("appToken");
		minimalAppTokenGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						"appToken", "someAppTokenId", "cora"));
		minimalAppTokenGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("note", "my device!"));

		extendedFunctionality.useExtendedFunctionality("dummy1Token", minimalAppTokenGroup);
		SpiderRecordUpdaterSpy spiderRecordUpdaterSpy = spiderInstanceFactory.createdUpdaters
				.get(0);
		SpiderDataGroup updatedUserDataGroup = spiderRecordUpdaterSpy.record;
		SpiderDataGroup userAppTokenGroup = (SpiderDataGroup) updatedUserDataGroup
				.getFirstChildWithNameInData("userAppTokenGroup");
		assertEquals(userAppTokenGroup.extractAtomicValue("note"), "my device!");
		SpiderDataGroup apptokenLink = userAppTokenGroup.extractGroup("appTokenLink");
		assertEquals(apptokenLink.extractAtomicValue("linkedRecordType"), "appToken");
		assertEquals(apptokenLink.extractAtomicValue("linkedRecordId"), "someAppTokenId");
		assertNotNull(userAppTokenGroup.getRepeatId());
	}

}
