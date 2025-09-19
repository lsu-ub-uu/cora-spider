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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.binary.iiif.IiifAdapterResponse;
import se.uu.ub.cora.binary.iiif.IiifParameters;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.binary.iiif.internal.IiifReaderImp;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class IiifReaderTest {

	private static final String AUTH_TOKEN = "authtoken";
	private static final String AUTH_TOKEN_WITH_CASES = "aUtHtOkEn";
	private static final String ACTION_READ = "read";
	private static final String JP2_PERMISSION = "binary.jp2";
	private static final String SOME_METHOD = "someMethod";
	private static final String SOME_REQUESTED_URI = "someRequestedUri";
	private static final String VISIBILITY = "visibility";

	private static final String DATA_DIVIDER = "someDataDivider";
	private static final String TYPE = "type";
	private static final String ID = "someId";

	private static final String SOME_MIME_TYPE = "someMimeType";
	private static final String MASTER = "master";
	private static final String THUMBNAIL = "thumbnail";
	private static final String MEDIUM = "medium";
	private static final String LARGE = "large";
	private static final String JP2 = "jp2";
	private static final String SOME_FILE_SIZE = "2123";
	private static final String ORIGINAL_FILE_NAME = "someOriginalFilename";

	private LoggerFactorySpy loggerFactorySpy;
	private IiifInstanceProviderSpy iiifInstanceProvider;

	private IiifReader reader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private DataRecordGroupSpy readBinaryDGS;
	private DataGroupSpy resourceTypeDGS;
	private DataGroupSpy adminInfo;
	private IiifAdapterSpy iiifAdapterSpy;
	Map<String, String> headersMap;

	@BeforeMethod
	private void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		headersMap = new HashMap<>();

		iiifInstanceProvider = new IiifInstanceProviderSpy();
		BinaryProvider.onlyForTestSetIiifImageAdapterInstanceProvider(iiifInstanceProvider);
		iiifAdapterSpy = new IiifAdapterSpy();
		iiifInstanceProvider.MRV.setDefaultReturnValuesSupplier("getIiifAdapter",
				() -> iiifAdapterSpy);
		dependencyProvider = new SpiderDependencyProviderSpy();
		setUpDependencyProvider();
		setupBinaryRecord();

		reader = IiifReaderImp.usingDependencyProvider(dependencyProvider);
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
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> DATA_DIVIDER);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getType", () -> TYPE);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getId", () -> ID);

		adminInfo = new DataGroupSpy();
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> adminInfo, "adminInfo");

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
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				JP2);

		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinaryDGS);
	}

	private void setVisibilityInAdminInfoInBinaryRecord(String visibility) {
		adminInfo.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> visibility, VISIBILITY);
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		IiifReaderImp readerImp = (IiifReaderImp) reader;
		assertSame(readerImp.onlyForTestGetDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testReadImageNotFoundThrowRecordNotFoundException() {
		recordStorage.MRV.setAlwaysThrowException("read",
				se.uu.ub.cora.storage.RecordNotFoundException
						.withMessage("message from exception"));
		try {
			reader.readIiif(ID, null, null, headersMap);
			fail("it should throw exception");
		} catch (Exception error) {
			assertTrue(error instanceof RecordNotFoundException);
			assertEquals(error.getMessage(),
					"Record not found for recordType: binary and recordId: " + ID);

			dependencyProvider.MCR.assertMethodWasCalled("getRecordStorage");
			recordStorage.MCR.assertParameters("read", 0, "binary", ID);

			iiifInstanceProvider.MCR.assertMethodNotCalled("getIiifAdapter");
		}
	}

	@Test
	public void testCallIiifServer() {
		setVisibilityInAdminInfoInBinaryRecord("published");

		reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);

		dependencyProvider.MCR.assertMethodWasCalled("getRecordStorage");
		recordStorage.MCR.assertParameters("read", 0, "binary", ID);

		iiifInstanceProvider.MCR.assertParameters("getIiifAdapter", 0);
		iiifAdapterSpy.MCR.assertMethodWasCalled("callIiifServer");

		IiifParameters iiifParameters = (IiifParameters) iiifAdapterSpy.MCR
				.getParameterForMethodAndCallNumberAndParameter("callIiifServer", 0,
						"iiifParameters");

		assertEquals(iiifParameters.dataDivider(), DATA_DIVIDER);
		assertEquals(iiifParameters.type(), TYPE);
		assertEquals(iiifParameters.id(), ID);
		assertEquals(iiifParameters.representation(), JP2);
		assertEquals(iiifParameters.uri(), SOME_REQUESTED_URI);
		assertEquals(iiifParameters.method(), SOME_METHOD);
		assertSame(iiifParameters.headersMap(), headersMap);
	}

	@Test
	public void testReturnedResponseFromIiifServerCall() {
		setVisibilityInAdminInfoInBinaryRecord("published");
		IiifAdapterResponse iiifAdapterResponse = createResponseAndSetItToBeReturnedFromAdapter();

		IiifResponse iiifResponse = reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD,
				headersMap);

		assertEquals(iiifResponse.status(), iiifAdapterResponse.status());
		assertEquals(iiifResponse.headers(), iiifAdapterResponse.headers());
		assertEquals(iiifResponse.body(), iiifAdapterResponse.body());
	}

	private IiifAdapterResponse createResponseAndSetItToBeReturnedFromAdapter() {
		InputStream inputStream = new ByteArrayInputStream("someInputStream".getBytes());
		IiifAdapterResponse iiifAdapterResponse = new IiifAdapterResponse(418, headersMap,
				inputStream);
		iiifAdapterSpy.MRV.setDefaultReturnValuesSupplier("callIiifServer",
				() -> iiifAdapterResponse);
		return iiifAdapterResponse;
	}

	@Test
	public void testUserIsFetchedFromMapUsingNoToken() {
		setVisibilityInAdminInfoInBinaryRecord("published");

		reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);

		Object authToken = authenticator.MCR
				.getParameterForMethodAndCallNumberAndParameter("getUserForToken", 0, "authToken");
		assertNull(authToken);
	}

	@Test
	public void testUserIsFetchedFromMapUsingToken() {
		setVisibilityInAdminInfoInBinaryRecord("published");
		headersMap.put(AUTH_TOKEN, "someToken");

		reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);

		authenticator.MCR.assertParameters("getUserForToken", 0, "someToken");
	}

	@Test
	public void testUserIsFetchedFromMapUsingTokenWithCases() {
		setVisibilityInAdminInfoInBinaryRecord("published");
		headersMap.put(AUTH_TOKEN_WITH_CASES, "someOtherToken");

		reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);

		authenticator.MCR.assertParameters("getUserForToken", 0, "someOtherToken");
	}

	@Test
	public void testReadIiifAsAuthorizedUser() {
		setVisibilityInAdminInfoInBinaryRecord("published");
		reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				readBinaryDGS);

		RecordTypeHandlerOldSpy recordTypeHandlerSpy = (RecordTypeHandlerOldSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandlerUsingDataRecordGroup", 0);
		DataGroupTermCollectorSpy termCollectorSpy = (DataGroupTermCollectorSpy) dependencyProvider.MCR
				.getReturnValue("getDataGroupTermCollector", 0);

		var definitionId = recordTypeHandlerSpy.MCR.getReturnValue("getDefinitionId", 0);

		termCollectorSpy.MCR.assertParameters("collectTerms", 0, definitionId, readBinaryDGS);

		CollectTerms terms = (CollectTerms) termCollectorSpy.MCR.getReturnValue("collectTerms", 0);

		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);
		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, ACTION_READ,
				JP2_PERMISSION, terms.permissionTerms);
	}

	@Test
	public void testThrowAuthorizationExceptionThrownIfUnAuthorized() {
		String errorMessage = "someExceptionMessage";
		authorizator.MRV.setAlwaysThrowException(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData",
				new AuthorizationException(errorMessage));
		try {
			reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);
			fail();
		} catch (Exception error) {
			assertTrue(error instanceof AuthorizationException);
			assertEquals(error.getMessage(), errorMessage);
		}
	}

	@Test
	public void testJP2NotFound() {
		setVisibilityInAdminInfoInBinaryRecord("published");
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, JP2);
		try {
			reader.readIiif(ID, SOME_REQUESTED_URI, SOME_METHOD, headersMap);
			fail("Exception should have been thrown due to JP2 not existing");
		} catch (Exception error) {
			assertTrue(error instanceof RecordNotFoundException);
			assertEquals(error.getMessage(),
					"Could not find a JP2 representation for binary and recordId: " + ID);
			dependencyProvider.MCR.assertMethodNotCalled("callIiifServer");
		}
	}
}
