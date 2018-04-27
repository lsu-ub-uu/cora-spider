package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordConflictException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordStorageDuplicateSpy implements RecordStorage {
	public List<String> requiredIds = new ArrayList<>();

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType")) {
			DataGroup group = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id,
					"true", "false");
			return group;
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		if (requiredIds.contains(id)) {
			throw new RecordConflictException("Record with recordId: " + id + " already exists");

		}
		requiredIds.add(id);

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {

	}

	@Override
	public Collection<DataGroup> readList(String type, DataGroup filter) {
		return null;
	}

	@Override
	public Collection<DataGroup> readAbstractList(String type, DataGroup filter) {
		return null;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		return null;
	}

	@Override
	public boolean recordsExistForRecordType(String type) {
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		return false;
	}
}
