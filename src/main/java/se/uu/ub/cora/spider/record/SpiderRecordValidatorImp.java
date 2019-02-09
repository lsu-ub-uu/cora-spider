/*
 * Copyright 2019 Uppsala University Library
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

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public final class SpiderRecordValidatorImp extends SpiderRecordHandler
		implements SpiderRecordValidator {
	private static final String UPDATED_STRING = "updated";
	private static final String TS_UPDATED = "tsUpdated";
	private static final String UPDATED_BY = "updatedBy";
	private static final String UPDATE = "update";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	// private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;
	private User user;
	private String userId;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector collectTermCollector;
	private RecordIndexer recordIndexer;

	private SpiderRecordValidatorImp(SpiderDependencyProvider dependencyProvider,
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

	public static SpiderRecordValidatorImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordValidatorImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public boolean validateRecord(String authToken, String recordType, String actionToPerform,
			SpiderDataGroup spiderDataGroup) {
		this.authToken = authToken;
		this.recordAsSpiderDataGroup = spiderDataGroup;
		this.recordType = recordType;
		// this.recordId = actionToPerform;
		user = tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		String metadataId = getMetadataIdFromActionToPerform(recordType, actionToPerform);
		return validateIncomingDataAsSpecifiedInMetadata(metadataId);
		// useExtendedFunctionalityAfterMetadataValidation(recordType, spiderDataGroup);
		//
		// checkRecordTypeAndIdIsSameAsInEnteredRecord();

		// DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();
		//
		// DataGroup collectedTerms = collectTermCollector.collectTerms(metadataId,
		// topLevelDataGroup);
		// checkUserIsAuthorisedToUpdateGivenCollectedData(collectedTerms);
		//
		// DataGroup collectedLinks = linkCollector.collectLinks(metadataId,
		// topLevelDataGroup,
		// recordType, recordId);
		// checkToPartOfLinkedDataExistsInStorage(collectedLinks);

	}

	private String getMetadataIdFromActionToPerform(String recordType, String actionToPerform) {
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);

		if ("create".equals(actionToPerform)) {
			return recordTypeHandler.getNewMetadataId();
		}
		return recordTypeHandler.getMetadataId();
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

	private boolean validateIncomingDataAsSpecifiedInMetadata(String metadataId) {
		DataGroup dataGroup = recordAsSpiderDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		return validationAnswer.dataIsValid();
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

	private SpiderDataGroup getRecordInfoFromStoredData() {
		DataGroup recordRead = recordStorage.read(recordType, recordId);
		return SpiderDataGroup.fromDataGroup(recordRead.getFirstGroupWithNameInData("recordInfo"));
	}

}
