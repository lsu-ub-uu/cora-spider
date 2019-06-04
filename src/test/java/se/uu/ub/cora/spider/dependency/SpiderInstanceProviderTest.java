/*
 * Copyright 2015, 2018 Uppsala University Library
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

package se.uu.ub.cora.spider.dependency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SpiderInstanceProviderTest {
	@Test
	public void testPrivateConstructor() throws Exception {
		Constructor<SpiderInstanceProvider> constructor = SpiderInstanceProvider.class
				.getDeclaredConstructor();
		Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
	}

	@Test
	public void testGetDependencyProviderClassName() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		assertEquals(SpiderInstanceProvider.getDependencyProviderClassName(),
				"someDependencyProviderClassNameFromSpy");
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
	public void makeSureFactoryCreateIsCalledForRecordReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordReader();
		assertTrue(factory.readerFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordIncomingLinksReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordIncomingLinksReader();
		assertTrue(factory.incomingLinksReaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForListRecordReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordListReader();
		assertTrue(factory.listReaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordCreator() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordCreator();
		assertTrue(factory.creatorFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordUpdater() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordUpdater();
		assertTrue(factory.updaterFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordDeleter() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordDeleter();
		assertTrue(factory.deleterFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordUploader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderUploader();
		assertTrue(factory.uploaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordDownloader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderDownloader();
		assertTrue(factory.downloaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordSearcher() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordSearcher();
		assertTrue(factory.searcherFactoryWasCalled);
	}

	@Test
	public void testSetInitInfo() throws Exception {
		Map<String, String> initInfo = new HashMap<>();
		SpiderInstanceProvider.setInitInfo(initInfo);
		assertEquals(SpiderInstanceProvider.getInitInfo(), initInfo);
	}

	@Test
	public void makeSureFactoryCreateIsCalledForRecordValidator() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getSpiderRecordValidator();
		assertTrue(factory.validatorFactoryWasCalled);
	}
}
