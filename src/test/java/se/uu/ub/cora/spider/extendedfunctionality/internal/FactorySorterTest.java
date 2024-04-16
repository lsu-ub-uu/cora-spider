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
package se.uu.ub.cora.spider.extendedfunctionality.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;

public class FactorySorterTest {
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private String testedClassName = "FactorySorterImp";

	private List<ExtendedFunctionalityFactory> fakeImplementations;
	private FactorySorterImp factorySorter;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void setUp() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		fakeImplementations = new ArrayList<>();
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);
	}

	@Test
	public void testGetDependencyProvider() {
		assertSame(factorySorter.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testGetExtendedFunctionalityFactoriesNeededForTest() {
		createFactorySpyInList(CREATE_AFTER_AUTHORIZATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		assertSame(factorySorter.getExtendedFunctionalityFactoriesNeededForTest(),
				fakeImplementations);
	}

	@Test
	public void testCreateBeforeMetadataValidationNoFunctionality() {
		FactorySorterImp factorySorterNoFactories = new FactorySorterImp(dependencyProvider,
				Collections.emptyList());
		List<ExtendedFunctionality> functionality = factorySorterNoFactories
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");
		assertTrue(functionality.isEmpty());
	}

	private ExtendedFunctionalityFactorySpy createFactorySpyInList(
			ExtendedFunctionalityPosition extendedFunctionalityPosition, String recordType,
			int runAsNumber) {
		ExtendedFunctionalityContext efc = new ExtendedFunctionalityContext(
				extendedFunctionalityPosition, recordType, runAsNumber);
		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = new ArrayList<>();
		extendedFunctionalityContexts.add(efc);

		ExtendedFunctionalityFactorySpy factorySpy = new ExtendedFunctionalityFactorySpy(
				extendedFunctionalityContexts);
		fakeImplementations.add(factorySpy);
		return factorySpy;
	}

	private void assertNoFunctionalityWasReturned(ExtendedFunctionalityFactorySpy factorySpy,
			List<ExtendedFunctionality> functionality) {
		factorySpy.MCR.assertMethodNotCalled("factor");
		assertTrue(functionality.isEmpty());
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityWrongPositionAndEmptyType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testLoggingOnStartup() {
		setupTestForLogging();

		expectedInfoLog(0, "Extended functionality found and sorted as follows:");
		expectedInfoLog(1, " position: CREATE_AFTER_AUTHORIZATION");
		expectedInfoLog(2, "  recordType: someRecordType");
		expectedInfoLog(3,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(4,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(5,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(6,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(7, " position: UPDATE_BEFORE_METADATA_VALIDATION");
		expectedInfoLog(8, "  recordType: otherRecordType2");
		expectedInfoLog(9,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(10, " position: UPDATE_AFTER_METADATA_VALIDATION");
		expectedInfoLog(11, "  recordType: otherRecordType");
		expectedInfoLog(12,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		expectedInfoLog(13, " position: DELETE_AFTER");
		expectedInfoLog(14, "  recordType: otherRecordType3");
		expectedInfoLog(15,
				"   class: se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy");
		assertEquals(loggerFactorySpy.getNoOfInfoLogMessagesUsingClassName(testedClassName), 16);
	}

	private void setupTestForLogging() {
		createFactorySpyInList(CREATE_AFTER_AUTHORIZATION, "someRecordType", 1);
		createFactorySpyInList(CREATE_AFTER_AUTHORIZATION, "someRecordType", 0);
		createFactorySpyInList(CREATE_AFTER_AUTHORIZATION, "someRecordType", 500);
		createFactorySpyInList(CREATE_AFTER_AUTHORIZATION, "someRecordType", -30);
		createFactoryWithMoreEFFsInSpyInList(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION, "otherRecordType",
				5);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		factorySorter.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
				"someRecordType");
	}

	private void expectedInfoLog(int messageNo, String infoText) {
		assertEquals(
				loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, messageNo),
				infoText);
	}

	private ExtendedFunctionalityFactorySpy createFactoryWithMoreEFFsInSpyInList(
			ExtendedFunctionalityPosition extendedFunctionalityPosition, String recordType,
			int runAsNumber) {
		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = new ArrayList<>();
		ExtendedFunctionalityContext efc = new ExtendedFunctionalityContext(
				extendedFunctionalityPosition, recordType, runAsNumber);
		extendedFunctionalityContexts.add(efc);
		ExtendedFunctionalityContext efc2 = new ExtendedFunctionalityContext(
				ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION, recordType + "2",
				2);
		extendedFunctionalityContexts.add(efc2);
		ExtendedFunctionalityContext efc3 = new ExtendedFunctionalityContext(
				ExtendedFunctionalityPosition.DELETE_AFTER, recordType + "3", 2);
		extendedFunctionalityContexts.add(efc3);

		ExtendedFunctionalityFactorySpy factorySpy = new ExtendedFunctionalityFactorySpy(
				extendedFunctionalityContexts);
		fakeImplementations.add(factorySpy);
		return factorySpy;
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityEmptyPositionWrongType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "notTheRecordTypeWeAreLookingFor", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityWrongPositionAndWrongType() {
		ExtendedFunctionalityPosition extendedFunctionalityPosition = CREATE_AFTER_AUTHORIZATION;
		String recordType = "notTheRecordTypeWeAreLookingFor";
		int runAsNumber = 0;
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				extendedFunctionalityPosition, recordType, runAsNumber);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityRightPositionAndRightType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		factorySpy.MCR.assertParameter("initializeUsingDependencyProvider", 0, "dependencyProvider",
				dependencyProvider);
		assertSame(functionality.get(0), getFirstReturnedExtendedFunctionality(factorySpy));
		factorySpy.MCR.assertParameters("factor", 0, CREATE_AFTER_AUTHORIZATION, "someRecordType");
	}

	private ExtendedFunctionality getFirstReturnedExtendedFunctionality(
			ExtendedFunctionalityFactorySpy factorySpy) {
		List<?> returnValue = (List<?>) factorySpy.MCR.getReturnValue("factor", 0);
		ExtendedFunctionality extendedFunctionality = (ExtendedFunctionality) returnValue.get(0);
		return extendedFunctionality;
	}

	@Test
	public void testCreateBeforeMetadataValidationWithTwoInOneFactory() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 0);
		factorySpy.numberOfReturnedFunctionalities = 2;
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		factorySpy.MCR.assertParameter("initializeUsingDependencyProvider", 0, "dependencyProvider",
				dependencyProvider);
		assertSame(functionality.get(0), getFirstReturnedExtendedFunctionality(factorySpy));
		factorySpy.MCR.assertParameters("factor", 0, CREATE_AFTER_AUTHORIZATION, "someRecordType");

		List<?> returnValue = (List<?>) factorySpy.MCR.getReturnValue("factor", 0);
		ExtendedFunctionality secondFunctionality = (ExtendedFunctionality) returnValue.get(1);
		assertSame(functionality.get(1), secondFunctionality);
	}

	@Test
	public void testTwoFunctionalityFactoriesForSameTypeAndRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 0);
		ExtendedFunctionalityFactorySpy factorySpy3 = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 500);
		ExtendedFunctionalityFactorySpy factorySpy4 = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", -30);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertSame(functionality.get(0), getFirstReturnedExtendedFunctionality(factorySpy4));
		assertSame(functionality.get(1), getFirstReturnedExtendedFunctionality(factorySpy2));
		assertSame(functionality.get(2), getFirstReturnedExtendedFunctionality(factorySpy));
		assertSame(functionality.get(3), getFirstReturnedExtendedFunctionality(factorySpy3));
	}

	@Test
	public void testTwoFunctionalityFactoriesForDifferentTypeAndSameRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_AFTER_METADATA_VALIDATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertSame(functionality.get(0), getFirstReturnedExtendedFunctionality(factorySpy));

		factorySpy2.MCR.assertMethodNotCalled("factor");
		factorySpy.MCR.assertParameters("factor", 0, CREATE_AFTER_AUTHORIZATION, "someRecordType");
	}

	@Test
	public void testTwoFunctionalityFactoriesForSameTypeAndDifferentRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_AFTER_AUTHORIZATION, "otherRecordType", 0);
		factorySorter = new FactorySorterImp(dependencyProvider, fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertSame(functionality.get(0), getFirstReturnedExtendedFunctionality(factorySpy));

		factorySpy2.MCR.assertMethodNotCalled("factor");
		factorySpy.MCR.assertParameters("factor", 0, CREATE_AFTER_AUTHORIZATION, "someRecordType");
	}

}
