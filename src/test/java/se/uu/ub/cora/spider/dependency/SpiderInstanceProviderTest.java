/*
 * Copyright 2015, 2018, 2021, 2024, 2025 Uppsala University Library
 * Copyright 2017, 2019 Uppsala University Library
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.binary.iiif.IiifReader;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;

public class SpiderInstanceProviderTest {
	private SpiderInstanceFactorySpy factory;

	@BeforeMethod
	public void beforeMethod() {
		factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.onlyForTestSetSpiderInstanceFactory(factory);
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		Constructor<SpiderInstanceProvider> constructor = SpiderInstanceProvider.class
				.getDeclaredConstructor();
		Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
	}

	@Test
	public void testGetDependencyProviderClassName() {
		String dummyClassName = "someDependencyProviderClassNameFromSpy";
		factory.MRV.setDefaultReturnValuesSupplier("getDependencyProviderClassName",
				() -> dummyClassName);

		assertEquals(SpiderInstanceProvider.getDependencyProviderClassName(), dummyClassName);
	}

	@Test(expectedExceptions = InvocationTargetException.class)
	public void testPrivateConstructorInvoke() throws Exception {
		Constructor<SpiderInstanceProvider> constructor = SpiderInstanceProvider.class
				.getDeclaredConstructor();
		Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void makeSureFactoryIsCalledForRecordReader() {
		var recordReader = SpiderInstanceProvider.getRecordReader();

		factory.MCR.assertReturn("factorRecordReader", 0, recordReader);
	}

	@Test
	public void makeSureFactoryIsCalledForDecoratedRecordReader() {
		var decorateRecordReader = SpiderInstanceProvider.getDecoratedRecordReader();

		factory.MCR.assertReturn("factorDecoratedRecordReader", 0, decorateRecordReader);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordIncomingLinksReader() {
		IncomingLinksReader incomingLinksReader = SpiderInstanceProvider.getIncomingLinksReader();

		factory.MCR.assertReturn("factorIncomingLinksReader", 0, incomingLinksReader);
	}

	@Test
	public void makeSureFactoryIsCalledForListRecordReader() {
		RecordListReader recordListReader = SpiderInstanceProvider.getRecordListReader();

		factory.MCR.assertReturn("factorRecordListReader", 0, recordListReader);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordCreator() {
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();

		factory.MCR.assertReturn("factorRecordCreator", 0, recordCreator);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordUpdater() {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();

		factory.MCR.assertReturn("factorRecordUpdater", 0, recordUpdater);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordDeleter() {
		RecordDeleter recordDeleter = SpiderInstanceProvider.getRecordDeleter();

		factory.MCR.assertReturn("factorRecordDeleter", 0, recordDeleter);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordUploader() {
		Uploader uploader = SpiderInstanceProvider.getUploader();

		factory.MCR.assertReturn("factorUploader", 0, uploader);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordDownloader() {
		Downloader downloader = SpiderInstanceProvider.getDownloader();

		factory.MCR.assertReturn("factorDownloader", 0, downloader);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordSearcher() {
		RecordSearcher recordSearcher = SpiderInstanceProvider.getRecordSearcher();

		factory.MCR.assertReturn("factorRecordSearcher", 0, recordSearcher);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordValidator() {
		RecordValidator recordValidator = SpiderInstanceProvider.getRecordValidator();

		factory.MCR.assertReturn("factorRecordValidator", 0, recordValidator);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordListIndexer() {
		var recordListIndexer = SpiderInstanceProvider.getRecordListIndexer();

		factory.MCR.assertReturn("factorRecordListIndexer", 0, recordListIndexer);
	}

	@Test
	public void makeSureFactoryIsCalledForIiifReader() {
		IiifReader iiifReader = SpiderInstanceProvider.getIiifReader();

		factory.MCR.assertReturn("factorIiifReader", 0, iiifReader);
	}
}
