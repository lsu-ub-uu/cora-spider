/*
 * Copyright 2018, 2019, 2020 Uppsala University Library
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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollectorImp;
import se.uu.ub.cora.bookkeeper.metadata.MetadataElement;
import se.uu.ub.cora.bookkeeper.metadata.MetadataHolder;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupRedactorImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupWrapperFactoryImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorImp;
import se.uu.ub.cora.bookkeeper.recordpart.MatcherFactoryImp;
import se.uu.ub.cora.bookkeeper.termcollector.CollectedDataCreatorImp;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollectorImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactoryImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorImp;
import se.uu.ub.cora.bookkeeper.validator.MetadataMatchDataImp;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.BasePermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizatorImp;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderImp;
import se.uu.ub.cora.spider.extendedfunctionality.internal.FactorySorterImp;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactory;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerImp;
import se.uu.ub.cora.spider.role.RulesProviderImp;
import se.uu.ub.cora.storage.MetadataStorageProvider;
import se.uu.ub.cora.storage.RecordIdGeneratorProvider;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorageProvider;

public class DependencyProviderAbstractTest {

	private Map<String, String> initInfo;
	private SpiderDependencyProviderTestHelper dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "DependencyProviderAbstract";
	private RecordStorageProviderSpy recordStorageProvider;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		initInfo = new HashMap<>();
		initInfo.put("foundKey", "someValue");
		dependencyProvider = new SpiderDependencyProviderTestHelper(initInfo);
		setPluggedInStorageNormallySetByTheRestModuleStarterImp();
	}

	private void setPluggedInStorageNormallySetByTheRestModuleStarterImp() {
		recordStorageProvider = new RecordStorageProviderSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProvider);
		StreamStorageProvider streamStorageProvider = new StreamStorageProviderSpy();
		dependencyProvider.setStreamStorageProvider(streamStorageProvider);
		RecordIdGeneratorProvider recordIdGeneratorProvider = new RecordIdGeneratorProviderSpy();
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProvider);
		MetadataStorageProvider metadataStorageProvider = new MetadataStorageProviderSpy();
		dependencyProvider.setMetadataStorageProvider(metadataStorageProvider);
	}

	@Test
	public void testInitInfoIsSetOnStartup() {
		assertEquals(dependencyProvider.getInitInfoFromParent("foundKey"), "someValue");
	}

	@Test
	public void testGetExtendedFunctionalityProviderStartedOnCall() throws Exception {
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
	public void testReadInitInfoIsCalledOnStartup() throws Exception {
		assertTrue(dependencyProvider.readInitInfoWasCalled);
	}

	@Test
	public void testTryToInitializeIsCalledOnStartup() throws Exception {
		assertTrue(dependencyProvider.tryToInitializeWasCalled);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ""
			+ "Error starting SpiderDependencyProviderTestHelper: some runtime error message")
	public void testStartupThrowsRuntimeException() throws Exception {
		initInfo.put("runtimeException", "some runtime error message");
		dependencyProvider = new SpiderDependencyProviderTestHelper(initInfo);
	}

	@Test
	public void testStartupThrowsRuntimeExceptionInitialExceptionIsSentAlong() throws Exception {
		initInfo.put("runtimeException", "some runtime error message");
		try {
			dependencyProvider = new SpiderDependencyProviderTestHelper(initInfo);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof Exception);
		}
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ""
			+ "Error starting SpiderDependencyProviderTestHelper: some invocation target error message")
	public void testStartupThrowsInvocationTargetException() throws Exception {
		initInfo.put("invocationTargetException", "some invocation target error message");
		dependencyProvider = new SpiderDependencyProviderTestHelper(initInfo);
	}

	@Test
	public void testStartupThrowsInvocationTargetExceptionInitialExceptionIsSentAlong()
			throws Exception {
		initInfo.put("invocationTargetException", "some invocation target error message");
		try {
			dependencyProvider = new SpiderDependencyProviderTestHelper(initInfo);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof Exception);
		}
	}

	@Test
	public void testSetGetRecordStorage() {
		RecordStorageProviderSpy recordStorageProvider = new RecordStorageProviderSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProvider);
		assertEquals(dependencyProvider.getRecordStorage(),
				recordStorageProvider.getRecordStorage());
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
	public void testExtendedFunctionalityProviderReturnsSame() throws Exception {
		ExtendedFunctionalityProvider extendedFunctionalityProvider1 = dependencyProvider
				.getExtendedFunctionalityProvider();
		ExtendedFunctionalityProvider extendedFunctionalityProvider2 = dependencyProvider
				.getExtendedFunctionalityProvider();
		assertSame(extendedFunctionalityProvider1, extendedFunctionalityProvider2);
	}

	@Test
	public void testSetMetadataStorage() {
		MetadataStorageProviderSpy metadataStorageProvider = new MetadataStorageProviderSpy();
		dependencyProvider.setMetadataStorageProvider(metadataStorageProvider);
		assertEquals(dependencyProvider.getMetadataStorage(),
				metadataStorageProvider.getMetadataStorage());
	}

	@Test
	public void testEnsureKeyExistsInInitInfoDoesNotLogInfoForFoundKey() {
		String key = "foundKey";
		dependencyProvider.ensureKeyExistsInInitInfo(key);
		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 0);
	}

	@Test
	public void testEnsureKeyExistsInInitInfoLoggsError() {
		String key = "nonExistingKey";
		boolean exceptionCaught = false;
		try {
			dependencyProvider.ensureKeyExistsInInitInfo(key);
		} catch (Exception e) {
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"InitInfo in SpiderDependencyProviderTestHelper must contain: " + key);
	}

	@Test(expectedExceptions = SpiderInitializationException.class, expectedExceptionsMessageRegExp = ""
			+ "InitInfo in SpiderDependencyProviderTestHelper must contain: nonExistingKey")
	public void testEnsureKeyExistsInInitInfoThrowsError() {
		String key = "nonExistingKey";
		dependencyProvider.ensureKeyExistsInInitInfo(key);
		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"InitInfo in SpiderDependencyProviderTestHelper must contain: " + key);
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
		assertSame(rulesProvider.getRecordStorage(), dependencyProvider.getRecordStorage());
	}

	@Test
	public void testDataValidatorHasCorrectDependecies() {
		MetadataStorageSpy metadataStorage = (MetadataStorageSpy) dependencyProvider
				.getMetadataStorage();

		DataValidatorImp dataValidator = (DataValidatorImp) dependencyProvider.getDataValidator();
		assertTrue(dataValidator instanceof DataValidatorImp);
		assertSame(dataValidator.getMetadataStorage(), dependencyProvider.getMetadataStorage());
		assertCorrectRecordTypeHolder(dataValidator.getRecordTypeHolder(), metadataStorage);

		DataValidatorFactoryImp dataValidatorFactory = (DataValidatorFactoryImp) dataValidator
				.getDataValidatorFactory();

		assertTrue(metadataStorage.getMetadataElementsWasCalled);

		assertCorrectRecordTypeHolder(dataValidatorFactory.getRecordTypeHolder(), metadataStorage);
		assertCorrectMetadataHolder(dataValidatorFactory);

	}

	private void assertCorrectMetadataHolder(DataValidatorFactoryImp dataValidatorFactory) {
		MetadataHolder metadataHolder = dataValidatorFactory.getMetadataHolder();

		MetadataElement metadataElement = metadataHolder.getMetadataElement("someMetadata1");
		assertEquals(metadataElement.getId(), "someMetadata1");
	}

	private void assertCorrectRecordTypeHolder(Map<String, DataGroup> recordTypeHolder,
			MetadataStorageSpy metadataStorage) {
		assertEquals(recordTypeHolder.get("someId1"), metadataStorage.recordTypes.get(0));
		assertEquals(recordTypeHolder.get("someId2"), metadataStorage.recordTypes.get(1));
	}

	@Test
	public void testGetDataRecordLinkCollector() {
		DataRecordLinkCollectorImp dataRecordLinkCollector = (DataRecordLinkCollectorImp) dependencyProvider
				.getDataRecordLinkCollector();
		assertTrue(dataRecordLinkCollector instanceof DataRecordLinkCollectorImp);
		assertSame(dataRecordLinkCollector.getMetadataStorage(),
				dependencyProvider.getMetadataStorage());
	}

	@Test
	public void testGetDataGroupTermCollector() {
		DataGroupTermCollectorImp dataGroupTermCollector = (DataGroupTermCollectorImp) dependencyProvider
				.getDataGroupTermCollector();
		assertTrue(dataGroupTermCollector instanceof DataGroupTermCollectorImp);
		assertSame(dataGroupTermCollector.getMetadataStorage(),
				dependencyProvider.getMetadataStorage());
		assertTrue(dataGroupTermCollector
				.getCollectedDataCreator() instanceof CollectedDataCreatorImp);
	}

	@Test
	public void testGetPermissionRuleCalculator() {
		PermissionRuleCalculator permissionRuleCalculator = dependencyProvider
				.getPermissionRuleCalculator();
		assertTrue(permissionRuleCalculator instanceof BasePermissionRuleCalculator);
	}

	@Test
	public void testGetRecordTypeHandler() throws Exception {
		String recordTypeId = "someRecordType";
		RecordTypeHandlerImp recordTypeHandler = (RecordTypeHandlerImp) ((SpiderDependencyProvider) dependencyProvider)
				.getRecordTypeHandler(recordTypeId);
		assertTrue(recordTypeHandler instanceof RecordTypeHandler);
		assertEquals(recordTypeHandler.getRecordTypeId(), recordTypeId);
		assertTrue(recordTypeHandler.getRecordStorage() instanceof RecordStorage);
		assertSame(recordStorageProvider.getRecordStorage(), recordTypeHandler.getRecordStorage());
		RecordTypeHandlerFactoryImp recordTypeHandlerFactory = (RecordTypeHandlerFactoryImp) recordTypeHandler
				.getRecordTypeHandlerFactory();
		assertTrue(recordTypeHandlerFactory instanceof RecordTypeHandlerFactory);
		assertSame(recordTypeHandlerFactory.getStorage(), recordTypeHandler.getRecordStorage());
	}

	@Test
	public void testGetDataRedactor() {
		DataRedactorImp dataRedactor = (DataRedactorImp) dependencyProvider.getDataRedactor();
		MetadataHolder metadataHolder = dataRedactor.getMetadataHolder();

		MetadataElement metadataElement = metadataHolder.getMetadataElement("someMetadata1");
		assertEquals(metadataElement.getId(), "someMetadata1");
		assertTrue(dataRedactor.getDataGroupRedactor() instanceof DataGroupRedactorImp);
		assertTrue(dataRedactor.getDataGroupWrapperFactory() instanceof DataGroupWrapperFactoryImp);

		MatcherFactoryImp matcherFactory = (MatcherFactoryImp) dataRedactor.getMatcherFactory();
		MetadataMatchDataImp metadataMatchData = (MetadataMatchDataImp) matcherFactory
				.getMetadataMatchData();
		assertSame(metadataMatchData.getMetadataHolder(), metadataHolder);
	}

	@Test
	public void testGetValueFromInitInfo() {
		String key = "foundKey";
		String value = dependencyProvider.getInitInfoValueUsingKey(key);
		assertEquals(value, "someValue");
	}

	@Test(expectedExceptions = SpiderInitializationException.class)
	public void testGetValueFromInitInfoKeyDoesNotExist() {
		String key = "NOTfoundKey";
		dependencyProvider.getInitInfoValueUsingKey(key);
	}

	@Test
	public void testGetDataGroupToRecordEnhancer() throws Exception {
		DataGroupToRecordEnhancerImp enhancer = (DataGroupToRecordEnhancerImp) dependencyProvider
				.getDataGroupToRecordEnhancer();
		assertSame(enhancer.getDependencyProvider(), dependencyProvider);
	}
}
