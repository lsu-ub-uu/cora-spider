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
package se.uu.ub.cora.spider.binary.iiif;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.spider.binary.iiif.internal.IiifImageReaderImp;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class IiifImageReaderTest {

	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private static final String SOME_MIME_TYPE = "someMimeType";
	private static final String MASTER = "master";
	private static final String THUMBNAIL = "thumbnail";
	private static final String MEDIUM = "medium";
	private static final String LARGE = "large";
	private static final String JP2 = "jp2";
	private static final String SOME_FILE_SIZE = "2123";
	private static final String ORIGINAL_FILE_NAME = "someOriginalFilename";

	private LoggerFactorySpy loggerFactorySpy;
	private IiifImageInstanceProviderSpy iiifImageAdapterInstanceProvider;

	private IiifReader reader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private DataRecordGroupSpy readBinaryDGS;
	private DataGroupSpy resourceTypeDGS;

	@BeforeMethod
	private void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		iiifImageAdapterInstanceProvider = new IiifImageInstanceProviderSpy();
		BinaryProvider
				.onlyForTestSetIiifImageAdapterInstanceProvider(iiifImageAdapterInstanceProvider);

		dependencyProvider = new SpiderDependencyProviderSpy();
		setUpDependencyProvider();
		setupBinaryRecord();

		reader = IiifImageReaderImp.usingDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		authenticator = new AuthenticatorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);

		authorizator = new SpiderAuthorizatorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);

		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
	}

	private void setupBinaryRecord() {
		resourceTypeDGS = new DataGroupSpy();
		resourceTypeDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_FILE_SIZE, "fileSize");
		resourceTypeDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_MIME_TYPE, "mimeType");

		readBinaryDGS = new DataRecordGroupSpy();
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, MASTER);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, THUMBNAIL);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, MEDIUM);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, LARGE);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, JP2);

		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> SOME_DATA_DIVIDER);

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinaryDGS);
	}

	@Test
	public void testReadImage() throws Exception {

		reader.readImage("", null, null, null, null, null);

		iiifImageAdapterInstanceProvider.MCR.assertParameters("getIiifImageAdapter", 0);

	}

}
