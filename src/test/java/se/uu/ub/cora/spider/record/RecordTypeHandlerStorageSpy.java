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
	public int numberOfChildrenWithReadWriteConstraint = 0;
	public int numberOfChildrenWithWriteConstraint = 0;
	public int numberOfGrandChildrenWithReadWriteConstraint = 0;
	public String maxNoOfGrandChildren = "1";
	public int numberOfAttributes = 0;
	public boolean addAttribute = false;
	public boolean useStandardMetadataGroupForNew = false;

	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		this.type = type;
		types.add(type);
		ids.add(id);
		this.id = id;
		if ("recordType".equals(type) && "organisation".equals(id)
				|| "recordType".equals(type) && "organisationChildWithAttribute".equals(id)) {
			DataGroup returnedValue = DataCreator.createRecordTypeWithIdAndUserSuppliedId(id,
					"true");
			MCR.addReturned(returnedValue);
			return returnedValue;

		}
		if ("metadataGroup".equals(type) && "organisation".equals(id)) {
			return createMetadataGroupForOrganisation();

		}
		if ("metadataGroup".equals(type) && "organisationChildWithAttribute".equals(id)) {
			return createMetadataGroupForOrganisationWithChildWithAttribute();

		}
		if ("metadataGroup".equals(type) && "organisationChildWithAttributeNew".equals(id)) {
			if (useStandardMetadataGroupForNew) {
				return createMetadataGroupForOrganisationWithChildWithAttribute();
			}
			return createMetadataGroupForOrganisationNewWithChildWithAttribute();

		}
		if ("metadataGroup".equals(type) && "organisationNew".equals(id)) {
			if (useStandardMetadataGroupForNew) {
				return createMetadataGroupForOrganisation();
			}
			return createMetadataGroupForOrganisationNew();
		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot".equals(id)) {
			return createMetadataTextVariableUsingNameInData("organisationRoot");
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar".equals(id)) {
			return createMetadataTextVariableUsingNameInData("showInPortal");
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar".equals(id)) {
			return createMetadataTextVariableUsingNameInData("showInDefence");
		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot2".equals(id)) {
			return createMetadataTextVariableUsingNameInData("organisationRoot2");
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar2".equals(id)) {
			return createMetadataTextVariableUsingNameInData("showInPortal2");
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar2".equals(id)) {
			return createMetadataTextVariableUsingNameInData("showInDefence2");
		}
		if ("metadataTextVariable".equals(type) && "greatGrandChildTextVar".equals(id)) {
			return createMetadataTextVariableUsingNameInData("greatGrandChild");
		}
		if ("metadataGroup".equals(type) && "organisationAlternativeNameGroup".equals(id)) {
			DataGroupSpy metadataGroup = createMetadataGroupWithChildReferences(
					"organisationAlternativeName");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			if (numberOfAttributes > 0) {
				DataGroupSpy attributeReferences = new DataGroupSpy("attributeReferences");
				DataGroupSpy ref = createAttributeReference("textPartTypeCollectionVar", "0");
				attributeReferences.addChild(ref);

				if (numberOfAttributes > 1) {
					DataGroupSpy ref2 = createAttributeReference("textPartLangCollectionVar", "0");
					attributeReferences.addChild(ref2);
				}
				metadataGroup.addChild(attributeReferences);
			}
			if (numberOfGrandChildrenWithReadWriteConstraint > 0) {
				DataGroupSpy grandChildWithReadConstraint = createChildReferenceWithConstraint(
						"metadataTextVariable", "showInPortalTextVar", "readWrite", "0", "1");
				childReferences.addChild(grandChildWithReadConstraint);
			}
			if (numberOfGrandChildrenWithReadWriteConstraint > 1) {
				DataGroupSpy grandChildWithReadConstraint = createChildReference("metadataGroup",
						"grandChildGroup", "0", maxNoOfGrandChildren);
				childReferences.addChild(grandChildWithReadConstraint);
			}

			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataGroup".equals(type) && "grandChildGroup".equals(id)) {
			DataGroupSpy metadataGroup = createMetadataGroupWithChildReferences(id);

			DataGroupSpy greatGrandChildWithReadConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "greatGrandChildTextVar", "readWrite", "0", "1");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			childReferences.addChild(greatGrandChildWithReadConstraint);
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataGroup".equals(type) && "divaOrganisationNameGroup".equals(id)) {
			DataGroupSpy metadataGroup = createMetadataGroupWithChildReferences("organisationName");

			DataGroupSpy grandChildWithReadConstraint = createChildReference("metadataTextVariable",
					"showInPortalTextVar", "0", "1");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			childReferences.addChild(grandChildWithReadConstraint);
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataCollectionVariable".equals(type) && "textPartTypeCollectionVar".equals(id)) {
			DataGroupSpy metadataGroup = createMetadataGroupWithNameInDataAndFinalValue("type",
					"default");
			return metadataGroup;
		}
		if ("metadataCollectionVariable".equals(type) && "textPartLangCollectionVar".equals(id)) {
			DataGroupSpy metadataGroup = createMetadataGroupWithNameInDataAndFinalValue("lang",
					"sv");
			return metadataGroup;
		}
		DataGroupSpy returnedValue = new DataGroupSpy(id);
		MCR.addReturned(returnedValue);
		return returnedValue;
	}

	private DataGroupSpy createMetadataGroupWithChildReferences(String nameInData) {
		DataGroupSpy metadataGroup = new DataGroupSpy("metadata");
		metadataGroup.addChild(new DataAtomicSpy("nameInData", nameInData));
		DataGroupSpy childReferences = new DataGroupSpy("childReferences");
		metadataGroup.addChild(childReferences);
		return metadataGroup;
	}

	private DataGroup createMetadataGroupForOrganisationNew() {
		DataGroupSpy dataGroupSpy = new DataGroupSpy("metadata");
		DataGroupSpy childReferences = new DataGroupSpy("childReferences");
		DataGroupSpy childReference = createChildReference("metadataGroup",
				"divaOrganisationNameGroup", "0", "1");
		childReferences.addChild(childReference);

		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "divaOrganisationRoot2", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);

		}
		if (numberOfChildrenWithReadWriteConstraint > 1) {
			DataGroupSpy referenceWithConstraint2 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInPortalTextVar2", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint2);

		}
		if (numberOfChildrenWithWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint3 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInDefenceTextVar2", "write", "0", "1");
			childReferences.addChild(referenceWithConstraint3);

		}

		dataGroupSpy.addChild(childReferences);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataGroupSpy createMetadataGroupWithNameInDataAndFinalValue(String nameInData,
			String finalValue) {
		DataGroupSpy metadataGroup = new DataGroupSpy("metadata");

		metadataGroup.addChild(new DataAtomicSpy("nameInData", nameInData));
		metadataGroup.addChild(new DataAtomicSpy("finalValue", finalValue));
		return metadataGroup;
	}

	private DataGroupSpy createAttributeReference(String linkedRecordId, String repeatId) {
		DataGroupSpy ref = new DataGroupSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", "metadataCollectionVariable"));
		ref.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		ref.setRepeatId(repeatId);
		return ref;
	}

	private DataGroupSpy createMetadataTextVariableUsingNameInData(String nameInData) {
		DataGroupSpy metadataTextVariable = new DataGroupSpy("metadata");
		metadataTextVariable.addChild(new DataAtomicSpy("nameInData", nameInData));
		MCR.addReturned(metadataTextVariable);
		return metadataTextVariable;
	}

	private DataGroup createMetadataGroupForOrganisation() {
		DataGroupSpy dataGroupSpy = new DataGroupSpy("metadata");
		DataGroupSpy childReferences = new DataGroupSpy("childReferences");
		DataGroupSpy childReference = createChildReference("metadataGroup",
				"divaOrganisationNameGroup", "0", "1");
		childReferences.addChild(childReference);

		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "divaOrganisationRoot", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);

		}
		if (numberOfChildrenWithReadWriteConstraint > 1) {
			DataGroupSpy referenceWithConstraint2 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInPortalTextVar", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint2);

		}
		if (numberOfChildrenWithWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint3 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInDefenceTextVar", "write", "0", "1");
			childReferences.addChild(referenceWithConstraint3);

		}

		dataGroupSpy.addChild(childReferences);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataGroup createMetadataGroupForOrganisationWithChildWithAttribute() {
		DataGroup metadataGroup = createMetadataGroupForOrganisation();
		DataGroup childReferences = metadataGroup.getFirstGroupWithNameInData("childReferences");
		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataGroup", "organisationAlternativeNameGroup", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);
		}

		return metadataGroup;
	}

	private DataGroup createMetadataGroupForOrganisationNewWithChildWithAttribute() {
		DataGroup metadataGroup = createMetadataGroupForOrganisationNew();
		DataGroup childReferences = metadataGroup.getFirstGroupWithNameInData("childReferences");
		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataGroup", "organisationAlternativeNameGroup", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);
		}

		return metadataGroup;
	}

	private DataGroupSpy createChildReferenceWithConstraint(String linkedRecordType,
			String linkedRecordId, String constraint, String repeatMin, String repeatMax) {
		DataGroupSpy referenceWithConstraint = createChildReference(linkedRecordType,
				linkedRecordId, repeatMin, repeatMax);
		referenceWithConstraint.addChild(new DataAtomicSpy("recordPartConstraint", constraint));
		return referenceWithConstraint;
	}

	private DataGroupSpy createChildReference(String linkedRecordType, String linkedRecordId,
			String repeatMin, String repeatMax) {
		DataGroupSpy childReference = new DataGroupSpy("childReference");
		DataGroupSpy ref = new DataGroupSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", linkedRecordType));
		ref.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		childReference.addChild(ref);
		childReference.addChild(new DataAtomicSpy("repeatMin", repeatMin));
		childReference.addChild(new DataAtomicSpy("repeatMax", repeatMax));
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
