/*
 * Copyright 2026 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class LinkAuthorizatorTest {
	private static final User NO_NEEDED_USER = null;
	private static final User USER = new User("987654321");
	private SpiderDependencyProviderSpy dependencyProvider;
	private LinkAuthorizatorImp linkAuthorizator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private DataGroupTermCollectorSpy termCollector;
	private DataRecordLinkSpy recordLink1;

	private Map<String, RecordTypeHandlerSpy> recordTypeHandlers;
	private Map<String, DataRecordGroupSpy> dataRecordGroups;

	@BeforeMethod
	public void setUp() {
		recordTypeHandlers = new HashMap<>();
		dataRecordGroups = new HashMap<>();

		setUpDependencyProvider();
		recordLink1 = creatRecordLinkAndRecords("someType", "someId");

		linkAuthorizator = new LinkAuthorizatorImp(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();

		authorizator = new SpiderAuthorizatorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);

		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);

		termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
	}

	@Test
	public void testRecordNotFound() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("someException"));

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(NO_NEEDED_USER, recordLink1));
	}

	@Test
	public void testIsPublicForRead_notPublic() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testIsPublicForRead_public() {
		recordTypeIsPublic(true, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitFalse_rulesNOK() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitFalse_rulesOK() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityFalse_published_permissionUnitFalse() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_published_permissionUnitFalse() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitTrue_noUnit() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitTrue_hasUnit_rulesOk() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(true, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitTrue_hasUnit_rulesNotOk() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		recordTypeUseHostRecord(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(true, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_hostRecord_hostRecordNoPermissionUnit_hostRecordRules_NOK() {
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(true, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		hostRecordTypeUsePermissionUnit(false, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_hostRecord_hostRecordNoPermissionUnit_hostRecordRules_OK() {
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(true, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		hostRecordTypeUsePermissionUnit(false, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Published_hostRecord() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		hostRecordTypeUsePermissionUnit(false, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Unpublished_hostRecord_hostRecordNoPermissionUnit_hostRecordRules_NOK() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(false, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Unpublished_hostRecord_hostRecordNoPermissionUnit_hostRecordRules_OK() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(false, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Unpublished_hostRecord_hostRecordUsesPermissionUnit_NOK() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		setAuthorizedForHostPermissionUnit(false, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Unpublished_hostRecord_hostRecordUsesPermissionUnit_hostRules_NOK() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		setAuthorizedForHostPermissionUnit(true, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test
	public void test_Unpublished_hostRecord_hostRecordUsesPermissionUnit_hostRules_OK() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		setAuthorizedForHostPermissionUnit(true, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));

	}

	@Test
	public void test_recordTypeHandlersCache() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		setAuthorizedForHostPermissionUnit(true, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));

		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "someType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "hostsomeType");

	}

	@Test
	public void test_recordLinksCache() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		setAuthorizedForHostPermissionUnit(true, recordLink1);
		setAuthorizedForHostActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
		assertTrue(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));

		RecordTypeHandlerSpy recordTypeHandlerSpy = recordTypeHandlers.get("someType");
		recordTypeHandlerSpy.MCR.assertNumberOfCallsToMethod("isPublicForRead", 1);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Visibility is missing in the record.")
	public void testMissingVisibility() {
		recordTypeUseVisibility(true, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "PermissionUnit is missing in the record.")
	public void testMissingPermissionUnit() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		DataRecordGroupSpy dataRecordGroupSpy = dataRecordGroups.get(createKey(recordLink1));
		dataRecordGroupSpy.MRV.setDefaultReturnValuesSupplier("getPermissionUnit", Optional::empty);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "HostRecord is missing in the record.")
	public void testMissingHostRecord() {
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUseHostRecord(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		hostRecordTypeUsePermissionUnit(true, recordLink1);
		DataRecordGroupSpy dataRecordGroupSpy = dataRecordGroups.get(createKey(recordLink1));
		dataRecordGroupSpy.MRV.setDefaultReturnValuesSupplier("getHostRecord", Optional::empty);

		assertFalse(linkAuthorizator.isAuthorizedToReadRecordLink(USER, recordLink1));
	}
	//
	// String x = """
	// for READ on a recordLink ex. binary, alvin-record, diva-output
	// recordTypeIsPublic --> possible yes
	// recordIsPublished --> possible yes
	// recordTypeUsesHostRecord --> *1
	// permissionUnit --> possible no (if no match permissionUnit)
	// roles rules (recordtype+colletterms+action)
	//
	// *1
	// recordTypeIsPublic (ignore)
	// recordIsPublished (ignore)
	// recordTypeUsesHostRecord (ignore for now)
	// permissionUnit --> possible no (if no match permissionUnit)
	// roles rules (READ, system.alvin-record.binary, collectTerms)
	// """;

	private DataRecordLinkSpy creatRecordLinkAndRecords(String type, String id) {
		DataRecordLinkSpy recordLink = new DataRecordLinkSpy();
		recordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> type);
		recordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> id);

		recordTypeHandlers.computeIfAbsent("host" + type,
				_ -> createHostRecordTypeHandlerAndAddToDependencyProvider(type));

		recordTypeHandlers.computeIfAbsent(type,
				this::createRecordTypeHandlerAndAddToDependencyProvider);

		String recordKey = createKey(recordLink);

		dataRecordGroups.computeIfAbsent("host" + recordKey,
				_ -> createHostDataRecordGroupAndAddToStorage(recordLink));

		dataRecordGroups.computeIfAbsent(recordKey,
				_ -> createDataRecordGroupAndAddToStorage(recordLink));

		return recordLink;
	}

	private RecordTypeHandlerSpy createHostRecordTypeHandlerAndAddToDependencyProvider(
			String recordType) {
		RecordTypeHandlerSpy recordTypeHandlerForLink = new RecordTypeHandlerSpy();
		recordTypeHandlerForLink.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "host" + "someDefintion_" + recordType);
		dependencyProvider.MRV.setSpecificReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandlerForLink, "host" + recordType);
		return recordTypeHandlerForLink;
	}

	private RecordTypeHandlerSpy createRecordTypeHandlerAndAddToDependencyProvider(
			String recordType) {
		RecordTypeHandlerSpy recordTypeHandlerForLink = new RecordTypeHandlerSpy();
		recordTypeHandlerForLink.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "someDefintion_" + recordType);
		dependencyProvider.MRV.setSpecificReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandlerForLink, recordType);
		return recordTypeHandlerForLink;
	}

	private String createKey(DataRecordLink recordLink) {
		return recordLink.getLinkedRecordType() + recordLink.getLinkedRecordId();
	}

	private DataRecordGroupSpy createHostDataRecordGroupAndAddToStorage(DataRecordLink recordLink) {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		String recordType = "host" + recordLink.getLinkedRecordType();
		String recordId = "host" + recordLink.getLinkedRecordId();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getType", () -> recordType);

		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> recordId);

		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of("host" + createKey(recordLink)));

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> dataRecordGroup, recordType,
				recordId);
		return dataRecordGroup;
	}

	private DataRecordGroupSpy createDataRecordGroupAndAddToStorage(DataRecordLink recordLink) {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getType",
				recordLink::getLinkedRecordType);
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", recordLink::getLinkedRecordId);

		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of(createKey(recordLink)));

		DataRecordLinkSpy hostRecord = new DataRecordLinkSpy();
		hostRecord.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "host" + recordLink.getLinkedRecordType());
		hostRecord.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "host" + recordLink.getLinkedRecordId());
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getHostRecord",
				() -> Optional.of(hostRecord));

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> dataRecordGroup,
				recordLink.getLinkedRecordType(), recordLink.getLinkedRecordId());
		return dataRecordGroup;
	}

	private void recordTypeUseHostRecord(boolean useHostRecord, DataRecordLink recordLink) {
		RecordTypeHandlerSpy recordTypeHandler = recordTypeHandlers
				.get(recordLink.getLinkedRecordType());
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> useHostRecord);
	}

	private void recordTypeIsPublic(boolean isPublicForRead, DataRecordLink recordLink) {
		RecordTypeHandlerSpy recordTypeHandler = recordTypeHandlers
				.get(recordLink.getLinkedRecordType());
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("isPublicForRead",
				() -> isPublicForRead);
	}

	private void recordTypeUseVisibility(boolean useVisibility, DataRecordLink recordLink) {
		RecordTypeHandlerSpy recordTypeHandler = recordTypeHandlers
				.get(recordLink.getLinkedRecordType());
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> useVisibility);
	}

	private void recordTypeUsePermissionUnit(boolean usePermissionUnit, DataRecordLink recordLink) {
		RecordTypeHandlerSpy recordTypeHandler = recordTypeHandlers
				.get(recordLink.getLinkedRecordType());
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("usePermissionUnit",
				() -> usePermissionUnit);
	}

	private void hostRecordTypeUsePermissionUnit(boolean usePermissionUnit,
			DataRecordLink recordLink) {
		RecordTypeHandlerSpy recordTypeHandler = recordTypeHandlers
				.get("host" + recordLink.getLinkedRecordType());
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("usePermissionUnit",
				() -> usePermissionUnit);
	}

	private void linkedRecordVisibilityIsForLink(String visibility, DataRecordLink recordLink) {
		String recordKey = recordLink.getLinkedRecordType() + recordLink.getLinkedRecordId();
		DataRecordGroupSpy dataRecordGroup = dataRecordGroups.get(recordKey);
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of(visibility));
	}

	private void setAuthorizedForHostPermissionUnit(boolean isAuthorized,
			DataRecordLink recordLink) {
		authorizator.MRV.setSpecificReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit",
				() -> isAuthorized, USER, "host" + createKey(recordLink));
	}

	private void setAuthorizedForPermissionUnit(boolean isAuthorized, DataRecordLink recordLink) {
		authorizator.MRV.setSpecificReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit",
				() -> isAuthorized, USER, createKey(recordLink));
	}

	private void setAuthorizedForHostActionRecordTypePermissionTerms(boolean isAuthorized,
			DataRecordLink recordLink) {
		String key = "host" + createKey(recordLink);

		String definitionId = "host" + "someDefintion_" + recordLink.getLinkedRecordType();
		PermissionTerm permissionTerm = new PermissionTerm("id", key, key);
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.addPermissionTerm(permissionTerm);

		DataRecordGroup dataRecordGroup = recordStorage.read(
				"host" + recordLink.getLinkedRecordType(), "host" + recordLink.getLinkedRecordId());
		termCollector.MRV.setSpecificReturnValuesSupplier("collectTerms", () -> collectTerms,
				definitionId, dataRecordGroup);

		authorizator.MRV.setSpecificReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> isAuthorized, USER,
				"read",
				"host" + recordLink.getLinkedRecordType() + "." + recordLink.getLinkedRecordType(),
				List.of(permissionTerm));
	}

	private void setAuthorizedForActionRecordTypePermissionTerms(boolean isAuthorized,
			DataRecordLink recordLink) {
		String key = createKey(recordLink);

		String definitionId = "someDefintion_" + recordLink.getLinkedRecordType();
		PermissionTerm permissionTerm = new PermissionTerm("id", key, key);
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.addPermissionTerm(permissionTerm);

		DataRecordGroup dataRecordGroup = recordStorage.read(recordLink.getLinkedRecordType(),
				recordLink.getLinkedRecordId());
		termCollector.MRV.setSpecificReturnValuesSupplier("collectTerms", () -> collectTerms,
				definitionId, dataRecordGroup);

		authorizator.MRV.setSpecificReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> isAuthorized, USER,
				"read", recordLink.getLinkedRecordType(), List.of(permissionTerm));
	}

	@Test
	public void testOnlyForTest() {
		assertEquals(linkAuthorizator.onlyForTestGetDependencyProvider(), dependencyProvider);
	}
}
