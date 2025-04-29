/*
 * Copyright 2018, 2019, 2020, 2023, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.dependency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.decorator.DataChildDecoratorFactoryImp;
import se.uu.ub.cora.bookkeeper.decorator.DataDecoratorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollectorImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorFactoryImp;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactory;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageProvider;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollectorImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactory;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactoryImp;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.BasePermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.internal.SpiderAuthorizatorImp;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.cache.DataChangedSenderImp;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.data.internal.DataGroupToFilterImp;
import se.uu.ub.cora.spider.dependency.spy.DataValidatorFactoySpy;
import se.uu.ub.cora.spider.dependency.spy.MetadataStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordArchiveProviderSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordIdGeneratorProviderSpy;
import se.uu.ub.cora.spider.dependency.spy.StreamStorageProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderImp;
import se.uu.ub.cora.spider.extendedfunctionality.internal.FactorySorterImp;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactorySpy;
import se.uu.ub.cora.spider.role.RulesProviderImp;
import se.uu.ub.cora.spider.unique.UniqueValidatorImp;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.StreamStorageProvider;
import se.uu.ub.cora.storage.archive.ResourceArchiveProvider;
import se.uu.ub.cora.storage.idgenerator.RecordIdGeneratorProvider;
import se.uu.ub.cora.storage.spies.RecordStorageInstanceProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveInstanceProviderSpy;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DependencyProviderAbstractTest {
	private SpiderDependencyProviderTestHelper dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private RecordStorageInstanceProviderSpy recordStorageInstanceProvider;
	private MetadataStorageProviderSpy metadataStorageProvider;
	private Map<String, String> settings;
	private ResourceArchiveInstanceProviderSpy resourceArchiveInstanceProvider;

	@BeforeMethod
	public void beforeMethod() {
		SpiderDependencyProviderTestHelper.MRV = new MethodReturnValues();
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		settings = new HashMap<>();
		settings.put("foundKey", "someValue");
		SettingsProvider.setSettings(settings);

		dependencyProvider = new SpiderDependencyProviderTestHelper();

		setPluggedInStorageNormallySetByTheRestModuleStarterImp();
	}

	private void setPluggedInStorageNormallySetByTheRestModuleStarterImp() {
		recordStorageInstanceProvider = new RecordStorageInstanceProviderSpy();
		RecordStorageProvider
				.onlyForTestSetRecordStorageInstanceProvider(recordStorageInstanceProvider);

		resourceArchiveInstanceProvider = new ResourceArchiveInstanceProviderSpy();
		ResourceArchiveProvider.onlyForTestSetInstanceProvider(resourceArchiveInstanceProvider);

		StreamStorageProvider streamStorageProvider = new StreamStorageProviderSpy();
		dependencyProvider.setStreamStorageProvider(streamStorageProvider);
		RecordIdGeneratorProvider recordIdGeneratorProvider = new RecordIdGeneratorProviderSpy();
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProvider);
		metadataStorageProvider = new MetadataStorageProviderSpy();
		MetadataStorageProvider
				.onlyForTestSetMetadataStorageViewInstanceProvider(metadataStorageProvider);
	}

	@Test
	public void testGetExtendedFunctionalityProviderStartedOnCall() {
		ExtendedFunctionalityProviderImp extendedFunctionalityProvider = (ExtendedFunctionalityProviderImp) dependencyProvider
				.getExtendedFunctionalityProvider();

		assertNull(dependencyProvider.getExtendedFunctionalityProvider());
		dependencyProvider.initializeExtendedFunctionality();

		extendedFunctionalityProvider = (ExtendedFunctionalityProviderImp) dependencyProvider
				.getExtendedFunctionalityProvider();
		assertNotNull(dependencyProvider.getExtendedFunctionalityProvider());

		FactorySorterImp factorySorterNeededForTest = (FactorySorterImp) extendedFunctionalityProvider
				.getFactorySorterNeededForTest();
		assertSame(factorySorterNeededForTest.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testReadInitInfoIsCalledOnStartup() {
		assertTrue(dependencyProvider.readInitInfoWasCalled);
	}

	@Test
	public void testTryToInitializeIsCalledOnStartup() {
		assertTrue(dependencyProvider.tryToInitializeWasCalled);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ""
			+ "Error starting SpiderDependencyProviderTestHelper: some runtime error message")
	public void testStartupThrowsRuntimeException() {
		SpiderDependencyProviderTestHelper.MRV.setAlwaysThrowException("tryToInitialize",
				new RuntimeException("some runtime error message"));

		dependencyProvider = new SpiderDependencyProviderTestHelper();
	}

	@Test
	public void testStartupThrowsRuntimeExceptionInitialExceptionIsSentAlong() {
		SpiderDependencyProviderTestHelper.MRV.setAlwaysThrowException("tryToInitialize",
				new RuntimeException("some runtime error message"));
		try {
			dependencyProvider = new SpiderDependencyProviderTestHelper();
			fail("It should fail");
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof Exception);
		}
	}

	@Test
	public void testGetRecordStorage() {
		recordStorageInstanceProvider.MCR.assertReturn("getRecordStorage", 0,
				dependencyProvider.getRecordStorage());
	}

	@Test
	public void testGetResourceArchive() {
		resourceArchiveInstanceProvider.MCR.assertReturn("getResourceArchive", 0,
				dependencyProvider.getResourceArchive());
	}

	@Test
	public void testSetGetRecordArchive() {
		RecordArchiveProviderSpy recordArchiveProvider = new RecordArchiveProviderSpy();
		dependencyProvider.setRecordArchiveProvider(recordArchiveProvider);
		assertEquals(dependencyProvider.getRecordArchive(),
				recordArchiveProvider.getRecordArchive());
	}

	@Test
	public void testSetGetStreamStorage() {
		StreamStorageProviderSpy streamStorageProvider = new StreamStorageProviderSpy();
		dependencyProvider.setStreamStorageProvider(streamStorageProvider);
		assertEquals(dependencyProvider.getStreamStorage(),
				streamStorageProvider.getStreamStorage());
	}

	@Test
	public void testSetGetRecordIdGenerator() {
		RecordIdGeneratorProviderSpy recordIdGeneratorProvider = new RecordIdGeneratorProviderSpy();
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProvider);
		assertEquals(dependencyProvider.getRecordIdGenerator(),
				recordIdGeneratorProvider.getRecordIdGenerator());
	}

	@Test
	public void testExtendedFunctionalityProviderReturnsSame() {
		ExtendedFunctionalityProvider extendedFunctionalityProvider1 = dependencyProvider
				.getExtendedFunctionalityProvider();
		ExtendedFunctionalityProvider extendedFunctionalityProvider2 = dependencyProvider
				.getExtendedFunctionalityProvider();
		assertSame(extendedFunctionalityProvider1, extendedFunctionalityProvider2);
	}

	@Test
	public void testGetSpiderAuthorizator() {
		SpiderAuthorizatorImp spiderAuthorizator = (SpiderAuthorizatorImp) dependencyProvider
				.getSpiderAuthorizator();
		assertTrue(spiderAuthorizator instanceof SpiderAuthorizatorImp);
		assertSame(spiderAuthorizator.getDependencyProvider(), dependencyProvider);
		assertTrue(spiderAuthorizator.getAuthorizator() instanceof AuthorizatorImp);
		RulesProviderImp rulesProvider = (RulesProviderImp) spiderAuthorizator.getRulesProvider();
		assertTrue(rulesProvider instanceof RulesProviderImp);
		recordStorageInstanceProvider.MCR.assertReturn("getRecordStorage", 0,
				rulesProvider.onlyForTestGetRecordStorage());
	}

	@Test
	public void testDataValidatorHasCorrectDependecies() {
		DataValidator dataValidator = dependencyProvider.getDataValidator();

		DataValidatorFactoySpy dataValidatorFactorySpy = dependencyProvider.dataValidatorFactory;
		dataValidatorFactorySpy.MCR.assertParameters("factor", 0);
		dataValidatorFactorySpy.MCR.assertReturn("factor", 0, dataValidator);
	}

	@Test
	public void testGetDataValidatorFactory() {
		dependencyProvider.standardDataValidatorFactory = true;
		DataValidatorFactory dataValidatorFactory = dependencyProvider.getDataValidatorFactory();
		assertTrue(dataValidatorFactory instanceof DataValidatorFactoryImp);
	}

	@Test
	public void testGetDataRecordLinkCollector() {
		DataRecordLinkCollectorImp dataRecordLinkCollector = (DataRecordLinkCollectorImp) dependencyProvider
				.getDataRecordLinkCollector();

		assertTrue(dataRecordLinkCollector instanceof DataRecordLinkCollectorImp);
	}

	@Test
	public void testGetDataGroupTermCollector() {
		DataGroupTermCollectorImp dataGroupTermCollector = (DataGroupTermCollectorImp) dependencyProvider
				.getDataGroupTermCollector();

		assertTrue(dataGroupTermCollector instanceof DataGroupTermCollectorImp);
	}

	@Test
	public void testGetPermissionRuleCalculator() {
		PermissionRuleCalculator permissionRuleCalculator = dependencyProvider
				.getPermissionRuleCalculator();
		assertTrue(permissionRuleCalculator instanceof BasePermissionRuleCalculator);
	}

	@Test
	public void testDefaultRecordTypeHandlerFactoryIsImplFromBookkeeper() {
		RecordTypeHandlerFactory factory = dependencyProvider
				.useOriginalGetRecordTypeHandlerFactory();

		assertTrue(factory instanceof RecordTypeHandlerFactoryImp);
	}

	@Test
	public void testGetRecordTypeHandler() {
		String recordTypeId = "someRecordType";

		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordTypeId);

		RecordTypeHandlerFactorySpy typeHandlerFactorySpy = (RecordTypeHandlerFactorySpy) dependencyProvider.recordTypeHandlerFactory;

		typeHandlerFactorySpy.MCR.assertParameters("factorUsingRecordTypeId", 0, recordTypeId);
		typeHandlerFactorySpy.MCR.assertReturn("factorUsingRecordTypeId", 0, recordTypeHandler);
	}

	@Test
	public void testGetRecordTypeHandlerUsingDataRecordGroup() {
		DataRecordGroup dataRecordGroup = new DataRecordGroupSpy();

		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataRecordGroup);

		RecordTypeHandlerFactorySpy typeHandlerFactorySpy = (RecordTypeHandlerFactorySpy) dependencyProvider.recordTypeHandlerFactory;

		typeHandlerFactorySpy.MCR.assertParameters("factorUsingDataRecordGroup", 0,
				dataRecordGroup);
		typeHandlerFactorySpy.MCR.assertReturn("factorUsingDataRecordGroup", 0, recordTypeHandler);
	}

	@Test
	public void testGetDefaultDataRedactor() {
		assertTrue(dependencyProvider
				.useOriginalGetDataRedactorFactory() instanceof DataRedactorFactoryImp);
	}

	@Test
	public void testGetDataRedactor() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();

		dependencyProvider.dataRedactorFactorySpy.MCR.assertReturn("factor", 0, dataRedactor);
	}

	@Test
	public void testGetDataGroupToRecordEnhancer() {
		DataGroupToRecordEnhancerImp enhancer = (DataGroupToRecordEnhancerImp) dependencyProvider
				.getDataGroupToRecordEnhancer();
		assertSame(enhancer.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testDataGroupToFilterConverter() {
		DataGroupToFilter converter1 = dependencyProvider.getDataGroupToFilterConverter();
		DataGroupToFilter converter2 = dependencyProvider.getDataGroupToFilterConverter();
		assertNotNull(converter1);
		assertNotNull(converter2);
		assertTrue(converter1 instanceof DataGroupToFilterImp);
		assertTrue(converter2 instanceof DataGroupToFilterImp);
		assertNotSame(converter1, converter2);
	}

	@Test
	private void testGetUniqueValidator() {
		RecordStorageSpy recordStorage = new RecordStorageSpy();
		UniqueValidatorImp uniqueValidator = (UniqueValidatorImp) dependencyProvider
				.getUniqueValidator(recordStorage);

		assertNotNull(uniqueValidator);
		assertSame(uniqueValidator.onlyForTestGetRecordStorage(), recordStorage);
	}

	@Test
	public void testGetDataChangeSender() {
		DataChangedSender sender = dependencyProvider.getDataChangeSender();

		assertTrue(sender instanceof DataChangedSenderImp);
	}

	@Test
	public void testGetDataDecorator() {
		DataDecoratorImp dataDecorator = (DataDecoratorImp) dependencyProvider.getDataDecorator();

		assertTrue(dataDecorator instanceof DataDecoratorImp);
		assertTrue(dataDecorator
				.onlyForTestGetDataChildDecoratorFactory() instanceof DataChildDecoratorFactoryImp);

	}

}