package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RecordStorageDuplicateSpy implements RecordStorage {
    public List<String> requiredIds = new ArrayList<>();
    @Override
    public DataGroup read(String type, String id) {
        return null;
    }

    @Override
    public void create(String type, String id, DataGroup record, DataGroup collectedTerms, DataGroup linkList, String dataDivider) {
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
    public void update(String type, String id, DataGroup record, DataGroup collectedTerms, DataGroup linkList, String dataDivider) {

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
    public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type, String id) {
        return false;
    }
}
