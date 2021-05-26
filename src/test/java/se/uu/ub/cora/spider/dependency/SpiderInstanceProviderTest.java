/*
 * Copyright 2015, 2018, 2021 Uppsala University Library
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
	public void makeSureFactoryIsCalledForRecordReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordReader();
		assertTrue(factory.readerFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordIncomingLinksReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getIncomingLinksReader();
		assertTrue(factory.incomingLinksReaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForListRecordReader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordListReader();
		assertTrue(factory.listReaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordCreator() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordCreator();
		assertTrue(factory.creatorFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordUpdater() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordUpdater();
		assertTrue(factory.updaterFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordDeleter() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordDeleter();
		assertTrue(factory.deleterFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordUploader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getUploader();
		assertTrue(factory.uploaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordDownloader() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getDownloader();
		assertTrue(factory.downloaderFactoryWasCalled);
	}

	@Test
	public void makeSureFactoryIsCalledForRecordSearcher() {
		SpiderInstanceFactorySpy factory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		SpiderInstanceProvider.getRecordSearcher();
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
		SpiderInstanceProvider.getRecordValidator();
		assertTrue(factory.validatorFactoryWasCalled);
	}
}
