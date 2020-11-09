/*
 * Copyright 2016, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.extended;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.RecordIdGeneratorProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.StreamStorageProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.SpiderRecordDeleterImp;
import se.uu.ub.cora.spider.workorder.WorkOrderDeleter;
import se.uu.ub.cora.spider.workorder.WorkOrderEnhancer;
import se.uu.ub.cora.spider.workorder.WorkOrderExecutor;

public class BaseExtendedFunctionalityProviderTest {
	private ExtendedFunctionalityProvider baseExtendedFunctionalityProvider;
	private SpiderDependencyProvider dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void setUp() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		StreamStorageProviderSpy streamStorageProviderSpy = new StreamStorageProviderSpy();
		dependencyProvider.setStreamStorageProvider(streamStorageProviderSpy);
		RecordIdGeneratorProviderSpy recordIdGeneratorProviderSpy = new RecordIdGeneratorProviderSpy();
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProviderSpy);

		baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider(
				dependencyProvider);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMetadataValidation() {
		fetchAndAssertCreateBeforeMetadataValidation(null);
		fetchAndAssertCreateBeforeMetadataValidation("");
		fetchAndAssertCreateBeforeMetadataValidation("UnknownType");
		fetchAndAssertCreateBeforeMetadataValidationForAppToken();
		fetchAndAssertCreateBeforeMetadataValidationForWorkOrder();

	}

	private void fetchAndAssertCreateBeforeMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	private void fetchAndAssertCreateBeforeMetadataValidationForAppToken() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation("appToken");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof AppTokenEnhancerAsExtendedFunctionality);
	}

	private void fetchAndAssertCreateBeforeMetadataValidationForWorkOrder() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation("workOrder");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof WorkOrderEnhancer);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidation() {
		fetchAndAssertCreateAfterMetadataValidation(null);
		fetchAndAssertCreateAfterMetadataValidation("");
		fetchAndAssertCreateAfterMetadataValidation("UnkownType");
	}

	@Test
	public void testGetAddedFunctionalityForCreateAfterMetadataValidationForMetadataGroup() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation("metadataGroup");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	@Test
	public void testGetAddedFunctionalityForCreateAfterMetadataValidationForCollectionVariable() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation("metadataCollectionVariable");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	@Test
	public void testGetAddedFunctionalityForCreateAfterMetadataValidationForWorkOrder() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation("workOrder");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof WorkOrderExecutor);
	}

	private void fetchAndAssertCreateAfterMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeReturn() {
		fetchAndAssertCreateBeforeReturn(null);
		fetchAndAssertCreateBeforeReturn("");
		fetchAndAssertCreateBeforeReturn("UnkownType");
		fetchAndAssertCreateBeforeReturnForAppToken();
		fetchAndAssertCreateBeforeReturnForWorkOrder();
	}

	private void fetchAndAssertCreateBeforeReturn(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	private void fetchAndAssertCreateBeforeReturnForAppToken() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn("appToken");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof UserUpdaterForAppTokenAsExtendedFunctionality);
	}

	private void fetchAndAssertCreateBeforeReturnForWorkOrder() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn("workOrder");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof WorkOrderDeleter);
		WorkOrderDeleter woExtendedFunctionality = (WorkOrderDeleter) extendedFunctionality;
		assertTrue(woExtendedFunctionality.getRecordDeleter() instanceof SpiderRecordDeleterImp);
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeMetadataValidationNullAsType() {
		fetchAndAssertUpdateBeforeMetadataValidation(null);
		fetchAndAssertUpdateBeforeMetadataValidation("");
		fetchAndAssertUpdateBeforeMetadataValidation("UnknownType");
	}

	private void fetchAndAssertUpdateBeforeMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidationNullAsType() {
		fetchAndAssertUpdateAfterMetadataValidation(null);
		fetchAndAssertUpdateAfterMetadataValidation("");
		fetchAndAssertUpdateAfterMetadataValidation("UnknownType");
	}

	private void fetchAndAssertUpdateAfterMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidationMetadataGroupAsType() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation("metadataGroup");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidationMetadataCollectionVariableAsType() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation("metadataCollectionVariable");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	@Test
	public void testGetFunctionalityBeforeDelete() {
		fetchAndAssertBeforeDelete(null);
		fetchAndAssertBeforeDelete("");
		fetchAndAssertBeforeDelete("UnknownType");
	}

	private void fetchAndAssertBeforeDelete(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityBeforeDelete(recordType);
		assertEquals(bEFP, Collections.emptyList());
	}

	@Test
	public void testGetFunctionalityAfterDelete() {
		fetchAndAssertAfterDelete(null);
		fetchAndAssertAfterDelete("");
		fetchAndAssertAfterDelete("UnknownType");
	}

	private void fetchAndAssertAfterDelete(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityAfterDelete(recordType);
		assertEquals(bEFP, Collections.emptyList());
	}
}
