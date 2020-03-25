package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordTypeHandlerStorageSpy implements RecordStorage {

	public String type;
	public List<String> types = new ArrayList<>();
	public List<String> ids = new ArrayList<>();
	public String id;
	public int numberOfChildsWithConstraint = 0;

	@Override
	public DataGroup read(String type, String id) {
		this.type = type;
		types.add(type);
		ids.add(id);
		this.id = id;
		if ("recordType".equals(type) && "organisation".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedId(id, "true");

		}
		if ("metadataGroup".equals(type) && "organisation".equals(id)) {
			DataGroupSpy dataGroupSpy = new DataGroupSpy("metadata");
			DataGroupSpy childReferences = new DataGroupSpy("childReferences");
			DataGroupSpy childReference = createChildReference("metadataGroup",
					"divaOrganisationNameGroup");
			childReferences.addChild(childReference);

			if (numberOfChildsWithConstraint > 0) {
				DataGroupSpy referenceWithConstraint = createChildWithConstraint(
						"metadataTextVariable", "divaOrganisationRoot", "readWrite");
				childReferences.addChild(referenceWithConstraint);

			}
			if (numberOfChildsWithConstraint > 1) {
				DataGroupSpy referenceWithConstraint2 = createChildWithConstraint(
						"metadataTextVariable", "showInPortalTextVar", "readWrite");
				childReferences.addChild(referenceWithConstraint2);

			}
			if (numberOfChildsWithConstraint > 2) {
				DataGroupSpy referenceWithConstraint3 = createChildWithConstraint(
						"metadataTextVariable", "showInDefenceTextVar", "write");
				childReferences.addChild(referenceWithConstraint3);

			}

			dataGroupSpy.addChild(childReferences);
			return dataGroupSpy;

		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "organisationRoot"));
			return metadataTextVariable;
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "showInPortal"));
			return metadataTextVariable;
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar".equals(id)) {
			DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
			metadataTextVariable.addChild(new DataAtomicSpy("nameInData", "showInDefence"));
			return metadataTextVariable;
		}
		return new DataGroupSpy(id);
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
