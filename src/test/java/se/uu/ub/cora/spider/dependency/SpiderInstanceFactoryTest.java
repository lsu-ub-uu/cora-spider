/*
 * Copyright 2016 Olov McKie
 * Copyright 2015, 2019, 2025 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.binary.iiif.internal.IiifReaderImp;
import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryType;
import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryTypeImp;
import se.uu.ub.cora.spider.binary.internal.UploaderImp;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.index.internal.BatchRunnerFactoryImp;
import se.uu.ub.cora.spider.index.internal.DataRecordGroupHandlerForIndexBatchJob;
import se.uu.ub.cora.spider.index.internal.IndexBatchHandlerImp;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordReaderDecorated;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.record.internal.RecordCreatorImp;
import se.uu.ub.cora.spider.record.internal.RecordListIndexerImp;
import se.uu.ub.cora.spider.record.internal.RecordReaderDecoratedImp;
import se.uu.ub.cora.spider.record.internal.RecordSearcherImp;
import se.uu.ub.cora.spider.record.internal.RecordValidatorImp;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvertImp;

public class SpiderInstanceFactoryTest {
	private SpiderInstanceFactory factory;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void setUp() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		Map<String, String> settingsMap = new HashMap<>();
		SettingsProvider.setSettings(settingsMap);
		settingsMap.put("rabbitMqHostname", "someRabbitHostname");
		settingsMap.put("rabbitMqPort", "1234");
		settingsMap.put("rabbitMqVirtualHost", "someVHost");
		settingsMap.put("rabbitMqImageExchange", "someImageExchange");
		settingsMap.put("rabbitMqPdfExchange", "somePdfExchange");
		settingsMap.put("rabbitMqRoutingKey", "someRoutingKey");

		dependencyProvider = new SpiderDependencyProviderOldSpy();

		factory = SpiderInstanceFactoryImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testGetDependencyProviderClassName() {
		assertEquals(factory.getDependencyProviderClassName(),
				dependencyProvider.getClass().getName());
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordReader() {
		RecordReader recordReader = factory.factorRecordReader();
		RecordReader recordReader2 = factory.factorRecordReader();
		assertNotNull(recordReader);
		assertNotNull(recordReader2);
		assertNotSame(recordReader, recordReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfDecoratorRecordReader() {
		RecordReaderDecorated recordReader = factory.factorRecordReaderDecorated();
		RecordReaderDecorated recordReader2 = factory.factorRecordReaderDecorated();
		assertNotSame(recordReader, recordReader2);
		assertDependencyProviderIsPassed(recordReader, recordReader2);
	}

	private void assertDependencyProviderIsPassed(RecordReaderDecorated... recordReaders) {
		for (var decoratedRecordReader : recordReaders) {
			assertEquals(((RecordReaderDecoratedImp) decoratedRecordReader)
					.onlyForTestGetDependencyProvider(), dependencyProvider);
		}
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordIncomingLinksReader() {
		IncomingLinksReader recordIncomingLInksReader = factory.factorIncomingLinksReader();
		IncomingLinksReader recordIncomingLInksReader2 = factory.factorIncomingLinksReader();
		assertNotNull(recordIncomingLInksReader);
		assertNotNull(recordIncomingLInksReader2);
		assertNotSame(recordIncomingLInksReader, recordIncomingLInksReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordListReader() {
		RecordListReader recordListReader = factory.factorRecordListReader();
		RecordListReader recordListReader2 = factory.factorRecordListReader();
		assertNotNull(recordListReader);
		assertNotNull(recordListReader2);
		assertNotSame(recordListReader, recordListReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordCreator() {
		RecordCreator recordCreator = factory.factorRecordCreator();
		RecordCreator recordCreator2 = factory.factorRecordCreator();
		assertNotNull(recordCreator);
		assertNotNull(recordCreator2);
		assertNotSame(recordCreator, recordCreator2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordUpdater() {
		RecordUpdater recordUpdater = factory.factorRecordUpdater();
		RecordUpdater recordUpdater2 = factory.factorRecordUpdater();
		assertNotNull(recordUpdater);
		assertNotNull(recordUpdater2);
		assertNotSame(recordUpdater, recordUpdater2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordDeleter() {
		RecordDeleter recordDeleter = factory.factorRecordDeleter();
		RecordDeleter recordDeleter2 = factory.factorRecordDeleter();
		assertNotNull(recordDeleter);
		assertNotNull(recordDeleter2);
		assertNotSame(recordDeleter, recordDeleter2);
	}

	@Test
	public void testFactorUploader() {
		UploaderImp recordUploader = (UploaderImp) factory.factorUploader();

		SpiderDependencyProvider passedDependencyProvider = recordUploader
				.onlyForTestGetDependecyProvider();
		assertEquals(passedDependencyProvider, dependencyProvider);

		ResourceConvertImp resouceConvert = (ResourceConvertImp) recordUploader
				.onlyForTestGetResourceConvert();
		assertTrue(resouceConvert instanceof ResourceConvertImp);

		MimeTypeToBinaryType mimeTypeToBinaryType = recordUploader
				.onlyForTestGetMimeTypeToBinaryTypeConvert();
		assertTrue(mimeTypeToBinaryType instanceof MimeTypeToBinaryTypeImp);

		assertEquals(resouceConvert.onlyForTestGetHostName(), "someRabbitHostname");
		assertEquals(resouceConvert.onlyForTestGetPort(), 1234);
		assertEquals(resouceConvert.onlyForTestGetVirtualHost(), "someVHost");
		assertEquals(resouceConvert.onlyForTestGetImageExchange(), "someImageExchange");
		assertEquals(resouceConvert.onlyForTestGetPdfExchange(), "somePdfExchange");
		assertEquals(resouceConvert.onlyForTestGetRoutingKey(), "someRoutingKey");
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfUploader() {
		Uploader recordUploader = factory.factorUploader();
		Uploader recordUploader2 = factory.factorUploader();
		assertNotNull(recordUploader);
		assertNotNull(recordUploader2);
		assertNotSame(recordUploader, recordUploader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfDownloader() {
		Downloader recordDownloader = factory.factorDownloader();
		Downloader recordDownloader2 = factory.factorDownloader();
		assertNotNull(recordDownloader);
		assertNotNull(recordDownloader2);
		assertNotSame(recordDownloader, recordDownloader2);
	}

	@Test
	public void factorRecordSearchersDecoratedDependencies() {
		RecordSearcherDecoratedImp recordSearcher = (RecordSearcherDecoratedImp) factory
				.factorRecordSearcherDecorated();

		var returnedDependencyProvided = recordSearcher.onlyForTestGetDependencyProvider();
		assertSame(returnedDependencyProvided, dependencyProvider);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfSearcher() {
		RecordSearcher recordSearcher = factory.factorRecordSearcher();
		RecordSearcher recordSearcher2 = factory.factorRecordSearcher();
		assertNotNull(recordSearcher);
		assertNotNull(recordSearcher2);
		assertNotSame(recordSearcher, recordSearcher2);
	}

	@Test
	public void factorRecordSearchersDependencies() {
		RecordSearcherImp recordSearcher = (RecordSearcherImp) factory.factorRecordSearcher();

		var returnedDependencyProvided = recordSearcher.onlyForTestGetDependencyProvider();
		assertSame(returnedDependencyProvided, dependencyProvider);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfSearcherDecorated() {
		RecordSearcher recordSearcher = factory.factorRecordSearcher();
		RecordSearcher recordSearcher2 = factory.factorRecordSearcher();
		assertNotNull(recordSearcher);
		assertNotNull(recordSearcher2);
		assertNotSame(recordSearcher, recordSearcher2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordValidator() {
		RecordValidator recordValidator = factory.factorRecordValidator();
		RecordValidator recordValidator2 = factory.factorRecordValidator();
		assertNotNull(recordValidator);
		assertNotNull(recordValidator2);
		assertNotSame(recordValidator, recordValidator2);
		assertTrue(recordValidator instanceof RecordValidatorImp);
	}

	@Test
	public void testCreatorImplementation() {
		RecordCreatorImp spiderRecordCreator = (RecordCreatorImp) factory.factorRecordCreator();
		DataGroupToRecordEnhancerImp enhancer = (DataGroupToRecordEnhancerImp) spiderRecordCreator
				.onlyForTestGetDataGroupToRecordEnhancer();

		assertSame(enhancer.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void makeSureWeGetCorrectAndMultipleInstancesOfRecordListIndexer() {
		RecordListIndexerImp listIndexer = (RecordListIndexerImp) factory.factorRecordListIndexer();
		assertSame(listIndexer.onlyForTestGetDependencyProvider(), dependencyProvider);

		IndexBatchHandlerImp indexBatchHandler = (IndexBatchHandlerImp) listIndexer
				.onlyForTestGetIndexBatchHandler();
		assertTrue(indexBatchHandler instanceof IndexBatchHandlerImp);
		BatchRunnerFactoryImp batchRunnerFactory = (BatchRunnerFactoryImp) indexBatchHandler
				.getBatchRunnerFactory();
		assertTrue(batchRunnerFactory instanceof BatchRunnerFactoryImp);
		assertSame(batchRunnerFactory.getDependencyProvider(), dependencyProvider);

		assertTrue(listIndexer
				.onlyForTestGetBatchJobConverter() instanceof DataRecordGroupHandlerForIndexBatchJob);

		RecordListIndexerImp listIndexer2 = (RecordListIndexerImp) factory
				.factorRecordListIndexer();
		assertSame(listIndexer2.onlyForTestGetDependencyProvider(), dependencyProvider);

		assertNotSame(listIndexer, listIndexer2);
	}

	@Test
	public void testFactorIiifReader() {
		IiifReaderImp factored = (IiifReaderImp) factory.factorIiifReader();

		assertTrue(factored instanceof IiifReaderImp);
		assertSame(factored.onlyForTestGetDependencyProvider(), dependencyProvider);
	}

}
