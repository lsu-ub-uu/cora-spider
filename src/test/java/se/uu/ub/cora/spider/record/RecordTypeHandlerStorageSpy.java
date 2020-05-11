package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordTypeHandlerStorageSpy implements RecordStorage {

	public String type;
	public List<String> types = new ArrayList<>();
	public List<String> ids = new ArrayList<>();
	public String id;
	public int numberOfChildrenWithReadConstraint = 0;
	public int numberOfChildrenWithWriteConstraint = 0;

	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		this.type = type;
		types.add(type);
		ids.add(id);
		this.id = id;
		if ("recordType".equals(type) && "organisation".equals(id)) {
			DataGroup returnedValue = DataCreator.createRecordTypeWithIdAndUserSuppliedId(id,
					"true");
			MCR.addReturned(returnedValue);
			return returnedValue;

		}
		if ("metadataGroup".equals(type) && "organisation".equals(id)) {
			DataGroupSpy dataGroupSpy = new DataGroupSpy("metadata");
			DataGroupSpy childReferences = new DataGroupSpy("childReferences");
			DataGroupSpy childReference = createChildReference("metadataGroup",
					"divaOrganisationNameGroup");
			childReferences.addChild(childReference);

			if (numberOfChildrenWithReadConstraint > 0) {
				DataGroupSpy referenceWithConstraint = createChildWithConstraint(
						"metadataTextVariable", "divaOrganisationRoot", "readWrite");
				childReferences.addChild(referenceWithConstraint);

			}
			if (numberOfChildrenWithReadConstraint > 1) {
				DataGroupSpy referenceWithConstraint2 = createChildWithConstraint(
						"metadataTextVariable", "showInPortalTextVar", "readWrite");
				childReferences.addChild(referenceWithConstraint2);

			}
			if (numberOfChildrenWithWriteConstraint > 0) {
				DataGroupSpy referenceWithConstraint3 = createChildWithConstraint(
						"metadataTextVariable", "showInDefenceTextVar", "write");
				childReferences.addChild(referenceWithConstraint3);

			}

			dataGroupSpy.addChild(childReferences);
			MCR.addReturned(dataGroupSpy);
			return dataGroupSpy;

		}
		if ("metadataGroup".equals(type) && "organisationNew".equals(id)) {
			DataGroupSpy dataGroupSpy = new DataGroupSpy("metadata");
			DataGroupSpy childReferences = new DataGroupSpy("childReferences");
			DataGroupSpy childReference = createChildReference("metadataGroup",
					"divaOrganisationNameGroup");
			childReferences.addChild(childReference);

			if (numberOfChildrenWithReadConstraint > 0) {
				DataGroupSpy referenceWithConstraint = createChildWithConstraint(
						"metadataTextVariable", "divaOrganisationRoot2", "readWrite");
				childReferences.addChild(referenceWithConstraint);

			}
			if (numberOfChildrenWithReadConstraint > 1) {
				DataGroupSpy referenceWithConstraint2 = createChildWithConstraint(
						"metadataTextVariable", "showInPortalTextVar2", "readWrite");
				childReferences.addChild(referenceWithConstraint2);

			}
			if (numberOfChildrenWithWriteConstraint > 0) {
				DataGroupSpy referenceWithConstraint3 = createChildWithConstraint(
						"metadataTextVariable", "showInDefenceTextVar2", "write");
				childReferences.addChild(referenceWithConstraint3);

			}

			dataGroupSpy.addChild(childReferences);
			MCR.addReturned(dataGroupSpy);
			return dataGroupSpy;

		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "organisationRoot"));
			MCR.addReturned(metadataTextVariable);
			return metadataTextVariable;
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "showInPortal"));
			MCR.addReturned(metadataTextVariable);
			return metadataTextVariable;
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "showInDefence"));
			MCR.addReturned(metadataTextVariable);
			return metadataTextVariable;
		}
		DataGroupSpy returnedValue = new DataGroupSpy(id);
		MCR.addReturned(returnedValue);
		return returnedValue;
	}

	private DataGroupSpy createChildWithConstraint(String linkedRecordType, String linkedRecordId,
			String constraint) {
		DataGroupSpy referenceWithConstraint = createChildReference(linkedRecordType,
				linkedRecordId);
		referenceWithConstraint.addChild(new DataAtomicSpy("recordPartConstraint", constraint));
		return referenceWithConstraint;
	}

	private DataGroupSpy createChildReference(String linkedRecordType, String linkedRecordId) {
		DataGroupSpy childReference = new DataGroupSpy("childReference");
		DataGroupSpy ref = new DataGroupSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", linkedRecordType));
		ref.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		childReference.addChild(ref);
		return childReference;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
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
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
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
	public boolean recordsExistForRecordType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		// TODO Auto-generated method stub
		return false;
	}

}
