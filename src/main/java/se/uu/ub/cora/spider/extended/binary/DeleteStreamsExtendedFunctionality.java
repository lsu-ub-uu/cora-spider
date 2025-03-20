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
package se.uu.ub.cora.spider.extended.binary;

import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

public class DeleteStreamsExtendedFunctionality implements ExtendedFunctionality {

	private SpiderDependencyProvider dependencyProvider;

	public DeleteStreamsExtendedFunctionality(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup binaryRecordGroup = data.dataRecordGroup;
		deleteStreamFromMasterRepresentation(binaryRecordGroup);
		deleteStreamsFromOtherRepresentations(binaryRecordGroup);
	}

	private void deleteStreamsFromOtherRepresentations(DataRecordGroup binaryRecordGroup) {
		List<String> representations = List.of("thumbnail", "medium", "large", "jp2");
		for (String representation : representations) {
			possibleDeleteStreamFromRepresentation(binaryRecordGroup, representation);
		}
	}

	private void possibleDeleteStreamFromRepresentation(DataRecordGroup binaryRecordGroup,
			String representation) {
		if (binaryRecordGroup.containsChildWithNameInData(representation)) {
			deleteStreamfromRepresentation(binaryRecordGroup, representation);
		}
	}

	private void deleteStreamfromRepresentation(DataRecordGroup binaryRecordGroup,
			String representation) {
		String resourceId = getResourceId(binaryRecordGroup, representation);
		String streamId = generateResourceId(binaryRecordGroup, resourceId);
		deleteFromStreamStorage(streamId, binaryRecordGroup.getDataDivider());
	}

	private String getResourceId(DataRecordGroup binaryRecordGroup, String representation) {
		DataGroup representationGroup = binaryRecordGroup
				.getFirstGroupWithNameInData(representation);
		return representationGroup.getFirstAtomicValueWithNameInData("resourceId");
	}

	private String generateResourceId(DataRecordGroup binaryRecordGroup, String resourceId) {
		return binaryRecordGroup.getType() + ":" + resourceId;
	}

	private void deleteFromStreamStorage(String streamId, String dataDivider) {
		StreamStorage streamStorage = dependencyProvider.getStreamStorage();
		streamStorage.delete(streamId, dataDivider);
	}

	private void deleteStreamFromMasterRepresentation(DataRecordGroup binaryRecordGroup) {
		if (binaryRecordGroup.containsChildWithNameInData("master")) {
			ResourceArchive resourceArchive = dependencyProvider.getResourceArchive();
			resourceArchive.delete(binaryRecordGroup.getDataDivider(), binaryRecordGroup.getType(),
					binaryRecordGroup.getId());
		}
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
