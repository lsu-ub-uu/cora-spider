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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.StreamStorageSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;

public class DeleteStreamsExtendedFunctionalityTest {

	private static final String DATA_DIVIDER = "someDataDivider";
	private static final String RECORD_TYPE = "someRecordType";
	private static final String RECORD_ID = "someId";
	private static final String MIME_TYPE = "someMimeType";
	private static final String MASTER = "master";
	private static final String THUMBNAIL = "thumbnail";
	private static final String MEDIUM = "medium";
	private static final String LARGE = "large";
	private static final String JP2 = "jp2";
	private static final String FILE_SIZE = "2123";
	private static final String ORIGINAL_FILE_NAME = "someOriginalFilename";

	private ExtendedFunctionalityData data;
	private DeleteStreamsExtendedFunctionality extFunc;
	private SpiderDependencyProviderSpy dependencyProvider;
	private StreamStorageSpy streamStorage;
	private DataGroupSpy masterResourceTypeDGS;
	private DataGroupSpy thumbnailResourceTypeDGS;
	private DataGroupSpy mediumResourceTypeDGS;
	private DataGroupSpy largeResourceTypeDGS;
	private DataGroupSpy jp2ResourceTypeDGS;
	private DataRecordGroupSpy readBinaryDGS;
	private ResourceArchiveSpy resourceArchive;

	@BeforeMethod
	private void beforeMethod() {
		setupDependencyProvider();
		setupBinaryRecord();

		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = readBinaryDGS;

		extFunc = new DeleteStreamsExtendedFunctionality(dependencyProvider);
	}

	private void setupDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		streamStorage = new StreamStorageSpy();
		resourceArchive = new ResourceArchiveSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getStreamStorage",
				() -> streamStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getResourceArchive",
				() -> resourceArchive);
	}

	private void setupBinaryRecord() {
		masterResourceTypeDGS = createResourceDataGroupForResourceId(MASTER);
		thumbnailResourceTypeDGS = createResourceDataGroupForResourceId(THUMBNAIL);
		mediumResourceTypeDGS = createResourceDataGroupForResourceId(MEDIUM);
		largeResourceTypeDGS = createResourceDataGroupForResourceId(LARGE);
		jp2ResourceTypeDGS = createResourceDataGroupForResourceId(JP2);

		readBinaryDGS = new DataRecordGroupSpy();
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> masterResourceTypeDGS, MASTER);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> thumbnailResourceTypeDGS, THUMBNAIL);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> mediumResourceTypeDGS, MEDIUM);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> largeResourceTypeDGS, LARGE);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> jp2ResourceTypeDGS, JP2);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData", () -> true);

		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> DATA_DIVIDER);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getType", () -> RECORD_TYPE);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);
	}

	private DataGroupSpy createResourceDataGroupForResourceId(String representation) {
		DataGroupSpy resourceTypeDG = new DataGroupSpy();
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> FILE_SIZE, "fileSize");
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> MIME_TYPE, "mimeType");
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someId-" + representation, "resourceId");
		return resourceTypeDG;
	}

	@Test
	public void testBinaryWithoutMaster() {
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> false);

		extFunc.useExtendedFunctionality(data);

		readBinaryDGS.MCR.assertCalledParameters("containsChildWithNameInData", "master");
		resourceArchive.MCR.assertMethodNotCalled("delete");
	}

	@Test
	public void testBinaryWithoutAnyRepresentations() {
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> false);

		extFunc.useExtendedFunctionality(data);

		readBinaryDGS.MCR.assertCalledParameters("containsChildWithNameInData", "thumbnail");
		readBinaryDGS.MCR.assertCalledParameters("containsChildWithNameInData", "medium");
		readBinaryDGS.MCR.assertCalledParameters("containsChildWithNameInData", "large");
		readBinaryDGS.MCR.assertCalledParameters("containsChildWithNameInData", "jp2");
		streamStorage.MCR.assertMethodNotCalled("delete");
	}

	@Test
	public void testMasterRepresentationExists() {
		setUpBinaryDataGroupToOnlyHaveRepresentation("master");

		extFunc.useExtendedFunctionality(data);

		resourceArchive.MCR.assertParameters("delete", 0, DATA_DIVIDER, RECORD_TYPE, RECORD_ID);
	}

	private void setUpBinaryDataGroupToOnlyHaveRepresentation(String representation) {
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> false);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				representation);
	}

	@Test
	public void testThumbnailRepresentationExists() {
		setUpBinaryDataGroupToOnlyHaveRepresentation(THUMBNAIL);

		extFunc.useExtendedFunctionality(data);

		assertStreamStorageCalledForDeleteOf(THUMBNAIL);
	}

	private void assertStreamStorageCalledForDeleteOf(String representation) {
		streamStorage.MCR.assertParameters("delete", 0, DATA_DIVIDER, RECORD_TYPE, RECORD_ID,
				representation);
		streamStorage.MCR.assertNumberOfCallsToMethod("delete", 1);
	}

	@Test
	public void testMediumRepresentationExists() {
		setUpBinaryDataGroupToOnlyHaveRepresentation(MEDIUM);

		extFunc.useExtendedFunctionality(data);

		assertStreamStorageCalledForDeleteOf(MEDIUM);
	}

	@Test
	public void testLargeRepresentationExists() {
		setUpBinaryDataGroupToOnlyHaveRepresentation(LARGE);

		extFunc.useExtendedFunctionality(data);

		assertStreamStorageCalledForDeleteOf(LARGE);
	}

	@Test
	public void testJp2RepresentationExists() {
		setUpBinaryDataGroupToOnlyHaveRepresentation(JP2);

		extFunc.useExtendedFunctionality(data);

		assertStreamStorageCalledForDeleteOf(JP2);
	}

	@Test
	public void testAllRepresentationExists() {
		extFunc.useExtendedFunctionality(data);

		resourceArchive.MCR.assertNumberOfCallsToMethod("delete", 1);
		streamStorage.MCR.assertNumberOfCallsToMethod("delete", 4);
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		assertEquals(extFunc.onlyForTestGetDependencyProvider(), dependencyProvider);

	}
}
