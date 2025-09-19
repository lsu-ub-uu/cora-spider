/*
 * Copyright 2024, 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.binary.iiif.internal;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.binary.iiif.IiifAdapter;
import se.uu.ub.cora.binary.iiif.IiifAdapterResponse;
import se.uu.ub.cora.binary.iiif.IiifParameters;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.binary.iiif.IiifReader;
import se.uu.ub.cora.spider.binary.iiif.IiifResponse;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class IiifReaderImp implements IiifReader {

	private static final String AUTH_TOKEN = "authtoken";
	private static final String ACTION_READ = "read";
	private static final String JP2_PERMISSION = "binary.jp2";

	private SpiderDependencyProvider dependencyProvider;
	private Authenticator authenticator;
	private DataGroupTermCollector termCollector;
	private SpiderAuthorizator spiderAuthorizator;

	public static IiifReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new IiifReaderImp(dependencyProvider);
	}

	private IiifReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
	}

	@Override
	public IiifResponse readIiif(String recordId, String requestedUri, String method,
			Map<String, String> headersMap) {
		try {
			return tryToReadIiif(recordId, requestedUri, method, headersMap);
		} catch (se.uu.ub.cora.storage.RecordNotFoundException _) {
			throw RecordNotFoundException.withMessage(
					"Record not found for recordType: binary and recordId: " + recordId);
		}
	}

	private IiifResponse tryToReadIiif(String recordId, String requestedUri, String method,
			Map<String, String> headersMap) {
		DataRecordGroup binaryRecordGroup = readBinaryRecord(recordId);

		authenticateAndAuthorizeUser(headersMap, binaryRecordGroup);

		throwNotFoundExceptionIfJP2DoesNotExist(binaryRecordGroup);

		IiifParameters iiifParameters = createIiifParameters(requestedUri, method, headersMap,
				binaryRecordGroup);
		return callIiifServer(iiifParameters);
	}

	private DataRecordGroup readBinaryRecord(String recordId) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.read("binary", recordId);
	}

	private void authenticateAndAuthorizeUser(Map<String, String> headersMap,
			DataRecordGroup binaryRecordGroup) {
		User user = getUserFromToken(headersMap);
		List<PermissionTerm> permissionTerms = getPermissionTerms(binaryRecordGroup);

		checkIfUserIsAuthorized(user, permissionTerms);
	}

	private User getUserFromToken(Map<String, String> headersMap) {
		return authenticator.getUserForToken(possiblyGetAuthToken(headersMap));
	}

	private String possiblyGetAuthToken(Map<String, String> headersMap) {
		for (Entry<String, String> entry : headersMap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(AUTH_TOKEN)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private List<PermissionTerm> getPermissionTerms(DataRecordGroup binaryRecordGroup) {
		String definitionId = getDefinitionId(binaryRecordGroup);
		CollectTerms collectTerms = termCollector.collectTerms(definitionId, binaryRecordGroup);
		return collectTerms.permissionTerms;
	}

	private String getDefinitionId(DataRecordGroup binaryRecordGroup) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(binaryRecordGroup);
		return recordTypeHandler.getDefinitionId();
	}

	private void checkIfUserIsAuthorized(User user, List<PermissionTerm> permissionTerms) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				ACTION_READ, JP2_PERMISSION, permissionTerms);
	}

	private void throwNotFoundExceptionIfJP2DoesNotExist(DataRecordGroup binaryRecordGroup) {
		if (!binaryRecordGroup.containsChildWithNameInData("jp2")) {
			throw RecordNotFoundException
					.withMessage("Could not find a JP2 representation for binary and recordId: "
							+ binaryRecordGroup.getId());
		}
	}

	private IiifParameters createIiifParameters(String requestedUri, String method,
			Map<String, String> headersMap, DataRecordGroup binaryRecordGroup) {
		String dataDivider = binaryRecordGroup.getDataDivider();
		String type = binaryRecordGroup.getType();
		String id = binaryRecordGroup.getId();
		return new IiifParameters(dataDivider, type, id, "jp2", requestedUri, method, headersMap);

	}

	private IiifResponse callIiifServer(IiifParameters iiifParameters) {
		IiifAdapter iiifAdapter = BinaryProvider.getIiifAdapter();
		IiifAdapterResponse adapterResponse = iiifAdapter.callIiifServer(iiifParameters);
		return new IiifResponse(adapterResponse.status(), adapterResponse.headers(),
				adapterResponse.body());
	}

	public Object onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

}
