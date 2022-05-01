package se.uu.ub.cora.spider.spy;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordStorageSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public RecordStorageSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("read", DataGroupSpy::new);
		// MRV.setDefaultReturnValuesSupplier("hasReadAction", (Supplier<Boolean>) () -> false);
		// MRV.setDefaultReturnValuesSupplier("getRepeatId", String::new);
		// MRV.setDefaultReturnValuesSupplier("getNameInData", String::new);
		// MRV.setDefaultReturnValuesSupplier("hasAttributes", (Supplier<Boolean>) () -> false);
		// MRV.setDefaultReturnValuesSupplier("getAttribute", DataAttributeSpy::new);
		// MRV.setDefaultReturnValuesSupplier("getAttributes", ArrayList<DataAttribute>::new);
		// MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", String::new);
		// MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", String::new);
	}

	@Override
	public DataGroup read(String type, String id) {
		return (DataGroup) MCR.addCallAndReturnFromMRV("type", type, "id", id);
	}

	@Override
	public void create(String type, String id, DataGroup dataRecord, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup dataRecord, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForType(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalNumberOfRecordsForAbstractType(String abstractType,
			List<String> implementingTypes, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
