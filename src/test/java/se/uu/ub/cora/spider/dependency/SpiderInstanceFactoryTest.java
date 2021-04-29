/*
 * Copyright 2016 Olov McKie
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

package se.uu.ub.cora.spider.dependency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.record.internal.IndexBatchJobCreator;
import se.uu.ub.cora.spider.record.internal.RecordCreatorImp;
import se.uu.ub.cora.spider.record.internal.RecordValidatorImp;

public class SpiderInstanceFactoryTest {
	private SpiderInstanceFactory factory;
	private SpiderDependencyProvider dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;

	@BeforeTest
	public void setUp() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		StreamStorageProviderSpy streamStorageProviderSpy = new StreamStorageProviderSpy();
		dependencyProvider.setStreamStorageProvider(streamStorageProviderSpy);
		RecordIdGeneratorProviderSpy recordIdGeneratorProviderSpy = new RecordIdGeneratorProviderSpy();
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProviderSpy);

		factory = SpiderInstanceFactoryImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testGetDependencyProviderClassName() {
		assertEquals(factory.getDependencyProviderClassName(),
				dependencyProvider.getClass().getName());
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordReader() {
		RecordReader recordReader = factory.factorSpiderRecordReader();
		RecordReader recordReader2 = factory.factorSpiderRecordReader();
		assertNotNull(recordReader);
		assertNotNull(recordReader2);
		assertNotSame(recordReader, recordReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordIncomingLinksReader() {
		IncomingLinksReader recordIncomingLInksReader = factory
				.factorSpiderRecordIncomingLinksReader();
		IncomingLinksReader recordIncomingLInksReader2 = factory
				.factorSpiderRecordIncomingLinksReader();
		assertNotNull(recordIncomingLInksReader);
		assertNotNull(recordIncomingLInksReader2);
		assertNotSame(recordIncomingLInksReader, recordIncomingLInksReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordListReader() {
		RecordListReader recordListReader = factory.factorSpiderRecordListReader();
		RecordListReader recordListReader2 = factory.factorSpiderRecordListReader();
		assertNotNull(recordListReader);
		assertNotNull(recordListReader2);
		assertNotSame(recordListReader, recordListReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordCreator() {
		RecordCreator recordCreator = factory.factorSpiderRecordCreator("someRecordType");
		RecordCreator recordCreator2 = factory.factorSpiderRecordCreator("someRecordType");
		assertNotNull(recordCreator);
		assertNotNull(recordCreator2);
		assertNotSame(recordCreator, recordCreator2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordUpdater() {
		RecordUpdater recordUpdater = factory
				.factorSpiderRecordUpdater("onlyDefaultUpdateImplemented");
		RecordUpdater recordUpdater2 = factory
				.factorSpiderRecordUpdater("onlyDefaultUpdateImplemented");
		assertNotNull(recordUpdater);
		assertNotNull(recordUpdater2);
		assertNotSame(recordUpdater, recordUpdater2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordDeleter() {
		RecordDeleter recordDeleter = factory.factorSpiderRecordDeleter();
		RecordDeleter recordDeleter2 = factory.factorSpiderRecordDeleter();
		assertNotNull(recordDeleter);
		assertNotNull(recordDeleter2);
		assertNotSame(recordDeleter, recordDeleter2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfUploader() {
		Uploader recordUploader = factory.factorSpiderUploader();
		Uploader recordUploader2 = factory.factorSpiderUploader();
		assertNotNull(recordUploader);
		assertNotNull(recordUploader2);
		assertNotSame(recordUploader, recordUploader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfDownloader() {
		Downloader recordDownloader = factory.factorSpiderDownloader();
		Downloader recordDownloader2 = factory.factorSpiderDownloader();
		assertNotNull(recordDownloader);
		assertNotNull(recordDownloader2);
		assertNotSame(recordDownloader, recordDownloader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfSearcher() {
		RecordSearcher recordSearcher = factory.factorSpiderRecordSearcher();
		RecordSearcher recordSearcher2 = factory.factorSpiderRecordSearcher();
		assertNotNull(recordSearcher);
		assertNotNull(recordSearcher2);
		assertNotSame(recordSearcher, recordSearcher2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordValidator() {
		RecordValidator recordValidator = factory.factorSpiderRecordValidator();
		RecordValidator recordValidator2 = factory.factorSpiderRecordValidator();
		assertNotNull(recordValidator);
		assertNotNull(recordValidator2);
		assertNotSame(recordValidator, recordValidator2);
		assertTrue(recordValidator instanceof RecordValidatorImp);
	}

	@Test
	public void testDefaultCreatorImplementation() {

		RecordCreatorImp spiderRecordCreator = (RecordCreatorImp) factory
				.factorSpiderRecordCreator("someRecordType");

		DataGroupToRecordEnhancerImp enhancer = (DataGroupToRecordEnhancerImp) spiderRecordCreator
				.getDataGroupToRecordEnhancer();

		assertSame(enhancer.getDependencyProvider(), dependencyProvider);

	}

	@Test
	public void testIndexBatchJobCreatorImplementation() {

		IndexBatchJobCreator indexBatchJobCreator = (IndexBatchJobCreator) factory
				.factorSpiderRecordCreator("indexBatchJob");

		DataGroupToRecordEnhancerImp enhancer = (DataGroupToRecordEnhancerImp) indexBatchJobCreator
				.getDataGroupToRecordEnhancer();

		assertSame(enhancer.getDependencyProvider(), dependencyProvider);

	}
}
