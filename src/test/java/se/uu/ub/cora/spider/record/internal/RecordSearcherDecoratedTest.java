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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataListSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordSearcherSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;

public class RecordSearcherDecoratedTest {
	private static final String SEARCH_ID = "someSearchId";
	private static final String AUTH_TOKEN = "someAuthToken";
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordSearcher searcher;
	private SpiderInstanceFactorySpy instanceFactory;
	private RecordDecoratorSpy recordDecoratorSpy;
	private DataRecordSpy dr1;
	private DataRecordSpy dr2;
	private DataListSpy searchResult;

	@BeforeMethod
	private void beforeMethod() {
		instanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(instanceFactory);
		setUpDependencyProvider();
		setUpRecordSearcherToReturnResultWithTwoRecords();
		searcher = new RecordSearcherDecoratedImp(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		recordDecoratorSpy = new RecordDecoratorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordDecorator",
				() -> recordDecoratorSpy);
	}

	private void setUpRecordSearcherToReturnResultWithTwoRecords() {
		dr1 = new DataRecordSpy();
		dr2 = new DataRecordSpy();
		searchResult = new DataListSpy();
		searchResult.MRV.setDefaultReturnValuesSupplier("getDataList", () -> List.of(dr1, dr2));

		RecordSearcherSpy recordSearcherSpy = new RecordSearcherSpy();
		recordSearcherSpy.MRV.setDefaultReturnValuesSupplier("search", () -> searchResult);
		instanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordSearcher",
				() -> recordSearcherSpy);
	}

	@Test
	public void testSearchDecorated() {
		DataGroupSpy searchData = new DataGroupSpy();
		DataList decoratedDataList = searcher.search(AUTH_TOKEN, SEARCH_ID, searchData);

		var recordSearcher = (RecordSearcherSpy) instanceFactory.MCR
				.assertCalledParametersReturn("factorRecordSearcher");
		recordSearcher.MCR.assertCalledParameters("search", AUTH_TOKEN, SEARCH_ID, searchData);

		var recordDecorator = (RecordDecoratorSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getRecordDecorator");

		recordDecorator.MCR.assertParameters("decorateRecord", 0, dr1, AUTH_TOKEN);
		recordDecorator.MCR.assertParameters("decorateRecord", 1, dr2, AUTH_TOKEN);
		assertSame(decoratedDataList, searchResult);
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		assertSame(((RecordSearcherDecoratedImp) searcher).onlyForTestGetDependencyProvider(),
				dependencyProvider);
	}
}
