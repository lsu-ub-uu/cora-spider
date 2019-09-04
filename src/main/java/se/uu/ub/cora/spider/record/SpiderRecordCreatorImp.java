/*
 * Copyright 2015, 2016, 2017 Uppsala University Library
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
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.storage.RecordIdGenerator;

public final class SpiderRecordCreatorImp extends SpiderRecordHandler
		implements SpiderRecordCreator {
	private static final String TS_CREATED = "tsCreated";
	private static final String CREATE = "create";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;
	private User user;
	private RecordTypeHandler recordTypeHandler;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector collectTermCollector;
	private RecordIndexer recordIndexer;

	private SpiderRecordCreatorImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.idGenerator = dependencyProvider.getRecordIdGenerator();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static SpiderRecordCreatorImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordCreatorImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String authToken, String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		this.authToken = authToken;
		this.recordType = recordTypeToCreate;
		this.recordAsSpiderDataGroup = spiderDataGroup;

		return validateCreateAndStoreRecord();

	}

	private SpiderDataRecord validateCreateAndStoreRecord() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordType);
		metadataId = recordTypeHandler.getNewMetadataId();

		checkNoCreateForAbstractRecordType();

		useExtendedFunctionalityBeforeMetadataValidation(recordType, recordAsSpiderDataGroup);

		validateDataInRecordAsSpecifiedInMetadata();

		useExtendedFunctionalityAfterMetadataValidation(recordType, recordAsSpiderDataGroup);

		ensureCompleteRecordInfo(user.id, recordType);
		recordId = extractIdFromData();

		DataGroup topLevelDataGroup = recordAsSpiderDataGroup.toDataGroup();

		DataGroup collectedTerms = collectTermCollector.collectTerms(metadataId, topLevelDataGroup);
		checkUserIsAuthorisedToCreateIncomingData(recordType, collectedTerms);

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);
		createRecordInStorage(topLevelDataGroup, collectedLinks, collectedTerms);

		List<String> ids = recordTypeHandler.createListOfPossibleIdsToThisRecord(recordId);
		recordIndexer.indexData(ids, collectedTerms, topLevelDataGroup);

		SpiderDataGroup spiderDataGroupWithActions = SpiderDataGroup
				.fromDataGroup(topLevelDataGroup);
		useExtendedFunctionalityBeforeReturn(recordType, spiderDataGroupWithActions);

		return dataGroupToRecordEnhancer.enhance(user, recordType, topLevelDataGroup);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, CREATE, recordType);
	}

	private void createRecordInStorage(DataGroup topLevelDataGroup, DataGroup collectedLinks,
			DataGroup collectedTerms) {
		String dataDivider = extractDataDividerFromData(recordAsSpiderDataGroup);
		recordStorage.create(recordType, recordId, topLevelDataGroup, collectedTerms,
				collectedLinks, dataDivider);
	}

	private void checkNoCreateForAbstractRecordType() {
		if (recordTypeHandler.isAbstract()) {
			throw new MisuseException(
					"Data creation on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private String extractIdFromData() {
		return recordAsSpiderDataGroup.extractGroup("recordInfo").extractAtomicValue("id");
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		DataGroup record = recordAsSpiderDataGroup.toDataGroup();

		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForCreateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForCreateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(SpiderDataGroup spiderDataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(authToken, spiderDataGroup);
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForCreateAfterMetadataValidation);
	}

	private void useExtendedFunctionalityBeforeReturn(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, extendedFunctionalityList);
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		ensureIdExists(recordType);
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		addTypeToRecordInfo(recordType, recordInfo);
		addCreatedInfoToRecordInfoUsingUserId(recordInfo, userId);
		addUpdatedInfoToRecordInfoUsingUserId(recordInfo, userId);
	}

	private void ensureIdExists(String recordType) {
		if (recordTypeHandler.shouldAutoGenerateId()) {
			removeIdIfPresentInData();
			generateAndAddIdToRecordInfo(recordType);
		}
	}

	private void removeIdIfPresentInData() {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		if (recordInfo.containsChildWithNameInData("id")) {
			recordInfo.removeChild("id");
		}
	}

	private void addTypeToRecordInfo(String recordType, SpiderDataGroup recordInfo) {
		SpiderDataGroup type = createTypeDataGroup(recordType);
		recordInfo.addChild(type);
	}

	private void addCreatedInfoToRecordInfoUsingUserId(SpiderDataGroup recordInfo, String userId) {
		SpiderDataGroup createdByGroup = createLinkToUserUsingUserIdAndNameInData(userId,
				"createdBy");
		recordInfo.addChild(createdByGroup);
		String currentLocalDateTime = getLocalTimeDateAsString(LocalDateTime.now());
		recordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue(TS_CREATED, currentLocalDateTime));
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private SpiderDataGroup createTypeDataGroup(String recordType) {
		SpiderDataGroup type = SpiderDataGroup.withNameInData("type");
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
		return type;
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String recordType,
			DataGroup collectedData) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, CREATE,
				recordType, collectedData);
	}
}
