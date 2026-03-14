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
		recordLink1 = creatRecordLink("someType", "someId");

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
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("someException"));

		assertFalse(linkAuthorizator.isAuthorizedToReadLink(NO_NEEDED_USER, recordLink1));
	}

	@Test
	public void testIsPublicForRead_notPublic() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testIsPublicForRead_public() {
		recordTypeIsPublic(true, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitFalse() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityFalse_published_permissionUnitFalse() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(false, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_published_permissionUnitFalse() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(false, recordLink1);
		linkedRecordVisibilityIsForLink("published", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(false, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitTrue_noUnit() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(false, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		assertFalse(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}

	@Test
	public void testPublished_visibilityTrue_unpublished_permissionUnitTrue_hasUnit() {
		recordTypeIsPublic(false, recordLink1);
		recordTypeUseVisibility(true, recordLink1);
		recordTypeUsePermissionUnit(true, recordLink1);
		linkedRecordVisibilityIsForLink("unpublished", recordLink1);
		setAuthorizedForPermissionUnit(true, recordLink1);
		setAuthorizedForActionRecordTypePermissionTerms(true, recordLink1);

		assertTrue(linkAuthorizator.isAuthorizedToReadLink(USER, recordLink1));
	}
	// TODO: make sure ALL calls are cached, so same link is only work on ONCE
	// TODO: tests for hostRecord

	private DataRecordLinkSpy creatRecordLink(String type, String id) {
		DataRecordLinkSpy recordLink = new DataRecordLinkSpy();
		recordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> type);
		recordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> id);

		recordTypeHandlers.computeIfAbsent(type,
				this::createRecordTypeHandlerAndAddToDependencyProvider);

		String recordKey = createKey(recordLink);
		dataRecordGroups.computeIfAbsent(recordKey,
				_ -> createDataRecordGroupAndAddToStorage(recordLink));
		return recordLink;
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

	private void linkedRecordVisibilityIsForLink(String visibility, DataRecordLink recordLink) {
		String recordKey = recordLink.getLinkedRecordType() + recordLink.getLinkedRecordId();
		DataRecordGroupSpy dataRecordGroup = dataRecordGroups.get(recordKey);
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of(visibility));
	}

	private DataRecordGroupSpy createDataRecordGroupAndAddToStorage(DataRecordLink recordLink) {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				recordLink::getLinkedRecordType);
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				recordLink::getLinkedRecordId);

		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of(createKey(recordLink)));

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> dataRecordGroup,
				recordLink.getLinkedRecordType(), recordLink.getLinkedRecordId());
		return dataRecordGroup;
	}

	private void setAuthorizedForPermissionUnit(boolean isAuthorized, DataRecordLink recordLink) {
		authorizator.MRV.setSpecificReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit",
				() -> isAuthorized, USER, createKey(recordLink));
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
}
