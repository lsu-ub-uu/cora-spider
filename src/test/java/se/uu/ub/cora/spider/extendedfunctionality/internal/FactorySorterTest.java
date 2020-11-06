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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactorySpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;

public class FactorySorterTest {
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "FactorySorterImp";

	private List<ExtendedFunctionalityFactory> fakeImplementations;
	private FactorySorterImp factorySorter;

	@BeforeMethod
	public void setUp() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		fakeImplementations = new ArrayList<>();
		factorySorter = new FactorySorterImp(fakeImplementations);
	}

	@Test
	public void testGetExtendedFunctionalityFactoriesNeededForTest() {
		createFactorySpyInList(CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		assertSame(factorySorter.getExtendedFunctionalityFactoriesNeededForTest(),
				fakeImplementations);
	}

	@Test
	public void testCreateBeforeMetadataValidationNoFunctionality() {
		FactorySorterImp factorySorterNoFactories = new FactorySorterImp(Collections.emptyList());
		List<ExtendedFunctionality> functionality = factorySorterNoFactories
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
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
				CREATE_BEFORE_METADATA_VALIDATION, "", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testLoggingOnStartup() {
		setupTestForLogging();

		expectedInfoLog(0, "Extended functionality found and sorted as follows:");
		expectedInfoLog(1, " position: CREATE_BEFORE_METADATA_VALIDATION");
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
		createFactorySpyInList(CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 1);
		createFactorySpyInList(CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 0);
		createFactorySpyInList(CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 500);
		createFactorySpyInList(CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", -30);
		createFactoryWithMoreEFFsInSpyInList(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION, "otherRecordType",
				5);
		factorySorter = new FactorySorterImp(fakeImplementations);

		factorySorter.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
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
				CREATE_BEFORE_METADATA_VALIDATION, "notTheRecordTypeWeAreLookingFor", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityWrongPositionAndWrongType() {
		ExtendedFunctionalityPosition extendedFunctionalityPosition = CREATE_AFTER_METADATA_VALIDATION;
		String recordType = "notTheRecordTypeWeAreLookingFor";
		int runAsNumber = 0;
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				extendedFunctionalityPosition, recordType, runAsNumber);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityRightPositionAndRightType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertEquals(functionality.get(0), factorySpy.MCR.getReturnValue("factor", 0));
		factorySpy.MCR.assertParameters("factor", 0, CREATE_BEFORE_METADATA_VALIDATION,
				"someRecordType");
	}

	@Test
	public void testTwoFunctionalityFactoriesForSameTypeAndRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 0);
		ExtendedFunctionalityFactorySpy factorySpy3 = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 500);
		ExtendedFunctionalityFactorySpy factorySpy4 = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", -30);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertEquals(functionality.get(0), factorySpy4.MCR.getReturnValue("factor", 0));
		assertEquals(functionality.get(1), factorySpy2.MCR.getReturnValue("factor", 0));
		assertEquals(functionality.get(2), factorySpy.MCR.getReturnValue("factor", 0));
		assertEquals(functionality.get(3), factorySpy3.MCR.getReturnValue("factor", 0));
	}

	@Test
	public void testTwoFunctionalityFactoriesForDifferentTypeAndSameRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_AFTER_METADATA_VALIDATION, "someRecordType", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertEquals(functionality.get(0), factorySpy.MCR.getReturnValue("factor", 0));

		factorySpy2.MCR.assertMethodNotCalled("factor");
		factorySpy.MCR.assertParameters("factor", 0, CREATE_BEFORE_METADATA_VALIDATION,
				"someRecordType");
	}

	@Test
	public void testTwoFunctionalityFactoriesForSameTypeAndDifferentRecordTypeListInRightOrder() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "someRecordType", 1);
		ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
				CREATE_BEFORE_METADATA_VALIDATION, "otherRecordType", 0);
		factorySorter = new FactorySorterImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = factorySorter
				.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
						"someRecordType");

		assertEquals(functionality.get(0), factorySpy.MCR.getReturnValue("factor", 0));

		factorySpy2.MCR.assertMethodNotCalled("factor");
		factorySpy.MCR.assertParameters("factor", 0, CREATE_BEFORE_METADATA_VALIDATION,
				"someRecordType");
	}

}
