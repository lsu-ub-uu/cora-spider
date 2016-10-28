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
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionKeyCalculator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;

public final class SpiderRecordCreatorImp extends SpiderRecordHandler
		implements SpiderRecordCreator {
	private static final String USER = "User:";
	private Authenticator authenticator;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;
	private User user;
	private RecordTypeHandler recordTypeHandler;

	private SpiderRecordCreatorImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.authorization = dependencyProvider.getAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.idGenerator = dependencyProvider.getIdGenerator();
		this.keyCalculator = dependencyProvider.getPermissionKeyCalculator();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static SpiderRecordCreatorImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordCreatorImp(dependencyProvider);
	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String authToken, String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		this.authToken = authToken;
		this.recordType = recordTypeToCreate;

		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordTypeToCreate);
		this.recordAsSpiderDataGroup = spiderDataGroup;
		metadataId = recordTypeHandler.getNewMetadataId();

		return validateCreateAndStoreRecord();

	}

	private SpiderDataRecord validateCreateAndStoreRecord() {
		tryToGetActiveUser();
		// TODO: we should do a first security check here as soon as we know the
		// user, based on action(create) and type
		checkNoCreateForAbstractRecordType();

		useExtendedFunctionalityBeforeMetadataValidation(recordType, recordAsSpiderDataGroup);

		validateDataInRecordAsSpecifiedInMetadata();

		useExtendedFunctionalityAfterMetadataValidation(recordType, recordAsSpiderDataGroup);

		ensureCompleteRecordInfo(user.id, recordType);

		DataGroup topLevelDataGroup = recordAsSpiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreateIncomingData(recordType, topLevelDataGroup);

		createRecordInStorage(topLevelDataGroup);

		SpiderDataGroup spiderDataGroupWithActions = SpiderDataGroup
				.fromDataGroup(topLevelDataGroup);

		useExtendedFunctionalityBeforeReturn(recordType, spiderDataGroupWithActions);

		return createDataRecordContainingDataGroup(spiderDataGroupWithActions);
	}

	private void tryToGetActiveUser() {
		user = authenticator.tryToGetActiveUser(authToken);
	}

	private void createRecordInStorage(DataGroup topLevelDataGroup) {
		String id = extractIdFromData();

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, id);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(recordAsSpiderDataGroup);

		recordStorage.create(recordType, id, topLevelDataGroup, collectedLinks, dataDivider);
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
		addUserAndTypeToRecordInfo(userId, recordType);
		// set more stuff, user, tscreated, status (created, updated, deleted,
		// etc), published
		// (true, false)
		// set owning organisation

	}

	private void ensureIdExists(String recordType) {
		if (recordTypeHandler.shouldAutoGenerateId()) {
			generateAndAddIdToRecordInfo(recordType);
		}
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private void addUserAndTypeToRecordInfo(String userId, String recordType) {
		SpiderDataGroup recordInfo = recordAsSpiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("createdBy", userId));
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String recordType, DataGroup record) {
		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(user, recordCalculateKeys)) {
			throw new AuthorizationException(USER + user.id
					+ " is not authorized to create a record  of type:" + recordType);
		}
	}

	@Override
	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		// a record that is being created, can not yet be linked from any other
		// record
		return false;
	}

}
