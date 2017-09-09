/*
 * Copyright 2016 Olov McKie
 * Copyright 2015 Uppsala University Library
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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;

import java.util.HashMap;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordIncomingLinksReader;
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordSearcher;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderUploader;

public class SpiderInstanceFactoryTest {
	private SpiderInstanceFactory factory;

	@BeforeTest
	public void setUp() {
		SpiderDependencyProvider dependencyProvider = new SpiderDependencyProviderSpy(
				new HashMap<>());
		factory = SpiderInstanceFactoryImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordReader() {
		SpiderRecordReader recordReader = factory.factorSpiderRecordReader();
		SpiderRecordReader recordReader2 = factory.factorSpiderRecordReader();
		assertNotNull(recordReader);
		assertNotNull(recordReader2);
		assertNotSame(recordReader, recordReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordIncomingLinksReader() {
		SpiderRecordIncomingLinksReader recordIncomingLInksReader = factory
				.factorSpiderRecordIncomingLinksReader();
		SpiderRecordIncomingLinksReader recordIncomingLInksReader2 = factory
				.factorSpiderRecordIncomingLinksReader();
		assertNotNull(recordIncomingLInksReader);
		assertNotNull(recordIncomingLInksReader2);
		assertNotSame(recordIncomingLInksReader, recordIncomingLInksReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordListReader() {
		SpiderRecordListReader recordListReader = factory.factorSpiderRecordListReader();
		SpiderRecordListReader recordListReader2 = factory.factorSpiderRecordListReader();
		assertNotNull(recordListReader);
		assertNotNull(recordListReader2);
		assertNotSame(recordListReader, recordListReader2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordCreator() {
		SpiderRecordCreator recordCreator = factory.factorSpiderRecordCreator();
		SpiderRecordCreator recordCreator2 = factory.factorSpiderRecordCreator();
		assertNotNull(recordCreator);
		assertNotNull(recordCreator2);
		assertNotSame(recordCreator, recordCreator2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordUpdater() {
		SpiderRecordUpdater recordUpdater = factory.factorSpiderRecordUpdater();
		SpiderRecordUpdater recordUpdater2 = factory.factorSpiderRecordUpdater();
		assertNotNull(recordUpdater);
		assertNotNull(recordUpdater2);
		assertNotSame(recordUpdater, recordUpdater2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfRecordDeleter() {
		SpiderRecordDeleter recordDeleter = factory.factorSpiderRecordDeleter();
		SpiderRecordDeleter recordDeleter2 = factory.factorSpiderRecordDeleter();
		assertNotNull(recordDeleter);
		assertNotNull(recordDeleter2);
		assertNotSame(recordDeleter, recordDeleter2);
	}

	@Test
	public void makeSureWeGetMultipleInstancesOfUploader() {
		SpiderUploader recordUploader = factory.factorSpiderUploader();
		SpiderUploader recordUploader2 = factory.factorSpiderUploader();
		assertNotNull(recordUploader);
		assertNotNull(recordUploader2);
		assertNotSame(recordUploader, recordUploader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfDownloader() {
		SpiderDownloader recordDownloader = factory.factorSpiderDownloader();
		SpiderDownloader recordDownloader2 = factory.factorSpiderDownloader();
		assertNotNull(recordDownloader);
		assertNotNull(recordDownloader2);
		assertNotSame(recordDownloader, recordDownloader2);
	}

	@Test
	public void initMakeSureWeGetMultipleInstancesOfSearcher() {
		SpiderRecordSearcher recordSearcher = factory.factorSpiderRecordSearcher();
		SpiderRecordSearcher recordSearcher2 = factory.factorSpiderRecordSearcher();
		assertNotNull(recordSearcher);
		assertNotNull(recordSearcher2);
		assertNotSame(recordSearcher, recordSearcher2);
	}
}
