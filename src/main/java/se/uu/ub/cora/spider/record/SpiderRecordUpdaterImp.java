/*
 * Copyright 2015, 2016 Uppsala University Library
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
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;

public final class SpiderRecordUpdaterImp extends SpiderRecordHandler
		implements SpiderRecordUpdater {
	private static final String UPDATE = "update";
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

	private SpiderRecordUpdaterImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
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

		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("metadataId");

		checkUserIsAuthorisedToUpdatePreviouslyStoredRecord();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, spiderDataGroup);

		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityAfterMetadataValidation(recordType, spiderDataGroup);

		checkRecordTypeAndIdIsSameAsInEnteredRecord();

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToStoreIncomingData(topLevelDataGroup);

		// validate (including protected data)
		// TODO: add validate here

		// merge possibly hidden data
		// TODO: merge incoming data with stored if user does not have right to
		// update some parts

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(spiderDataGroup);
		recordStorage.update(recordType, recordId, spiderDataGroup.toDataGroup(), collectedLinks,
				dataDivider);

		return dataGroupToRecordEnhancer.enhance(user, recordType, topLevelDataGroup);
	}

	private User tryToGetActiveUser() {
		return authenticator.tryToGetActiveUser(authToken);
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
		checkValueIsSameAsInEnteredRecord(recordId, "id");
		checkValueIsSameAsInEnteredRecord(recordType, "type");
	}

	private void checkValueIsSameAsInEnteredRecord(String value, String valueToExtract) {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		String valueFromRecord = recordInfo.extractAtomicValue(valueToExtract);
		if (!value.equals(valueFromRecord)) {
			throw new DataException("Value in data(" + valueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void checkUserIsAuthorisedToUpdatePreviouslyStoredRecord() {
		DataGroup recordRead = recordStorage.read(recordType, recordId);
		checkUserIsAuthorisedToStoreIncomingData(recordRead);
	}

	private void checkUserIsAuthorisedToStoreIncomingData(DataGroup incomingData) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndRecord(user, UPDATE,
				recordType, incomingData);
	}
}
