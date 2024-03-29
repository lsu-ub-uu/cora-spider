/*
 * Copyright 2015, 2017, 2019, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class OldRecordStorageSpy implements RecordStorage {

	public Collection<List<String>> readLists = new ArrayList<>();
	public boolean readWasCalled = false;
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;
	public boolean updateWasCalled = false;
	public boolean linksExist = false;
	public DataGroup createRecord;
	public String type;
	public List<String> types = new ArrayList<>();
	public String id;
	public List<String> ids = new ArrayList<>();
	public int numOfTimesReadWasCalled = 0;
	public List<Filter> filters = new ArrayList<>();
	public boolean readListWasCalled = false;
	private DataGroup child1Place0002;
	private DataGroup authorityPlace0001 = DataCreator
			.createDataGroupWithNameInDataTypeAndId("abstract", "place", "place001");
	public DataGroup aRecord = DataCreator.createDataGroupWithNameInDataTypeAndId("someType",
			"someNameInData", "someId");
	public DataGroup readDataGroup;
	public DataGroup filter;
	public DataGroup dataGroupToReturn;

	@Override
	public DataGroup read(List<String> types, String id) {
		numOfTimesReadWasCalled++;
		// this.type = types;
		// this.types.add(type);
		this.id = id;
		ids.add(id);

		readWasCalled = true;
		for (String type : types) {

			if ("spyType".equals(type) && "spyId".equals(id)) {
				readDataGroup = aRecord;
				addCreatedInfoToRecordInfo(readDataGroup);
				return aRecord;
			}

			if ("abstract".equals(id)) {
				readDataGroup = DataCreator
						.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
								"false", "true", "false");
				return readDataGroup;
			}
			if ("abstract2".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "true", "false");
			}
			if ("child1".equals(type) && "place:0002".equals(id)) {
				child1Place0002 = DataCreator.createDataGroupWithNameInDataTypeAndId("unknown",
						type, id);
				readDataGroup = child1Place0002;
				return child1Place0002;
			}
			if ("child1".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "true", "false", "false");
			}
			if ("child2".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
						"abstract");
			}
			if ("child1_2".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
						"abstract2");
			}
			if ("child2_2".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
						"abstract2");
			}
			if ("otherType".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
						"NOT_ABSTRACT");
			}
			if ("spyType".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "false", "false");
			}
			if ("spyType2".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "true", "false", "false");
			}
			if ("recordType".equals(type) && "publicReadType".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "true", "false", "true");
			}
			if ("publicReadType".equals(type)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedId("publicReadType",
						"true");

			}
			if ("public".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "true", "true");
			}
			if ("notPublic".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "true", "false");
			}
			if ("publicMissing".equals(id)) {
				DataGroup publicValueIsMissing = DataCreator
						.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
								"false", "true", "false");
				publicValueIsMissing.removeFirstChildWithNameInData("public");
				return publicValueIsMissing;
			}
			if ("recordType".equals(type) && "image".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId("image",
						"true", "binary");
			}
			if ("recordType".equals(type) && "binary".equals(id)) {
				DataGroup recordTypeGroup = DataCreator
						.createRecordTypeWithIdAndUserSuppliedId("binary", "false");
				return recordTypeGroup;
			}
			if (type.equals("recordType") && ("place".equals(id))) {
				DataGroup dataGroup = DataCreator
						.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
								"authority");
				DataGroup filter = new DataGroupOldSpy("filter");
				filter.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
				filter.addChild(new DataAtomicSpy("linkedRecordId", "placeFilterGroup"));
				dataGroup.addChild(filter);
				return dataGroup;
			}

			if ("binary".equals(type) && "image:123456789".equals(id)) {
				return DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
						"binary", "image:123456789", "binary", "cora");
			}
			if ("recordType".equals(type) && "book".equals(id)) {
				DataGroup book = new DataGroupOldSpy("recordType");
				book.addChild(DataCreator.createLinkWithLinkedId("metadataId", "metadataGroup",
						"bookGroup"));
				return book;
			}
			if ("recordType".equals(type) && "abstractAuthority".equals(id)) {
				DataGroup authority = DataCreator
						.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
								"abstractAuthority", "false", "true", "false");
				return authority;
			}

			if ("book".equals(type) && "book1".equals(id)) {
				DataGroup book = new DataGroupOldSpy("book");
				DataGroup recordInfo = DataCreator2.createRecordInfoWithIdAndTypeAndLinkedRecordId(
						"book1", "book", "testSystem");
				book.addChild(recordInfo);
				return book;

			}
			if ("permissionRole".equals(type) && "guest".equals(id)) {
				return createRoleForGuest();
			}
			if ("permissionRole".equals(type) && "guestWithPermissionTerm".equals(id)) {
				return createRoleForGuest();
			}

			if ("permissionRole".equals(type) && "guestWithPermissionTerms".equals(id)) {
				DataGroup roleForGuest = createRoleForGuest();
				DataGroup permissionRuleLink = createPermissionRuleLink("ruleWithPermissionPart");
				roleForGuest.addChild(permissionRuleLink);
				return roleForGuest;
			}
			if ("permissionRole".equals(type) && "guestWithMultiplePermissionTerms".equals(id)) {
				DataGroup roleForGuest = createRoleForGuest();
				DataGroup permissionRuleLink = createPermissionRuleLink("ruleWithPermissionPart");
				roleForGuest.addChild(permissionRuleLink);

				DataGroup permissionRuleLink2 = createPermissionRuleLink(
						"ruleWithMultiplePermissionPart");

				roleForGuest.addChild(permissionRuleLink2);
				return roleForGuest;
			}

			if ("permissionRole".equals(type) && "inactive".equals(id)) {
				DataGroup permissionRole = new DataGroupOldSpy("permissionRole");
				DataGroup permissionRuleLink = createPermissionRuleLink("authorityReader");
				permissionRole.addChild(permissionRuleLink);
				permissionRole.addChild(new DataAtomicSpy("activeStatus", "inactive"));
				return permissionRole;
			}
			if ("permissionRule".equals(type) && "ruleWithPermissionPart".equals(id)) {
				DataGroup rule = new DataGroupOldSpy("permissionRule");

				DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
						"system.read");
				rule.addChild(permissionRulePart);

				DataGroup permissionTermRulePart = createPermissionTermRulePart(
						"publishedStatusPermissionTerm", "system.published");
				rule.addChild(permissionTermRulePart);

				rule.addChild(new DataAtomicSpy("activeStatus", "active"));
				return rule;
			}
			if ("permissionRule".equals(type) && "ruleWithMultiplePermissionPart".equals(id)) {
				DataGroup rule = new DataGroupOldSpy("permissionRule");

				DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
						"system.read");
				rule.addChild(permissionRulePart);

				DataGroup permissionTermRulePart = createPermissionTermRulePart(
						"publishedStatusPermissionTerm", "system.published", "system.notPublished");
				rule.addChild(permissionTermRulePart);

				DataGroup permissionTermRulePart2 = createPermissionTermRulePart(
						"deletedStatusPermissionTerm", "system.deleted", "system.notDeleted");
				rule.addChild(permissionTermRulePart2);

				rule.addChild(new DataAtomicSpy("activeStatus", "active"));
				return rule;
			}

			if ("permissionRule".equals(type) && "authorityReader".equals(id)) {
				DataGroup rule = new DataGroupOldSpy("permissionRule");

				DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
						"system.read");
				rule.addChild(permissionRulePart);

				DataGroup permissionRulePart2 = createPermissionRulePart("recordType",
						"system.person", "system.place");
				rule.addChild(permissionRulePart2);

				rule.addChild(new DataAtomicSpy("activeStatus", "active"));
				return rule;
			}
			if ("permissionRule".equals(type) && "metadataReader".equals(id)) {
				DataGroup rule = new DataGroupOldSpy("permissionRule");

				DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
						"system.read");
				rule.addChild(permissionRulePart);

				DataGroup permissionRulePart2 = createPermissionRulePart("recordType",
						"system.person", "system.place");
				rule.addChild(permissionRulePart2);

				rule.addChild(new DataAtomicSpy("activeStatus", "inactive"));
				return rule;
			}
			if ("permissionRule".equals(type) && "inactive".equals(id)) {
				DataGroup rule = new DataGroupOldSpy("permissionRule");

				DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
						"system.read");
				rule.addChild(permissionRulePart);

				rule.addChild(new DataAtomicSpy("activeStatus", "inactive"));
				return rule;
			}
			if ("permissionRole".equals(type) && "roleNotFoundInStorage".equals(id)) {
				return null;
			}

			if ("collectTerm".equals(type) && "publishedStatusPermissionTerm".equals(id)) {
				return createCollectPermissionTermWIthKey("PUBLISHED_STATUS");
			}
			if ("collectTerm".equals(type) && "deletedStatusPermissionTerm".equals(id)) {
				return createCollectPermissionTermWIthKey("DELETED_STATUS");
			}
			if ("collectTerm".equals(type) && "organisationPermissionTerm".equals(id)) {
				return createCollectPermissionTermWIthKey("OWNING_ORGANISATION");
			}
			if ("collectTerm".equals(type) && "journalPermissionTerm".equals(id)) {
				return createCollectPermissionTermWIthKey("JOURNAL_ACCESS");
			}

			if ("inactiveUserId".equals(id)) {
				DataGroup user = new DataGroupOldSpy("user");
				user.addChild(new DataAtomicSpy("activeStatus", "inactive"));
				return user;
			}
			if ("someUserId".equals(id)) {
				DataGroup user = new DataGroupOldSpy("user");
				user.addChild(new DataAtomicSpy("activeStatus", "active"));
				addRoleToUser("guest", user);

				return user;
			}

			if ("user".equals(type) && "dummy1".equals(id)) {
				DataGroup dataGroup = DataCreator2
						.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("user", "dummy1",
								"systemOneUser", "cora");
				return dataGroup;
			}

			if ("abstractAuthority".equals(type)) {
				return authorityPlace0001;

			}
			if ("place".equals(type)) {
				return authorityPlace0001;
			}
			if ("metadataGroup".equals(type) && "bookGroup".contentEquals(id)) {
				readDataGroup = new DataGroupOldSpy("bookGroup");
				readDataGroup.addChild(new DataAtomicSpy("nameInData", "book"));
				return readDataGroup;
			}
		}
		dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupOldSpy("recordInfo"));
		return dataGroupToReturn;
	}

	private void addCreatedInfoToRecordInfo(DataGroup readDataGroup) {
		DataGroup recordInfo = readDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup createdBy = new DataGroupOldSpy("createdBy");
		createdBy.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdBy.addChild(new DataAtomicSpy("linkedRecordId", "4422"));
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2014-08-01T00:00:00.000000Z"));
		readDataGroup.addChild(recordInfo);
	}

	private DataGroup createCollectPermissionTermWIthKey(String key) {
		DataGroup collectTerm = new DataGroupOldSpy("collectTerm");
		DataGroup extraData = new DataGroupOldSpy("extraData");
		collectTerm.addChild(extraData);
		extraData.addChild(new DataAtomicSpy("permissionKey", key));

		return collectTerm;
	}

	private DataGroup createPermissionTermRulePart(String permissionTermId, String... value) {
		DataGroup permissionTermRulePart = new DataGroupOldSpy("permissionTermRulePart");
		DataGroup internalRule = new DataGroupOldSpy("rule");
		permissionTermRulePart.addChild(internalRule);
		permissionTermRulePart.setRepeatId("12");
		internalRule.addChild(new DataAtomicSpy("linkedRecordType", "collectTerm"));
		internalRule.addChild(new DataAtomicSpy("linkedRecordId", permissionTermId));
		for (int idx = 0; idx < value.length; idx++) {
			permissionTermRulePart
					.addChild(new DataAtomicSpy("value", value[idx], String.valueOf(idx)));
		}
		return permissionTermRulePart;
	}

	private DataGroup createPermissionRulePart(String permissionType, String... value) {
		DataGroup permissionRulePart = new DataGroupOldSpy("permissionRulePart");
		for (int idx = 0; idx < value.length; idx++) {
			permissionRulePart.addChild(
					new DataAtomicSpy("permissionRulePartValue", value[idx], String.valueOf(idx)));
		}
		permissionRulePart.addAttributeByIdWithValue("type", permissionType);
		return permissionRulePart;
	}

	private DataGroup createPermissionRuleLink(String linkedId) {
		DataGroup permissionRuleLink = new DataGroupOldSpy("permissionRuleLink");
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordId", linkedId));
		return permissionRuleLink;
	}

	private DataGroup createRoleForGuest() {
		DataGroup permissionRole = new DataGroupOldSpy("permissionRole");

		DataGroup permissionRuleLink = new DataGroupOldSpy("permissionRuleLink");
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordId", "authorityReader"));
		permissionRole.addChild(permissionRuleLink);

		DataGroup permissionRuleLink2 = new DataGroupOldSpy("permissionRuleLink");
		permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordId", "metadataReader"));
		permissionRole.addChild(permissionRuleLink2);

		DataGroup permissionRuleLink3 = new DataGroupOldSpy("permissionRuleLink");
		permissionRuleLink3.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink3.addChild(new DataAtomicSpy("linkedRecordId", "inactive"));
		permissionRole.addChild(permissionRuleLink3);

		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	@Override
	public void create(String type, String id, DataGroup record, Set<StorageTerm> storageTerms,
			Set<Link> links, String dataDivider) {
		createWasCalled = true;
		createRecord = record;
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		deleteWasCalled = true;
	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		if ("place".equals(type)) {
			if (id.equals("place:0001")) {
				return false;
			}
		} else if ("authority".equals(type)) {
			if ("place:0003".equals(id)) {
				return true;
			}
		}
		return linksExist;
	}

	@Override
	public void update(String type, String id, DataGroup record, Set<StorageTerm> storageTerm,
			Set<Link> links, String dataDivider) {
		updateWasCalled = true;
	}

	@Override
	public StorageReadResult readList(List<String> types, Filter filter) {
		readListWasCalled = true;
		readLists.add(types);
		filters.add(filter);
		if ("recordType".equals(types)) {
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read(List.of("recordType"), "abstract"));
			recordTypes.add(read(List.of("recordType"), "child1"));
			recordTypes.add(read(List.of("recordType"), "child2"));
			recordTypes.add(read(List.of("recordType"), "abstract2"));
			recordTypes.add(read(List.of("recordType"), "child1_2"));
			recordTypes.add(read(List.of("recordType"), "child2_2"));
			recordTypes.add(read(List.of("recordType"), "otherType"));
			StorageReadResult spiderReadResult = new StorageReadResult();
			spiderReadResult.listOfDataGroups = recordTypes;
			return spiderReadResult;
		}
		if ("child1_2".equals(types)) {
			throw RecordNotFoundException
					.withMessage("No records exists with recordType: " + types);
		}
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		spiderReadResult.totalNumberOfMatches = 199;
		return spiderReadResult;
	}

	private void addRoleToUser(String roleId, DataGroup user) {
		DataGroup outerUserRole = createUserRoleWithId(roleId);
		user.addChild(outerUserRole);
	}

	private DataGroup createUserRoleWithId(String roleId) {
		DataGroup outerUserRole = new DataGroupOldSpy("userRole");
		DataGroup innerUserRole = new DataGroupOldSpy("userRole");
		innerUserRole.addChild(new DataAtomicSpy("linkedRecordType", "permissionRole"));
		innerUserRole.addChild(new DataAtomicSpy("linkedRecordId", roleId));
		outerUserRole.addChild(innerUserRole);
		return outerUserRole;
	}

	@Override
	public Set<Link> getLinksToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExists(List<String> types, String id) {
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, Filter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DataRecordGroup read(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}
}
