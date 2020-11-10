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
package se.uu.ub.cora.spider.consistency;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.MetadataConsistencyValidatorAsExtendedFunctionality;
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
		MetadataConsistencyValidatorAsExtendedFunctionality functionality = (MetadataConsistencyValidatorAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION,
						"metadataGroup");
		MetadataConsistencyGroupAndCollectionValidatorImp validator = (MetadataConsistencyGroupAndCollectionValidatorImp) functionality
				.getValidator();

		assertEquals(validator.getRecordType(), "metadataGroup");
		assertSame(validator.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorCreateAfterValidationForMetadataCollectionVariable() {
		MetadataConsistencyValidatorAsExtendedFunctionality functionality = (MetadataConsistencyValidatorAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION,
						"metadataCollectionVariable");
		MetadataConsistencyGroupAndCollectionValidatorImp validator = (MetadataConsistencyGroupAndCollectionValidatorImp) functionality
				.getValidator();

		assertEquals(validator.getRecordType(), "metadataCollectionVariable");
		assertSame(validator.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorUpdateAfterValidationForMetadataGroup() {
		MetadataConsistencyValidatorAsExtendedFunctionality functionality = (MetadataConsistencyValidatorAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION,
						"metadataGroup");
		MetadataConsistencyGroupAndCollectionValidatorImp validator = (MetadataConsistencyGroupAndCollectionValidatorImp) functionality
				.getValidator();

		assertEquals(validator.getRecordType(), "metadataGroup");
		assertSame(validator.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}

	@Test
	public void testConsistencyValidatorUpdateAfterValidationForMetadataCollectionVariable() {
		MetadataConsistencyValidatorAsExtendedFunctionality functionality = (MetadataConsistencyValidatorAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION,
						"metadataCollectionVariable");
		MetadataConsistencyGroupAndCollectionValidatorImp validator = (MetadataConsistencyGroupAndCollectionValidatorImp) functionality
				.getValidator();

		assertEquals(validator.getRecordType(), "metadataCollectionVariable");
		assertSame(validator.getRecordStorage(), dependencyProviderSpy.getRecordStorage());
	}
}
