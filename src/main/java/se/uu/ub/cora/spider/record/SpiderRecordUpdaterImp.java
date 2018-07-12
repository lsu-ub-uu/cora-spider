/*
 * Copyright 2015, 2016, 2018 Uppsala University Library
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public final class SpiderRecordUpdaterImp extends SpiderRecordHandler
		implements SpiderRecordUpdater {
	private static final String UPDATED_STRING = "updated";
	private static final String TS_UPDATED = "tsUpdated";
	private static final String UPDATED_BY = "updatedBy";
	private static final String UPDATE = "update";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final int MAX_NUMBER_OF_UPDATED = 10;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;
	private User user;
	private String userId;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector collectTermCollector;
	private RecordIndexer recordIndexer;

	private SpiderRecordUpdaterImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static SpiderRecordUpdaterImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordUpdaterImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataRecord updateRecord(String authToken, String recordType, String recordId,
			SpiderDataGroup spiderDataGroup) {
		this.authToken = authToken;
		this.recordAsSpiderDataGroup = spiderDataGroup;
		this.recordType = recordType;
		this.recordId = recordId;
		user = tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		metadataId = recordTypeHandler.getMetadataId();

		checkUserIsAuthorisedToUpdatePreviouslyStoredRecord();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, spiderDataGroup);

		addUpdateInfo(spiderDataGroup);
		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityAfterMetadataValidation(recordType, spiderDataGroup);

		checkRecordTypeAndIdIsSameAsInEnteredRecord();

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		DataGroup collectedTerms = collectTermCollector.collectTerms(metadataId, topLevelDataGroup);
		checkUserIsAuthorisedToUpdateGivenCollectedData(collectedTerms);

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(spiderDataGroup);

		recordStorage.update(recordType, recordId, topLevelDataGroup, collectedTerms,
				collectedLinks, dataDivider);

		List<String> ids = recordTypeHandler.createListOfPossibleIdsToThisRecord(recordId);
		recordIndexer.indexData(ids, collectedTerms, topLevelDataGroup);

		return dataGroupToRecordEnhancer.enhance(user, recordType, topLevelDataGroup);
	}

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, UPDATE, recordType);
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForUpdateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(SpiderDataGroup spiderDataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(userId, spiderDataGroup);
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForUpdateAfterMetadataValidation);
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		DataGroup dataGroup = recordAsSpiderDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void checkRecordTypeAndIdIsSameAsInEnteredRecord() {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		checkIdIsSameAsInEnteredRecord(recordInfo);
		checkTypeIsSameAsInEnteredRecord(recordInfo);
	}

	private void checkIdIsSameAsInEnteredRecord(SpiderDataGroup recordInfo) {
		String valueFromRecord = recordInfo.extractAtomicValue("id");
		checkValueIsSameAsValueInEnteredRecord(recordId, valueFromRecord);
	}

	private void checkValueIsSameAsValueInEnteredRecord(String value,
			String extractedValueFromRecord) {
		if (!value.equals(extractedValueFromRecord)) {
			throw new DataException("Value in data(" + extractedValueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void checkTypeIsSameAsInEnteredRecord(SpiderDataGroup recordInfo) {
		String type = extractTypeFromRecordInfo(recordInfo);
		checkValueIsSameAsValueInEnteredRecord(recordType, type);
	}

	private String extractTypeFromRecordInfo(SpiderDataGroup recordInfo) {
		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		return typeGroup.extractAtomicValue(LINKED_RECORD_ID);
	}

	private void checkUserIsAuthorisedToUpdatePreviouslyStoredRecord() {
		DataGroup recordRead = recordStorage.read(recordType, recordId);
		DataGroup collectedTerms = collectTermCollector.collectTerms(metadataId, recordRead);

		checkUserIsAuthorisedToUpdateGivenCollectedData(collectedTerms);
	}

	private void checkUserIsAuthorisedToUpdateGivenCollectedData(DataGroup collectedTerms) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, UPDATE,
				recordType, collectedTerms);
	}

	private void addUpdateInfo(SpiderDataGroup topLevelDataGroup) {
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup("recordInfo");
		replaceUpdatedInfoWithInfoFromPreviousRecord(recordInfo);
		SpiderDataGroup updated = createUpdateInfoForThisUpdate(recordInfo);
		recordInfo.addChild(updated);
	}

	private void replaceUpdatedInfoWithInfoFromPreviousRecord(SpiderDataGroup recordInfo) {
		removeUpdateInfoFromIncomingData(recordInfo);
		addRecordInfoFromReadData(recordInfo);
	}

	private void removeUpdateInfoFromIncomingData(SpiderDataGroup recordInfo) {
		while (recordInfo.containsChildWithNameInData(UPDATED_STRING)) {
			recordInfo.removeChild(UPDATED_STRING);
		}
	}

	private void addRecordInfoFromReadData(SpiderDataGroup recordInfo) {
		SpiderDataGroup recordInfoStoredRecord = getRecordInfoFromStoredData();
		List<SpiderDataGroup> updatedGroups = recordInfoStoredRecord
				.getAllGroupsWithNameInData(UPDATED_STRING);
		limitNumberOfUpdatedByPossiblyRemovingFirst(updatedGroups);
		updatedGroups.forEach(recordInfo::addChild);
	}

	private void limitNumberOfUpdatedByPossiblyRemovingFirst(List<SpiderDataGroup> updatedGroups) {
		if(updatedGroups.size() == MAX_NUMBER_OF_UPDATED){
			updatedGroups.remove(0);
		}
	}

	private SpiderDataGroup getRecordInfoFromStoredData() {
		DataGroup recordRead = recordStorage.read(recordType, recordId);
		return SpiderDataGroup
				.fromDataGroup(recordRead.getFirstGroupWithNameInData("recordInfo"));
	}

	private SpiderDataGroup createUpdateInfoForThisUpdate(SpiderDataGroup recordInfo) {
		SpiderDataGroup updated = SpiderDataGroup.withNameInData(UPDATED_STRING);
		String repeatId = getRepeatId(recordInfo);
		updated.setRepeatId(repeatId);

		setUpdatedBy(updated);
		setTsUpdated(updated);
		return updated;
	}

	private void setUpdatedBy(SpiderDataGroup updated) {
		SpiderDataGroup updatedBy = createUpdatedByLink();
		updated.addChild(updatedBy);
	}

	private void setTsUpdated(SpiderDataGroup updated) {
		String currentLocalDateTime = getLocalTimeDateAsString(LocalDateTime.now());
		updated.addChild(SpiderDataAtomic.withNameInDataAndValue(TS_UPDATED, currentLocalDateTime));
	}

	private String getRepeatId(SpiderDataGroup recordInfo) {
		List<SpiderDataGroup> updatedList = recordInfo.getAllGroupsWithNameInData(UPDATED_STRING);
		if(updatedList.isEmpty()){
			return "0";
		}
		return calculateRepeatId(updatedList);
	}

	private String calculateRepeatId(List<SpiderDataGroup> updatedList) {
		List<Integer> repeatIds = getAllCurrentRepeatIds(updatedList);
		Integer max = Collections.max(repeatIds);
		return String.valueOf(max+1);
	}

	private List<Integer> getAllCurrentRepeatIds(List<SpiderDataGroup> updatedList) {
		List<Integer> repeatIds = new ArrayList<>();
		for(SpiderDataGroup updated : updatedList){
			repeatIds.add(Integer.valueOf(updated.getRepeatId()));
		}
		return repeatIds;
	}


	private SpiderDataGroup createUpdatedByLink() {
		SpiderDataGroup updatedBy = SpiderDataGroup.withNameInData(UPDATED_BY);
		updatedBy.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "user"));
		updatedBy.addChild(SpiderDataAtomic.withNameInDataAndValue(LINKED_RECORD_ID, user.id));
		return updatedBy;
	}

}
