/*
 * Copyright 2024 Uppsala University Library
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

import java.util.Map;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.binary.iiif.IiifAdapter;
import se.uu.ub.cora.binary.iiif.IiifAdapterResponse;
import se.uu.ub.cora.binary.iiif.IiifParameters;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.binary.iiif.IiifReader;
import se.uu.ub.cora.spider.binary.iiif.IiifResponse;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class IiifReaderImp implements IiifReader {

	private SpiderDependencyProvider dependencyProvider;

	public static IiifReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new IiifReaderImp(dependencyProvider);
	}

	private IiifReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public IiifResponse readIiif(String identifier, String requestedUri, String method,
			Map<String, String> headersMap) {
		try {
			return tryToReadIiif(identifier, requestedUri, method, headersMap);
		} catch (se.uu.ub.cora.storage.RecordNotFoundException e) {
			throw RecordNotFoundException.withMessage(
					"Record not found for recordType: binary and recordId: " + identifier);
		}
	}

	private IiifResponse tryToReadIiif(String identifier, String requestedUri, String method,
			Map<String, String> headersMap) {
		DataRecordGroup binaryRecordGroup = readBinaryRecord(identifier);
		throwErrorIfNotAuthorizedToCallIiifForRecord(binaryRecordGroup);

		IiifParameters iiifParameters = createIiifParameters(identifier, requestedUri, method,
				headersMap, binaryRecordGroup);

		return callIiifServer(iiifParameters);
	}

	private IiifParameters createIiifParameters(String identifier, String requestedUri,
			String method, Map<String, String> headersMap, DataRecordGroup binaryRecordGroup) {
		String dataDivider = binaryRecordGroup.getDataDivider();
		String uri = String.join("/", identifier, dataDivider, requestedUri);
		return new IiifParameters(uri, method, headersMap);
	}

	private IiifResponse callIiifServer(IiifParameters iiifParameters) {
		IiifAdapter iiifAdapter = BinaryProvider.getIiifAdapter();
		IiifAdapterResponse adapterResponse = iiifAdapter.callIiifServer(iiifParameters);

		return new IiifResponse(adapterResponse.status(), adapterResponse.headers(),
				adapterResponse.body());
	}

	private void throwErrorIfNotAuthorizedToCallIiifForRecord(DataRecordGroup binaryRecordGroup) {
		if (isNotPublic(binaryRecordGroup)) {
			throw exceptionNotAuthorized(binaryRecordGroup.getId());
		}
	}

	private boolean isNotPublic(DataRecordGroup binaryRecordGroup) {
		DataGroup adminInfo = binaryRecordGroup.getFirstGroupWithNameInData("adminInfo");
		String visibility = adminInfo.getFirstAtomicValueWithNameInData("visibility");
		return !"published".equals(visibility);
	}

	private AuthorizationException exceptionNotAuthorized(String identifier) {
		String notAuthorizedMessage = "Not authorized to read binary record with id: " + identifier;
		return new AuthorizationException(notAuthorizedMessage);
	}

	private DataRecordGroup readBinaryRecord(String identifier) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.read("binary", identifier);
	}

}
