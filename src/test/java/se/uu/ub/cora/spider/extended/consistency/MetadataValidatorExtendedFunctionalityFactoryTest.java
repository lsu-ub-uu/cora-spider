/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.consistency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;

public class MetadataValidatorExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProviderSpy;

	@BeforeMethod
	public void setUp() {
		LoggerFactory loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		factory = new MetadataValidatorExtendedFunctionalityFactory();
		dependencyProviderSpy = new SpiderDependencyProviderSpy(Collections.emptyMap());
		factory.initializeUsingDependencyProvider(dependencyProviderSpy);
	}

	@Test
	public void testGetExtendedFunctionalityContexts() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 4);
		assertCorrectContextUsingIndexNumberAndPosition(0, 0, "metadataGroup",
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(1, 0, "metadataCollectionVariable",
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(2, 0, "metadataGroup",
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(3, 0, "metadataCollectionVariable",
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION);
	}

	private void assertCorrectContextUsingIndexNumberAndPosition(int index, int runAsNumber,
			String recordType, ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityContext workOrderEnhancer = factory.getExtendedFunctionalityContexts()
				.get(index);
		assertEquals(workOrderEnhancer.position, position);
		assertEquals(workOrderEnhancer.recordType, recordType);
		assertEquals(workOrderEnhancer.runAsNumber, runAsNumber);
	}

	@Test
	public void testConsistencyValidatorCreateAfterValidationForMetadataGroup() {
		List<ExtendedFunctionality> functionalities = factory.factor(
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION, "metadataGroup");
		MetadataConsistencyGroupAndCollectionValidator functionality = (MetadataConsistencyGroupAndCollectionValidator) functionalities
				.get(0);
		assertEquals(functionality.getRecordType(), "metadataGroup");
		assertSame(functionality.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorCreateAfterValidationForMetadataCollectionVariable() {
		List<ExtendedFunctionality> functionalities = factory.factor(
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION,
				"metadataCollectionVariable");
		MetadataConsistencyGroupAndCollectionValidator functionality = (MetadataConsistencyGroupAndCollectionValidator) functionalities
				.get(0);
		assertEquals(functionality.getRecordType(), "metadataCollectionVariable");
		assertSame(functionality.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorUpdateAfterValidationForMetadataGroup() {
		List<ExtendedFunctionality> functionalities = factory.factor(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION, "metadataGroup");
		MetadataConsistencyGroupAndCollectionValidator functionality = (MetadataConsistencyGroupAndCollectionValidator) functionalities
				.get(0);
		assertEquals(functionality.getRecordType(), "metadataGroup");
		assertSame(functionality.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorUpdateAfterValidationForMetadataCollectionVariable() {
		List<ExtendedFunctionality> functionalities = factory.factor(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION,
				"metadataCollectionVariable");
		MetadataConsistencyGroupAndCollectionValidator functionality = (MetadataConsistencyGroupAndCollectionValidator) functionalities
				.get(0);
		assertEquals(functionality.getRecordType(), "metadataCollectionVariable");
		assertSame(functionality.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}
}
