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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.DataDecoratorSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.record.RecordDecorator;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordReaderSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;

public class RecordDecoratorTest {
	private static final String SOME_ID = "someId";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private SpiderInstanceFactorySpy instanceFactory;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordReaderSpy customRecordReader;
	private RecordDecorator recordDecorator;
	private DataFactorySpy dataFactory;
	private Pair mainRecord;
	private Pair childRecord01;
	private Pair childRecord02;
	private Pair childRecord03;
	private Pair grandChildRecord01;
	private Pair grandChildRecord02;
	private Pair grandChildRecord03;
	private Pair grandGrandChildRecord01;
	private Pair childRecordInfo;
	private DataRecordSpy someDataRecord;
	private DataRecordSpy inputRecord;

	@BeforeMethod
	public void beforeMethod() {
		instanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(instanceFactory);
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);
		setUpDependencyProvider();
		someDataRecord = new DataRecordSpy();

		recordDecorator = RecordDecoratorImp.usingDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();

		RecordTypeHandlerSpy recordTypeHandler = new RecordTypeHandlerSpy();
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "someDefinitionId");

		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);
	}

	@Test
	public void testInit() {
		recordDecorator.decorateRecord(someDataRecord, SOME_AUTH_TOKEN);

		var dataDecorator = (DataDecoratorSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getDataDecorator");
		dataDecorator.MCR.assertParameters("decorateRecord", 0, "someDefinitionId", someDataRecord);
	}

	@Test
	public void testAddDecoratedOnlyReadToDepth2() {
		customRecordReader = new RecordReaderSpy();
		instanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordReader",
				() -> customRecordReader);

		setupRecordReaderWithMainAndRelativesRecords();

		recordDecorator.decorateRecord(inputRecord, SOME_AUTH_TOKEN);

		customRecordReader.MCR.assertNumberOfCallsToMethod("readRecord", 7);
		assertRecordReadFromStorage(0, "someChildId01");
		assertRecordReadFromStorage(1, "someGrandChildId01");
		assertRecordReadFromStorage(2, "someGrandChildId02");
		assertRecordReadFromStorage(3, "someChildId02");
		assertRecordReadFromStorage(4, "someGrandChildId03");
		assertRecordReadFromStorage(5, "someGrandChildId02");
		assertRecordReadFromStorage(6, "someChildId03");

		dataFactory.MCR.assertNumberOfCallsToMethod("factorGroupFromDataRecordGroup", 11);

		assertLinkedRecordSetToLink(childRecord01);
		assertLinkedRecordSetToLink(childRecord02);
		assertLinkedRecordSetToLink(childRecord03);
		assertLinkedRecordSetToLink(grandChildRecord01);
		assertLinkedRecordSetToLink(grandChildRecord02);
		assertLinkedRecordSetToLink(grandChildRecord03);
		assertNotLinkedRecordSetToLink(childRecordInfo);
		assertNotLinkedRecordSetToLink(grandGrandChildRecord01);
	}

	@Test
	public void testAddDecorated_OneChildLinkHasNoReadAction() {
		customRecordReader = new RecordReaderSpy();
		instanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordReader",
				() -> customRecordReader);

		setupRecordReaderWithMainAndRelativesRecords();
		setChildLinkWithoutReadAction(childRecord01.link());

		recordDecorator.decorateRecord(inputRecord, SOME_AUTH_TOKEN);

		customRecordReader.MCR.assertNumberOfCallsToMethod("readRecord", 4);
		assertRecordReadFromStorage(0, "someChildId02");
		assertRecordReadFromStorage(1, "someGrandChildId03");
		assertRecordReadFromStorage(2, "someGrandChildId02");
		assertRecordReadFromStorage(3, "someChildId03");

		dataFactory.MCR.assertNumberOfCallsToMethod("factorGroupFromDataRecordGroup", 7);

		assertLinkedRecordSetToLink(childRecord02);
		assertLinkedRecordSetToLink(grandChildRecord02);
		assertLinkedRecordSetToLink(grandChildRecord03);
		assertNotLinkedRecordSetToLink(childRecord01);
		assertNotLinkedRecordSetToLink(childRecord03);
		assertNotLinkedRecordSetToLink(grandChildRecord01);
		assertNotLinkedRecordSetToLink(childRecordInfo);
		assertNotLinkedRecordSetToLink(grandGrandChildRecord01);
	}

	private void setChildLinkWithoutReadAction(DataRecordLinkSpy dataRecordLink) {
		dataRecordLink.MRV.setDefaultReturnValuesSupplier("hasReadAction", () -> false);
	}

	private void assertRecordReadFromStorage(int order, String id) {
		customRecordReader.MCR.assertParameters("readRecord", order, SOME_AUTH_TOKEN, SOME_TYPE,
				id);
	}

	private void assertLinkedRecordSetToLink(Pair linkedRecord) {
		var linkedRecordAsGroup = dataFactory.MCR.assertCalledParametersReturn(
				"factorGroupFromDataRecordGroup", linkedRecord.dataRecordGroup);
		linkedRecord.link.MCR.assertParameters("setLinkedRecord", 0, linkedRecordAsGroup);
	}

	// TODO: It might be moved to MCR as assertNotCalledParameters
	private void assertNotLinkedRecordSetToLink(Pair linkedRecord) {
		try {
			dataFactory.MCR.assertCalledParameters("factorGroupFromDataRecordGroup",
					linkedRecord.dataRecordGroup);
			fail();
		} catch (AssertionError e) {
			assertTrue(true);
		}
	}

	private void setupRecordReaderWithMainAndRelativesRecords() {
		inputRecord = new DataRecordSpy();
		inputRecord.MRV.setDefaultReturnValuesSupplier("getType", () -> SOME_TYPE);
		DataRecordGroupSpy dataRecordGroup1 = new DataRecordGroupSpy();
		inputRecord.MRV.setDefaultReturnValuesSupplier("getDataRecordGroup",
				() -> dataRecordGroup1);
		DataRecordGroupSpy dataRecordGroup = dataRecordGroup1;
		DataRecordLinkSpy link = createRecordLinkToRecord(SOME_ID);
		mainRecord = new Pair(dataRecordGroup, link);

		childRecord01 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someChildId01");
		childRecord02 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someChildId02");
		childRecord03 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someChildId03");
		grandChildRecord01 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someGrandChildId01");
		grandChildRecord02 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someGrandChildId02");
		grandChildRecord03 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someGrandChildId03");
		grandGrandChildRecord01 = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someGrandGrandChildId01");
		childRecordInfo = createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
				"someChildId0RecordInfo");

		DataGroupSpy mG0 = createGroup();
		DataGroupSpy mG0RI = createRecorInfoGroup();
		DataGroupSpy mG00 = createGroup();
		DataGroupSpy mG01 = createGroup();
		DataGroupSpy mG000 = createGroup();

		setTopGroupToDataRecordGroup(mainRecord.dataRecordGroup, mG0);
		setGroupAsChildAsGroup(mG0, mG0RI, mG00, mG01);
		setGroupAsChildAsGroup(mG00, mG000);

		linkRecordGroupToLink(mG0RI, childRecordInfo.link);
		linkRecordGroupToLink(mG00, childRecord01.link);
		linkRecordGroupToLink(mG01, childRecord02.link);
		linkRecordGroupToLink(mG0, childRecord03.link);

		DataGroupSpy ch0GRI = createGroup();
		DataGroupSpy ch1G0 = createGroup();
		DataGroupSpy ch1G00 = createGroup();
		DataGroupSpy ch2G0 = createGroup();
		DataGroupSpy ch2G00 = createGroup();
		DataGroupSpy ch3G0 = createGroup();

		setTopGroupToDataRecordGroup(childRecordInfo.dataRecordGroup, ch0GRI);

		setTopGroupToDataRecordGroup(childRecord01.dataRecordGroup, ch1G0);
		setGroupAsChildAsGroup(ch1G0, ch1G00);

		setTopGroupToDataRecordGroup(childRecord02.dataRecordGroup, ch2G0);
		setGroupAsChildAsGroup(ch2G0, ch2G00);
		linkRecordGroupToLink(ch1G00, grandChildRecord01.link, grandChildRecord02.link);
		linkRecordGroupToLink(ch2G0, grandChildRecord02.link);
		linkRecordGroupToLink(ch2G00, grandChildRecord03.link);

		setTopGroupToDataRecordGroup(childRecord03.dataRecordGroup, ch3G0);

		DataGroupSpy gc3G0 = createGroup();
		setTopGroupToDataRecordGroup(grandChildRecord03.dataRecordGroup, gc3G0);
		linkRecordGroupToLink(gc3G0, grandGrandChildRecord01.link);
	}

	private void setTopGroupToDataRecordGroup(DataRecordGroupSpy dataRecord,
			DataGroupSpy topGroup) {
		dataFactory.MRV.setSpecificReturnValuesSupplier("factorGroupFromDataRecordGroup",
				() -> topGroup, dataRecord);
	}

	private void setGroupAsChildAsGroup(DataGroupSpy group, DataGroupSpy... children) {
		group.MRV.setSpecificReturnValuesSupplier("getChildrenOfType", () -> List.of(children),
				DataGroup.class);
	}

	private DataGroupSpy createGroup() {
		return new DataGroupSpy();
	}

	private DataGroupSpy createRecorInfoGroup() {
		DataGroupSpy dataGroupSpy = new DataGroupSpy();
		dataGroupSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "recordInfo");
		return dataGroupSpy;
	}

	private void linkRecordGroupToLink(DataGroupSpy group, DataRecordLinkSpy... links) {
		group.MRV.setSpecificReturnValuesSupplier("getChildrenOfType", () -> List.of(links),
				DataRecordLink.class);
	}

	private Pair createRecordToStorageAndReturnRelatedDataRecordAndDataRecordLinkUsingId(
			String recordId) {
		DataRecordGroupSpy dataRecordGroup = createRecordInStorageAndReturnItsRecordGroup(recordId);
		DataRecordLinkSpy link = createRecordLinkToRecord(recordId);
		return new Pair(dataRecordGroup, link);
	}

	private DataRecordLinkSpy createRecordLinkToRecord(String recordId) {
		DataRecordLinkSpy link = new DataRecordLinkSpy();
		link.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> SOME_TYPE);
		link.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> recordId);
		link.MRV.setDefaultReturnValuesSupplier("hasReadAction", () -> true);
		return link;
	}

	private DataRecordGroupSpy createRecordInStorageAndReturnItsRecordGroup(String recordId) {
		DataRecordSpy dataRecordSpy = new DataRecordSpy();
		customRecordReader.MRV.setSpecificReturnValuesSupplier("readRecord", () -> dataRecordSpy,
				SOME_AUTH_TOKEN, SOME_TYPE, recordId);
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getDataRecordGroup",
				() -> dataRecordGroup);
		return dataRecordGroup;
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		RecordReaderDecoratedImp drr = RecordReaderDecoratedImp
				.usingDependencyProvider(dependencyProvider);

		assertSame(drr.onlyForTestGetDependencyProvider(), dependencyProvider);
	}

	record Pair(DataRecordGroupSpy dataRecordGroup, DataRecordLinkSpy link) {
	}
}