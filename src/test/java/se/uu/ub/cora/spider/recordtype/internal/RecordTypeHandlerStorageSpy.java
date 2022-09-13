package se.uu.ub.cora.spider.recordtype.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.RecordToRecordLink;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordTypeHandlerStorageSpy implements RecordStorage {

	public String type;
	public List<String> types = new ArrayList<>();
	public List<String> ids = new ArrayList<>();
	public String id;
	public int numberOfChildrenWithReadWriteConstraint = 0;
	public int numberOfChildrenWithWriteConstraint = 0;
	public int numberOfGrandChildrenWithReadWriteConstraint = 0;
	public String maxNoOfGrandChildren = "1";
	// public int numberOfAttributes = 0;
	public List<String> attributesIdsToAddToConstraint = new LinkedList<String>();
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
		if ("recordType".equals(type)) {
			if ("organisation".equals(id) || "organisationChildWithAttribute".equals(id)
					|| "organisationRecursiveChild".equals(id)) {
				DataGroup returnedValue = DataCreator.createRecordTypeWithIdAndUserSuppliedId(id,
						"true");
				MCR.addReturned(returnedValue);
				return returnedValue;

			}
		}
		if ("metadataGroup".equals(type) && "organisation".equals(id)) {
			// return createMetadataGroupForOrganisation();
			DataGroup metadata = createMetadataGroupForOrganisation();
			MCR.addReturned(metadata);
			return metadata;

		}

		if ("metadataGroup".equals(type) && "organisationChildWithAttribute".equals(id)) {
			DataGroup metadata = createMetadataGroupForOrganisationWithChildWithAttribute();
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataGroup".equals(type) && "organisationChildWithAttributeNew".equals(id)) {
			if (useStandardMetadataGroupForNew) {
				DataGroup metadata = createMetadataGroupForOrganisationWithChildWithAttribute();
				MCR.addReturned(metadata);
				return metadata;
			}
			DataGroup metadata = createMetadataGroupForOrganisationNewWithChildWithAttribute();
			MCR.addReturned(metadata);
			return metadata;
		}

		if ("metadataGroup".equals(type) && "organisationRecursiveChild".equals(id)) {
			DataGroup returnedValue = createMetadataGroupForOrganisationRecursiveChild();
			MCR.addReturned(returnedValue);
			return returnedValue;

		}
		if ("metadataGroup".equals(type) && "organisationNew".equals(id)) {
			if (useStandardMetadataGroupForNew) {
				DataGroup metadata = createMetadataGroupForOrganisation();
				MCR.addReturned(metadata);
				return metadata;
			}
			DataGroup metadata = createMetadataGroupForOrganisationNew();
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("organisationRoot");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("showInPortal");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("showInDefence");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "divaOrganisationRoot2".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("organisationRoot2");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "showInPortalTextVar2".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("showInPortal2");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "showInDefenceTextVar2".equals(id)) {
			DataGroup metadata = createMetadataTextVariableUsingNameInData("showInDefence2");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataTextVariable".equals(type) && "greatGrandChildTextVar".equals(id)) {
			DataGroupOldSpy metadata = createMetadataTextVariableUsingNameInData("greatGrandChild");
			MCR.addReturned(metadata);
			return metadata;
		}
		if ("metadataGroup".equals(type) && "organisationAlternativeNameGroup".equals(id)) {
			DataGroupOldSpy metadataGroup = createMetadataGroupWithChildReferences(
					"organisationAlternativeName", "organisationAlternativeNameGroup");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			if (!attributesIdsToAddToConstraint.isEmpty()) {
				DataGroupOldSpy attributeReferences = new DataGroupOldSpy("attributeReferences");
				for (String attributeId : attributesIdsToAddToConstraint) {
					DataGroupOldSpy ref = createAttributeReference(attributeId, "0");
					attributeReferences.addChild(ref);
				}
				metadataGroup.addChild(attributeReferences);
			}

			if (numberOfGrandChildrenWithReadWriteConstraint > 0) {
				DataGroupOldSpy grandChildWithReadConstraint = createChildReferenceWithConstraint(
						"metadataTextVariable", "showInPortalTextVar", "readWrite", "0", "1");
				childReferences.addChild(grandChildWithReadConstraint);
			}
			if (numberOfGrandChildrenWithReadWriteConstraint > 1) {
				DataGroupOldSpy grandChildWithReadConstraint = createChildReference("metadataGroup",
						"grandChildGroup", "0", maxNoOfGrandChildren);
				childReferences.addChild(grandChildWithReadConstraint);
			}

			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataGroup".equals(type) && "grandChildGroup".equals(id)) {
			DataGroupOldSpy metadataGroup = createMetadataGroupWithChildReferences(id,
					"grandChildGroup");

			DataGroupOldSpy greatGrandChildWithReadConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "greatGrandChildTextVar", "readWrite", "0", "1");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			childReferences.addChild(greatGrandChildWithReadConstraint);
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataGroup".equals(type) && "divaOrganisationNameGroup".equals(id)) {
			DataGroupOldSpy metadataGroup = createMetadataGroupWithChildReferences(
					"organisationName", "divaOrganisationNameGroup");

			DataGroupOldSpy grandChildWithReadConstraint = createChildReference(
					"metadataTextVariable", "showInPortalTextVar", "0", "1");
			DataGroup childReferences = metadataGroup
					.getFirstGroupWithNameInData("childReferences");
			childReferences.addChild(grandChildWithReadConstraint);
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataGroup".equals(type) && "divaOrganisationRecursiveNameGroup".equals(id)) {
			// DataGroupSpy metadataGroup =
			// createMetadataGroupWithChildReferences("organisationName",
			// "divaOrganisationRecursiveNameGroup");

			DataGroupOldSpy metadataGroup = createBasicMetadataGroup(
					"divaOrganisationRecursiveNameGroup");
			metadataGroup.addChild(new DataAtomicSpy("nameInData", "organisationName"));
			DataGroupOldSpy childReferences = new DataGroupOldSpy("childReferences");
			metadataGroup.addChild(childReferences);

			DataGroupOldSpy grandChildWithReadConstraint = createChildReference(
					"metadataTextVariable", "showInPortalTextVar", "0", "1");
			childReferences.addChild(grandChildWithReadConstraint);

			DataGroupOldSpy recursiveChild = createChildReference("metadataGroup",
					"divaOrganisationRecursiveNameGroup", "0", "1");
			childReferences.addChild(recursiveChild);

			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataCollectionVariable".equals(type) && "textPartTypeCollectionVar".equals(id)) {
			DataGroupOldSpy metadataGroup = createMetadataGroupWithNameInDataAndFinalValue("type",
					"default");
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		// TODO: if ("metadataCollectionVariable".equals(type) &&
		// "choosableAttributesCollectionVar".equals(id))
		// if ("metadataCollectionVariable".equals(type) &&
		// "choosableAttributesCollectionVar".equals(id)) {
		//
		// DataGroupSpy metadataGroup = new DataGroupSpy();
		// metadataGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
		// Supplier, "bla");
		// return metadataGroup;
		// }
		if ("metadataCollectionVariable".equals(type) && "textPartLangCollectionVar".equals(id)) {
			DataGroupOldSpy metadataGroup = createMetadataGroupWithNameInDataAndFinalValue("lang",
					"sv");
			MCR.addReturned(metadataGroup);
			return metadataGroup;
		}
		if ("metadataCollectionVariable".equals(type)
				&& "choosableAttributeCollectionVar".equals(id)) {
			DataGroupSpy collectionVar = new DataGroupSpy();

			DataGroupSpy refCollection = new DataGroupSpy();

			collectionVar.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
					(Supplier<Boolean>) () -> false, "finalValue");

			collectionVar.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
					(Supplier<DataGroupSpy>) () -> refCollection, "refCollection");
			String collectionId = "choosableCollection";
			refCollection.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
					(Supplier<String>) () -> collectionId, "linkedRecordId");

			MCR.addReturned(collectionVar);
			return collectionVar;
		}

		if ("metadataItemCollection".equals(type) && "choosableCollection".equals(id)) {
			DataGroupSpy collection = new DataGroupSpy();

			DataGroupSpy collectionItemReferences = new DataGroupSpy();
			collection.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
					(Supplier<DataGroupSpy>) () -> collectionItemReferences,
					"collectionItemReferences");

			List<DataGroupSpy> refs = new ArrayList<>();
			refs.add(createCollectionItemWithId("choosableCollectionItem1"));
			refs.add(createCollectionItemWithId("choosableCollectionItem2"));
			collectionItemReferences.MRV.setSpecificReturnValuesSupplier(
					"getAllGroupsWithNameInData", (Supplier<List<DataGroupSpy>>) () -> refs, "ref");

			MCR.addReturned(collection);
			return collection;
		}
		if ("metadataCollectionItem".equals(type) && "choosableCollectionItem1".equals(id)) {
			DataGroupSpy collectionItem = createCollectionItemWithValue("choosableItemValue1");
			MCR.addReturned(collectionItem);
			return collectionItem;
		}
		if ("metadataCollectionItem".equals(type) && "choosableCollectionItem2".equals(id)) {
			DataGroupSpy collectionItem = createCollectionItemWithValue("choosableItemValue2");
			MCR.addReturned(collectionItem);
			return collectionItem;
		}
		DataGroupOldSpy returnedValue = new DataGroupOldSpy(id);
		MCR.addReturned(returnedValue);
		return returnedValue;
	}

	private DataGroupSpy createCollectionItemWithValue(String itemValue) {
		DataGroupSpy collectionItem = new DataGroupSpy();

		// itemValue = "choosableItemValue1";
		// collectionItem.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
		// (Supplier<String>) () -> itemValue, "collectionItemReferences");
		collectionItem.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> itemValue, "nameInData");
		MCR.addReturned(collectionItem);
		return collectionItem;
	}

	private DataGroupSpy createCollectionItemWithId(String collectionItemId) {
		DataGroupSpy ref1 = new DataGroupSpy();
		ref1.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> collectionItemId, "linkedRecordId");
		return ref1;
	}

	private DataGroupOldSpy createMetadataGroupWithChildReferences(String nameInData,
			String groupId) {
		DataGroupOldSpy metadataGroup = createBasicMetadataGroup(groupId);
		metadataGroup.addChild(new DataAtomicSpy("nameInData", nameInData));
		DataGroupOldSpy childReferences = new DataGroupOldSpy("childReferences");
		metadataGroup.addChild(childReferences);
		return metadataGroup;
	}

	private DataGroup createMetadataGroupForOrganisationNew() {
		DataGroupOldSpy dataGroupSpy = createBasicMetadataGroup("organinsationNewGroup");
		DataGroupOldSpy childReferences = new DataGroupOldSpy("childReferences");
		DataGroupOldSpy childReference = createChildReference("metadataGroup",
				"divaOrganisationNameGroup", "0", "1");
		childReferences.addChild(childReference);

		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "divaOrganisationRoot2", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);

		}
		if (numberOfChildrenWithReadWriteConstraint > 1) {
			DataGroupOldSpy referenceWithConstraint2 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInPortalTextVar2", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint2);

		}
		if (numberOfChildrenWithWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint3 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInDefenceTextVar2", "write", "0", "1");
			childReferences.addChild(referenceWithConstraint3);

		}

		dataGroupSpy.addChild(childReferences);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataGroupOldSpy createMetadataGroupWithNameInDataAndFinalValue(String nameInData,
			String finalValue) {
		DataGroupOldSpy metadataGroup = createBasicMetadataGroup(nameInData + "Id");

		metadataGroup.addChild(new DataAtomicSpy("nameInData", nameInData));
		metadataGroup.addChild(new DataAtomicSpy("finalValue", finalValue));
		return metadataGroup;
	}

	private DataGroupOldSpy createBasicMetadataGroup(String id) {
		DataGroupOldSpy metadataGroup = new DataGroupOldSpy("metadata");
		DataGroupOldSpy recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		metadataGroup.addChild(recordInfo);
		return metadataGroup;
	}

	private DataGroupOldSpy createAttributeReference(String linkedRecordId, String repeatId) {
		DataGroupOldSpy ref = new DataGroupOldSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", "metadataCollectionVariable"));
		ref.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		ref.setRepeatId(repeatId);
		return ref;
	}

	private DataGroupOldSpy createMetadataTextVariableUsingNameInData(String nameInData) {
		DataGroupOldSpy metadataTextVariable = createBasicMetadataGroup(nameInData + "Id");
		metadataTextVariable.addChild(new DataAtomicSpy("nameInData", nameInData));
		MCR.addReturned(metadataTextVariable);
		return metadataTextVariable;
	}

	private DataGroup createMetadataGroupForOrganisation() {
		DataGroupOldSpy dataGroupSpy = createBasicMetadataGroup("organisationGroup");
		DataGroupOldSpy childReferences = new DataGroupOldSpy("childReferences");
		DataGroupOldSpy childReference = createChildReference("metadataGroup",
				"divaOrganisationNameGroup", "0", "1");
		childReferences.addChild(childReference);

		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataTextVariable", "divaOrganisationRoot", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);

		}
		if (numberOfChildrenWithReadWriteConstraint > 1) {
			DataGroupOldSpy referenceWithConstraint2 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInPortalTextVar", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint2);

		}
		if (numberOfChildrenWithWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint3 = createChildReferenceWithConstraint(
					"metadataTextVariable", "showInDefenceTextVar", "write", "0", "1");
			childReferences.addChild(referenceWithConstraint3);

		}

		dataGroupSpy.addChild(childReferences);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataGroup createMetadataGroupForOrganisationRecursiveChild() {
		DataGroupOldSpy dataGroupSpy = createBasicMetadataGroup("recursiveChild");
		DataGroupOldSpy childReferences = new DataGroupOldSpy("childReferences");
		DataGroupOldSpy childReference = createChildReference("metadataGroup",
				"divaOrganisationRecursiveNameGroup", "0", "1");
		childReferences.addChild(childReference);
		dataGroupSpy.addChild(childReferences);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataGroup createMetadataGroupForOrganisationWithChildWithAttribute() {
		DataGroup metadataGroup = createMetadataGroupForOrganisation();
		DataGroup childReferences = metadataGroup.getFirstGroupWithNameInData("childReferences");
		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataGroup", "organisationAlternativeNameGroup", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);
		}

		return metadataGroup;
	}

	private DataGroup createMetadataGroupForOrganisationNewWithChildWithAttribute() {
		DataGroup metadataGroup = createMetadataGroupForOrganisationNew();
		DataGroup childReferences = metadataGroup.getFirstGroupWithNameInData("childReferences");
		if (numberOfChildrenWithReadWriteConstraint > 0) {
			DataGroupOldSpy referenceWithConstraint = createChildReferenceWithConstraint(
					"metadataGroup", "organisationAlternativeNameGroup", "readWrite", "0", "1");
			childReferences.addChild(referenceWithConstraint);
		}

		return metadataGroup;
	}

	private DataGroupOldSpy createChildReferenceWithConstraint(String linkedRecordType,
			String linkedRecordId, String constraint, String repeatMin, String repeatMax) {
		DataGroupOldSpy referenceWithConstraint = createChildReference(linkedRecordType,
				linkedRecordId, repeatMin, repeatMax);
		referenceWithConstraint.addChild(new DataAtomicSpy("recordPartConstraint", constraint));
		return referenceWithConstraint;
	}

	private DataGroupOldSpy createChildReference(String linkedRecordType, String linkedRecordId,
			String repeatMin, String repeatMax) {
		DataGroupOldSpy childReference = new DataGroupOldSpy("childReference");
		DataGroupOldSpy ref = new DataGroupOldSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", linkedRecordType));
		ref.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		childReference.addChild(ref);
		childReference.addChild(new DataAtomicSpy("repeatMin", repeatMin));
		childReference.addChild(new DataAtomicSpy("repeatMax", repeatMax));
		return childReference;
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<RecordToRecordLink> links, String dataDivider) {
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> collectedTerms,
			List<RecordToRecordLink> links, String dataDivider) {
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
