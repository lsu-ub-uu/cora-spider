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

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

/**
 * DeleteStreamsExtendedFunctionality remove stored datastreams from the archive and streamstorage,
 * for the handled dataRecordGroup.
 * <p>
 * It is expected that this functionallity only is called for binary records.
 */
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

	private void deleteStreamFromMasterRepresentation(DataRecordGroup binaryRecordGroup) {
		if (binaryRecordGroup.containsChildWithNameInData("master")) {
			ResourceArchive resourceArchive = dependencyProvider.getResourceArchive();
			resourceArchive.delete(binaryRecordGroup.getDataDivider(), binaryRecordGroup.getType(),
					binaryRecordGroup.getId());
		}
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
		StreamStorage streamStorage = dependencyProvider.getStreamStorage();
		String dataDivider = binaryRecordGroup.getDataDivider();
		String type = binaryRecordGroup.getType();
		String id = binaryRecordGroup.getId();
		streamStorage.delete(dataDivider, type, id, representation);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
