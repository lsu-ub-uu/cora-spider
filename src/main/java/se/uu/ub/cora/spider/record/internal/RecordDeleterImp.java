/*
 * Copyright 2015, 2022, 2024 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_BEFORE;

import java.util.List;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordNotFoundException;

public final class RecordDeleterImp extends RecordHandler implements RecordDeleter {
	private static final String DELETE = "delete";
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private RecordIndexer recordIndexer;
	private DataGroupTermCollector termCollector;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	private RecordDeleterImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		authorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		recordIndexer = dependencyProvider.getRecordIndexer();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordDeleterImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new RecordDeleterImp(dependencyProvider);
	}

	@Override
	public void deleteRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		try {
			tryToDelete();
		} catch (se.uu.ub.cora.storage.RecordNotFoundException e) {
			String errorMessageNotFound = "Record with type: " + recordType + " and id: " + recordId
					+ " could not be deleted.";
			throw RecordNotFoundException.withMessageAndException(errorMessageNotFound, e);
		}
	}

	private void tryToDelete() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		useExtendedFunctionalityUsingPosition(DELETE_AFTER_AUTHORIZATION);

		DataRecordGroup dataRecordGroup = readRecordFromStorage();
		checkUserIsAuthorizedToDeleteStoredRecord(dataRecordGroup);
		checkNoIncomingLinksExists();

		useExtendedFunctionalityUsingPositionAndDataGroup(DELETE_BEFORE, dataRecordGroup);

		deleteRecordFromStorageAndIndex();

		useExtendedFunctionalityUsingPositionAndDataGroup(DELETE_AFTER, dataRecordGroup);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, DELETE, recordType);
	}

	private void useExtendedFunctionalityUsingPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> extendedFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(extendedFunctionality);
	}

	protected void useExtendedFunctionality(List<ExtendedFunctionality> functionalityList) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData();
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData() {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordType;
		data.recordId = recordId;
		data.authToken = authToken;
		data.user = user;
		return data;
	}

	private DataRecordGroup readRecordFromStorage() {
		return recordStorage.read(recordType, recordId);
	}

	private void checkUserIsAuthorizedToDeleteStoredRecord(DataRecordGroup dataRecordGroup) {
		CollectTerms collectTerms = getCollectedTermsForPreviouslyReadRecord(dataRecordGroup);
		authorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, DELETE,
				recordType, collectTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForPreviouslyReadRecord(DataRecordGroup dataRecordGroup) {
		String definitionId = getDefinitionIdUsingDataRecord(dataRecordGroup);
		return termCollector.collectTerms(definitionId, dataRecordGroup);
	}

	private String getDefinitionIdUsingDataRecord(DataRecordGroup dataRecordGroup) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataRecordGroup);
		return recordTypeHandler.getDefinitionId();
	}

	private void checkNoIncomingLinksExists() {
		if (recordStorage.linksExistForRecord(recordType, recordId)) {
			throw new MisuseException("Record with type: " + recordType + " and id: " + recordId
					+ " could not be deleted since other records are linking to it");
		}
	}

	private void useExtendedFunctionalityUsingPositionAndDataGroup(
			ExtendedFunctionalityPosition position, DataRecordGroup dataRecordGroup) {
		List<ExtendedFunctionality> extendedFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(DataProvider.createGroupFromRecordGroup(dataRecordGroup),
				extendedFunctionality);
	}

	private void deleteRecordFromStorageAndIndex() {
		recordStorage.deleteByTypeAndId(recordType, recordId);
		recordIndexer.deleteFromIndex(recordType, recordId);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
