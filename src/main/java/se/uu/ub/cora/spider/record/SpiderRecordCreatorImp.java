/*
 * Copyright 2015 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordCreatorImp extends SpiderRecordHandler
		implements SpiderRecordCreator {
	private static final String USER = "User:";
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private DataGroup recordTypeDefinition;
	private SpiderDataGroup spiderDataGroup;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;

	public static SpiderRecordCreatorImp usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator,
			DataRecordLinkCollector linkCollector) {
		return new SpiderRecordCreatorImp(authorization, dataValidator, recordStorage, idGenerator,
				keyCalculator, linkCollector);
	}

	private SpiderRecordCreatorImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator, DataRecordLinkCollector linkCollector) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;
		this.linkCollector = linkCollector;

	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String userId, String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		this.recordType = recordTypeToCreate;
		this.spiderDataGroup = spiderDataGroup;
		recordTypeDefinition = getRecordTypeDefinition();
		metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("newMetadataId");

		checkNoCreateForAbstractRecordType(recordType);
		validateDataInRecordAsSpecifiedInMetadata();

		validateInheritanceRules(recordTypeToCreate);

		ensureCompleteRecordInfo(userId, recordType);

		// set more stuff, user, tscreated, status (created, updated, deleted,
		// etc), published
		// (true, false)
		// set owning organisation

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreateIncomingData(userId, recordType, topLevelDataGroup);

		String id = extractIdFromData();

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, id);

		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(spiderDataGroup);
		recordStorage.create(recordType, id, topLevelDataGroup, collectedLinks, dataDivider);

		SpiderDataGroup spiderDataGroupWithActions = SpiderDataGroup
				.fromDataGroup(topLevelDataGroup);

		return createDataRecordContainingDataGroup(spiderDataGroupWithActions);

	}

	private String extractIdFromData() {
		return spiderDataGroup.extractGroup("recordInfo").extractAtomicValue("id");
	}

	private void checkNoCreateForAbstractRecordType(String recordTypeToCreate) {
		if (isRecordTypeAbstract()) {
			throw new MisuseException("Data creation on abstract recordType:" + recordTypeToCreate
					+ " is not allowed");
		}
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		DataGroup record = spiderDataGroup.toDataGroup();

		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void validateInheritanceRules(String recordTypeToCreate) {
		if(recordTypeIsMetadataGroup(recordTypeToCreate) && dataGroupHasParent()){
			ensureAllChildrenExistsInParent();
		}
	}

	private boolean recordTypeIsMetadataGroup(String recordTypeToCreate) {
		return "metadataGroup".equals(recordTypeToCreate);
	}

	private boolean dataGroupHasParent(){
		return spiderDataGroup.containsChildWithNameInData("refParentId");
	}

	private void ensureAllChildrenExistsInParent(){
		SpiderDataGroup childReferences = (SpiderDataGroup) spiderDataGroup.getFirstChildWithNameInData("childReferences");

		for(SpiderDataElement childReference : childReferences.getChildren()){
			String childNameInData = getNameInDataFromChildReference(childReference);
			ensureChildExistInParent(childNameInData);
		}
	}

	private void ensureChildExistInParent(String childNameInData) {
		SpiderDataGroup parentChildReferences = getParentChildReferences();
		boolean childFound = false;
		for(SpiderDataElement parentChildReference : parentChildReferences.getChildren()){
			childFound = isSameNameInData(childNameInData, parentChildReference, childFound);
        }
		if(!childFound){
            throw new DataException("Data is not valid: child does not exist in parent");
        }
	}

	private SpiderDataGroup getParentChildReferences() {
		SpiderDataAtomic refParentId = (SpiderDataAtomic)spiderDataGroup.getFirstChildWithNameInData("refParentId");
		SpiderDataGroup parent = SpiderDataGroup.fromDataGroup(recordStorage.read("metadataGroup", refParentId.getValue()));

		return (SpiderDataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	private boolean isSameNameInData(String childNameInData, SpiderDataElement parentChildReference, boolean childFound) {
		String parentChildNameInData = getNameInDataFromChildReference(parentChildReference);
		if(childNameInData.equals(parentChildNameInData)){
            childFound = true;
        }
		return childFound;
	}

	private String getNameInDataFromChildReference(SpiderDataElement childReference) {
		SpiderDataGroup childReferenceGroup = (SpiderDataGroup) childReference;
		String refId = childReferenceGroup.extractAtomicValue("ref");
//		DataGroup childDataGroup =	recordStorage.read("metadata", refId);
		DataGroup childDataGroup =	findChildOfUnknownMetadataType(refId);
		return childDataGroup.getNameInData();
	}

	private DataGroup findChildOfUnknownMetadataType(String refId){
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			if (isChildOfAbstractRecordType("metadata", recordTypePossibleChild)) {
				DataGroup recordInfo = (DataGroup)recordTypePossibleChild.getFirstChildWithNameInData("recordInfo");
				String id = recordInfo.getFirstAtomicValueWithNameInData("id");
				DataGroup childDataGroup = null;
				try {
					childDataGroup = recordStorage.read(id, refId);
				}catch(RecordNotFoundException exception){

				}
				if(childDataGroup != null){
					return childDataGroup;
				}
			}
		}
		throw new DataException("Data is not valid: referenced child does not exist");
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		ensureIdExists(recordType);
		addUserAndTypeToRecordInfo(userId, recordType);
	}

	private void ensureIdExists(String recordType) {
		if (shouldAutoGenerateId(recordTypeDefinition)) {
			generateAndAddIdToRecordInfo(recordType);
		}
	}

	private boolean shouldAutoGenerateId(DataGroup recordTypeDataGroup) {
		String userSuppliedId = recordTypeDataGroup
				.getFirstAtomicValueWithNameInData("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private void addUserAndTypeToRecordInfo(String userId, String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("createdBy", userId));
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String userId, String recordType,
			DataGroup record) {
		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(
					USER + userId + " is not authorized to create a record  of type:" + recordType);
		}
	}

	@Override
	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		// a record that is being created, can not yet be linked from any other
		// record
		return false;
	}

}
