/*
 * Copyright 2016, 2023, 2024 Uppsala University Library
 * Copyright 2016 Olov McKie
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

package se.uu.ub.cora.spider.binary.internal;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.ResourceInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.spider.record.ResourceNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

public final class DownloaderImp implements Downloader {
	private static final String ACTION_READ = "read";
	private static final String ERR_MESSAGE_MISUSE = "Downloading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private List<String> allowedRepresentations = List.of("master", "thumbnail", "medium", "large",
			"jp2");
	private String representation;
	private SpiderAuthorizator spiderAuthorizator;
	private StreamStorage streamStorage;
	private ResourceArchive resourceArchive;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private String type;
	private String id;
	private DataGroupTermCollector termCollector;
	private SpiderDependencyProvider dependencyProvider;
	private String authToken;

	private DownloaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		resourceArchive = dependencyProvider.getResourceArchive();
		streamStorage = dependencyProvider.getStreamStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static Downloader usingDependencyProvider(
			SpiderDependencyProvider spiderDependencyProvider) {
		return new DownloaderImp(spiderDependencyProvider);
	}

	@Override
	public ResourceInputStream download(String authToken, String type, String id,
			String representation) {
		this.authToken = authToken;
		this.type = type;
		this.id = id;
		this.representation = representation;

		validateInput();
		return tryToReadRepresentation();
	}

	private void validateInput() {
		if (typeNotBinary(type)) {
			throw new MisuseException(MessageFormat.format(ERR_MESSAGE_MISUSE, type, id));
		}
		if (notValidRepresentation(representation)) {
			throw new MisuseException("Representation " + representation + " does not exist.");
		}
	}

	private boolean typeNotBinary(String type) {
		return !"binary".equals(type);
	}

	private boolean notValidRepresentation(String representation) {
		return !allowedRepresentations.contains(representation);
	}

	private ResourceInputStream tryToReadRepresentation() {
		try {
			DataRecordGroup binaryRecordGroup = recordStorage.read(type, id);
			authenticateAndAuthorizeUser(binaryRecordGroup);
			return readRepresentation(binaryRecordGroup);
		} catch (se.uu.ub.cora.storage.RecordNotFoundException e) {
			throw throwRecordNotFoundException(e);
		} catch (se.uu.ub.cora.storage.ResourceNotFoundException e) {
			throw throwResourceNotFoundException(e);
		}
	}

	private void authenticateAndAuthorizeUser(DataRecordGroup binaryRecordGroup) {
		User user = authenticator.getUserForToken(authToken);
		List<PermissionTerm> permissionTerms = getPermissionTerms(binaryRecordGroup);

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				ACTION_READ, type + "." + representation, permissionTerms);
	}

	private List<PermissionTerm> getPermissionTerms(DataRecordGroup binaryRecordGroup) {
		DataGroup dataGroup = DataProvider.createGroupFromRecordGroup(binaryRecordGroup);
		String definitionId = getDefinitionId(binaryRecordGroup);

		CollectTerms collectTerms = termCollector.collectTerms(definitionId, dataGroup);
		return collectTerms.permissionTerms;
	}

	private String getDefinitionId(DataRecordGroup binaryRecordGroup) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(binaryRecordGroup);
		return recordTypeHandler.getDefinitionId();
	}

	private RecordNotFoundException throwRecordNotFoundException(
			se.uu.ub.cora.storage.RecordNotFoundException e) {
		String errorMessage = MessageFormat
				.format("Could not find record with type: {0} and id:" + " {1}", type, id);
		return RecordNotFoundException.withMessageAndException(errorMessage, e);
	}

	private ResourceNotFoundException throwResourceNotFoundException(
			se.uu.ub.cora.storage.ResourceNotFoundException e) {
		String errorMessage = MessageFormat.format(
				"Could not download the stream because it could not be "
						+ "found in storage. Type: {0}, id: {1} and representation: {2}",
				type, id, representation);
		return ResourceNotFoundException.withMessageAndException(errorMessage, e);
	}

	private ResourceInputStream readRepresentation(DataRecordGroup binaryRecordGroup) {
		String dataDivider = binaryRecordGroup.getDataDivider();
		if (isMasterRepresentation(representation)) {
			return readMasterRepresentation(binaryRecordGroup, dataDivider);
		}
		return readConvertedRepresentation(binaryRecordGroup, dataDivider);
	}

	private boolean isMasterRepresentation(String representation) {
		return "master".equals(representation);
	}

	private ResourceInputStream readConvertedRepresentation(DataRecordGroup binaryRecordGroup,
			String dataDivider) {
		InputStream stream = streamStorage.retrieve(type + ":" + id + "-" + representation,
				dataDivider);
		return prepareResponseForResourceInputStream(representation, binaryRecordGroup, stream);
	}

	private ResourceInputStream readMasterRepresentation(DataRecordGroup binaryRecordGroup,
			String dataDivider) {
		InputStream stream = resourceArchive.readMasterResource(dataDivider, type, id);
		return prepareResponseForResourceInputStream(representation, binaryRecordGroup, stream);
	}

	private ResourceInputStream prepareResponseForResourceInputStream(String representation,
			DataRecordGroup binaryRecordGroup, InputStream stream) {
		DataGroup resourceGroup = binaryRecordGroup.getFirstGroupWithNameInData(representation);
		String resourceId = resourceGroup.getFirstAtomicValueWithNameInData("resourceId");
		String fileSize = resourceGroup.getFirstAtomicValueWithNameInData("fileSize");
		String mimeType = resourceGroup.getFirstAtomicValueWithNameInData("mimeType");

		return ResourceInputStream.withNameSizeInputStream(resourceId, Long.valueOf(fileSize),
				mimeType, stream);
	}
}
