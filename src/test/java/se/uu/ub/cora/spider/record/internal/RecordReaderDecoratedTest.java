/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertSame;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.RecordReaderDecorated;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordReaderSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;

public class RecordReaderDecoratedTest {
	private static final String SOME_ID = "someId";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private SpiderInstanceFactorySpy instanceFactory;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordReaderDecorated decoratedRecordReader;
	private RecordDecoratorSpy recordDecoratorSpy;

	@BeforeMethod
	public void beforeMethod() {
		instanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(instanceFactory);
		setUpDependencyProvider();
		decoratedRecordReader = RecordReaderDecoratedImp
				.usingDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		recordDecoratorSpy = new RecordDecoratorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordDecorator",
				() -> recordDecoratorSpy);
	}

	@Test
	public void testInit() {
		DataRecord decoratedRecord = decoratedRecordReader.readDecoratedRecord(SOME_AUTH_TOKEN,
				SOME_TYPE, SOME_ID);

		var recordReader = (RecordReaderSpy) instanceFactory.MCR
				.assertCalledParametersReturn("factorRecordReader");
		var readRecord = (DataRecord) recordReader.MCR.assertCalledParametersReturn("readRecord",
				SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		var recordDecorator = (RecordDecoratorSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getRecordDecorator");
		recordDecorator.MCR.assertParameters("decorateRecord", 0, readRecord, SOME_AUTH_TOKEN);
		assertSame(readRecord, decoratedRecord);
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		RecordReaderDecoratedImp drr = RecordReaderDecoratedImp
				.usingDependencyProvider(dependencyProvider);

		assertSame(drr.onlyForTestGetDependencyProvider(), dependencyProvider);
	}
}