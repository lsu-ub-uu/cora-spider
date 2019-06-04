/*
 * Copyright 2018 Uppsala University Library
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

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SpiderDependencyProviderTest {

	private Map<String, String> initInfo;
	private SpiderDependencyProviderSpy dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		initInfo = new HashMap<>();
		initInfo.put("someId", "someKey");
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
	}

	@Test
	public void testInitInfoIsSetOnStartup() {
		assertEquals(dependencyProvider.getInitInfoFromParent("someId"), "someKey");
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
			+ "Error starting SpiderDependencyProviderSpy: some runtime error message")
	public void testStartupThrowsRuntimeException() throws Exception {
		initInfo.put("runtimeException", "some runtime error message");
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
	}

	@Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ""
			+ "Error starting SpiderDependencyProviderSpy: some invocation target error message")
	public void testStartupThrowsInvocationTargetException() throws Exception {
		initInfo.put("invocationTargetException", "some invocation target error message");
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
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
	public void testSetMetadataStorage() {
		MetadataStorageProviderSpy metadataStorageProvider = new MetadataStorageProviderSpy();
		dependencyProvider.setMetadataStorageProvider(metadataStorageProvider);
		assertEquals(dependencyProvider.getMetadataStorage(),
				metadataStorageProvider.getMetadataStorage());
	}

}
