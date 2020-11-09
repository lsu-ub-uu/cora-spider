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
package se.uu.ub.cora.spider.extended;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.SpiderRecordDeleterImp;

public class WorkOrderExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProviderSpy;

	@BeforeMethod
	public void setUp() {
		LoggerFactory loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		factory = new WorkOrderExtendedFunctionalityFactory();
		dependencyProviderSpy = new SpiderDependencyProviderSpy(Collections.emptyMap());
		factory.initializeUsingDependencyProvider(dependencyProviderSpy);
	}

	@Test
	public void testGetExtendedFunctionalityContexts() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 3);
		assertCorrectContextUsingIndexNumberAndPosition(0, 0,
				ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(1, 0,
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(2, 0,
				ExtendedFunctionalityPosition.CREATE_BEFORE_RETURN);
	}

	private void assertCorrectContextUsingIndexNumberAndPosition(int index, int runAsNumber,
			ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityContext workOrderEnhancer = factory.getExtendedFunctionalityContexts()
				.get(index);
		assertEquals(workOrderEnhancer.extendedFunctionalityPosition, position);
		assertEquals(workOrderEnhancer.recordType, "workOrder");
		assertEquals(workOrderEnhancer.runAsNumber, runAsNumber);
	}

	@Test
	public void testWorkOrderCreateBeforeValidation() {
		ExtendedFunctionality functionality = factory.factor(
				ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "workOrder");
		assertTrue(functionality instanceof WorkOrderEnhancerAsExtendedFunctionality);
	}

	@Test
	public void testWorkOrderCreateAfterValidation() {
		WorkOrderExecutorAsExtendedFunctionality functionality = (WorkOrderExecutorAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION,
						"workOrder");
		assertSame(functionality.getDependencyProvider(), dependencyProviderSpy);
	}

	@Test
	public void testWorkOrderCreateBeforeReturn() {
		WorkOrderDeleterAsExtendedFunctionality functionality = (WorkOrderDeleterAsExtendedFunctionality) factory
				.factor(ExtendedFunctionalityPosition.CREATE_BEFORE_RETURN, "workOrder");
		SpiderRecordDeleterImp recordDeleter = (SpiderRecordDeleterImp) functionality
				.getRecordDeleter();
		assertSame(recordDeleter.getDependencyProvider(), dependencyProviderSpy);
	}
}
