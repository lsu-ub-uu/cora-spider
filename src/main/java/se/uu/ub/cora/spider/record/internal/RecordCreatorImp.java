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

package se.uu.ub.cora.spider.record.internal;

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordIdGenerator;

public final class RecordCreatorImp extends RecordHandler
		implements RecordCreator {
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
	private DataGroupTermCollector dataGroupTermCollector;
	private RecordIndexer recordIndexer;
	private SpiderDependencyProvider dependencyProvider;
	private Set<String> writePermissions;

	private RecordCreatorImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.idGenerator = dependencyProvider.getRecordIdGenerator();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.dataGroupTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordCreatorImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordCreatorImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord createAndStoreRecord(String authToken, String recordTypeToCreate,
			DataGroup dataGroup) {
		this.authToken = authToken;
		this.recordType = recordTypeToCreate;
		this.recordAsDataGroup = dataGroup;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		metadataId = recordTypeHandler.getNewMetadataId();

		return validateCreateAndStoreRecord();
	}

	private DataRecord validateCreateAndStoreRecord() {
		checkActionAuthorizationForUser();
		checkNoCreateForAbstractRecordType();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, recordAsDataGroup);
		validateRecord();
		useExtendedFunctionalityAfterMetadataValidation(recordType, recordAsDataGroup);
		createAndStoreRecord();
		useExtendedFunctionalityBeforeReturn(recordType, recordAsDataGroup);
		return enhanceRecord();
	}

	private void checkActionAuthorizationForUser() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, CREATE, recordType);
	}

	private void checkNoCreateForAbstractRecordType() {
		if (recordTypeHandler.isAbstract()) {
			throw new MisuseException(
					"Data creation on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForCreateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForCreateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(DataGroup dataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(authToken, dataGroup);
		}
	}

	private void validateRecord() {
		checkRecordPartsUserIsNotAllowtoChange();
		validateDataInRecordAsSpecifiedInMetadata();
	}

	private void checkRecordPartsUserIsNotAllowtoChange() {
		checkUserIsAuthorisedToCreateIncomingData(recordType);
		possiblyRemoveRecordPartsUserIsNotAllowedToChange();
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String recordType) {
		DataGroup collectedTerms = dataGroupTermCollector.collectTermsWithoutTypeAndId(metadataId,
				recordAsDataGroup);
		writePermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, CREATE, recordType, collectedTerms, true);
	}

	private void possiblyRemoveRecordPartsUserIsNotAllowedToChange() {
		if (recordTypeHandler.hasRecordPartCreateConstraint()) {
			removeRecordPartsUserIsNotAllowedToChange();
		}
	}

	private void removeRecordPartsUserIsNotAllowedToChange() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		recordAsDataGroup = dataRedactor.removeChildrenForConstraintsWithoutPermissions(metadataId,
				recordAsDataGroup, recordTypeHandler.getRecordPartCreateWriteConstraints(),
				writePermissions);
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId,
				recordAsDataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForCreateAfterMetadataValidation);
	}

	private void createAndStoreRecord() {
		ensureCompleteRecordInfo(user.id, recordType);
		finalizeAndStoreRecord();
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		ensureIdExists(recordType);
		DataGroup recordInfo = recordAsDataGroup.getFirstGroupWithNameInData(RECORD_INFO);
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
		DataGroup recordInfo = recordAsDataGroup.getFirstGroupWithNameInData(RECORD_INFO);
		if (recordInfo.containsChildWithNameInData("id")) {
			recordInfo.removeFirstChildWithNameInData("id");
		}
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		DataGroup recordInfo = recordAsDataGroup.getFirstGroupWithNameInData(RECORD_INFO);
		recordInfo.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private void addTypeToRecordInfo(String recordType, DataGroup recordInfo) {
		DataGroup type = createTypeDataGroup(recordType);
		recordInfo.addChild(type);
	}

	private DataGroup createTypeDataGroup(String recordType) {
		DataGroup type = DataGroupProvider.getDataGroupUsingNameInData("type");
		type.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("linkedRecordType",
				"recordType"));
		type.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("linkedRecordId",
				recordType));
		return type;
	}

	private void addCreatedInfoToRecordInfoUsingUserId(DataGroup recordInfo, String userId) {
		DataGroup createdByGroup = createLinkToUserUsingUserIdAndNameInData(userId, "createdBy");
		recordInfo.addChild(createdByGroup);
		String currentTimestamp = getCurrentTimestampAsString();
		recordInfo.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(TS_CREATED,
				currentTimestamp));
	}

	private void finalizeAndStoreRecord() {
		DataGroup collectedTerms;
		recordId = extractIdFromData();

		collectedTerms = dataGroupTermCollector.collectTerms(metadataId, recordAsDataGroup);

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, recordAsDataGroup,
				recordType, recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);
		createRecordInStorage(recordAsDataGroup, collectedLinks, collectedTerms);

		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId(recordId);
		recordIndexer.indexData(ids, collectedTerms, recordAsDataGroup);
	}

	private void createRecordInStorage(DataGroup topLevelDataGroup, DataGroup collectedLinks,
			DataGroup collectedTerms) {
		String dataDivider = extractDataDividerFromData(recordAsDataGroup);
		recordStorage.create(recordType, recordId, topLevelDataGroup, collectedTerms,
				collectedLinks, dataDivider);
	}

	private String extractIdFromData() {
		return recordAsDataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
	}

	private void useExtendedFunctionalityBeforeReturn(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, extendedFunctionalityList);
	}

	private DataRecord enhanceRecord() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		return dataGroupToRecordEnhancer.enhanceIgnoringReadAccess(user, recordType,
				recordAsDataGroup, dataRedactor);
	}

	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		// needed for test
		return dataGroupToRecordEnhancer;
	}
}
