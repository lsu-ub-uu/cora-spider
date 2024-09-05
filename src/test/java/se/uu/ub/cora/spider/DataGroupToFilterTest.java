/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.data.internal.DataGroupToFilterImp;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;

public class DataGroupToFilterTest {
	DataGroupToFilter converter;
	DataGroupSpy dataGroup;

	@BeforeMethod
	public void beforeMethod() {
		converter = new DataGroupToFilterImp();
		dataGroup = createXncludeGroup();
	}

	@Test
	public void testEmptyFilter() throws Exception {

		Filter filter = converter.convert(dataGroup);

		assertFalse(filter.filtersResults());
	}

	@Test
	public void testFromNo() throws Exception {
		String nameInData = "fromNo";
		dataGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				nameInData);
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "5", nameInData);

		Filter filter = converter.convert(dataGroup);

		assertTrue(filter.filtersResults());
		assertEquals(filter.fromNo, 5);
	}

	@Test
	public void testToNo() throws Exception {
		String nameInData = "toNo";
		dataGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				nameInData);
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "5", nameInData);

		Filter filter = converter.convert(dataGroup);

		assertTrue(filter.filtersResults());
		assertEquals(filter.toNo, 5);

	}

	@Test
	public void testInclude() throws Exception {
		String nameInData = "include";
		dataGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				nameInData);
		DataGroupSpy includeGroup = createXncludeGroup();
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> includeGroup, nameInData);

		Filter filter = converter.convert(dataGroup);

		assertTrue(filter.filtersResults());
		assertEquals(filter.include.size(), 2);

		Part part = filter.include.get(0);
		assertEquals(part.conditions.size(), 2);
		Condition condition = part.conditions.get(0);
		assertEquals(condition.key(), "nameInData11");
		assertEquals(condition.value(), "value11");
		Condition condition1 = part.conditions.get(1);
		assertEquals(condition1.key(), "nameInData12");
		assertEquals(condition1.value(), "value12");

		Part part2 = filter.include.get(1);
		assertEquals(part2.conditions.size(), 2);
		Condition condition20 = part2.conditions.get(0);
		assertEquals(condition20.key(), "nameInData21");
		assertEquals(condition20.value(), "value21");
		Condition condition21 = part2.conditions.get(1);
		assertEquals(condition21.key(), "nameInData22");
		assertEquals(condition21.value(), "value22");
	}

	private DataGroupSpy createXncludeGroup() {
		DataGroupSpy includeGroup = new DataGroupSpy();
		String nameInData = "part";
		DataGroupSpy includePart = createPart("1");
		DataGroupSpy includePart2 = createPart("2");
		includeGroup.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> List.of(includePart, includePart2), nameInData);

		return includeGroup;
	}

	private DataGroupSpy createPart(String prefix) {
		DataAtomicOldSpy atomic = createAtomic(prefix + "1");
		DataAtomicOldSpy atomic2 = createAtomic(prefix + "2");

		DataGroupSpy part = new DataGroupSpy();
		part.MRV.setDefaultReturnValuesSupplier("getChildren", () -> List.of(atomic, atomic2));
		return part;
	}

	private DataAtomicOldSpy createAtomic(String suffix) {
		DataAtomicOldSpy atomic = new DataAtomicOldSpy();
		atomic.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "nameInData" + suffix);
		atomic.MRV.setDefaultReturnValuesSupplier("getValue", () -> "value" + suffix);
		return atomic;
	}

	@Test
	public void testExclude() throws Exception {
		String nameInData = "exclude";
		dataGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				nameInData);
		DataGroupSpy includeGroup = createXncludeGroup();
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> includeGroup, nameInData);

		Filter filter = converter.convert(dataGroup);

		List<Part> exclude = filter.exclude;
		assertTrue(filter.filtersResults());
		assertEquals(exclude.size(), 2);

		Part part = exclude.get(0);
		assertEquals(part.conditions.size(), 2);
		Condition condition = part.conditions.get(0);
		assertEquals(condition.key(), "nameInData11");
		assertEquals(condition.value(), "value11");
		Condition condition1 = part.conditions.get(1);
		assertEquals(condition1.key(), "nameInData12");
		assertEquals(condition1.value(), "value12");

		Part part2 = exclude.get(1);
		assertEquals(part2.conditions.size(), 2);
		Condition condition20 = part2.conditions.get(0);
		assertEquals(condition20.key(), "nameInData21");
		assertEquals(condition20.value(), "value21");
		Condition condition21 = part2.conditions.get(1);
		assertEquals(condition21.key(), "nameInData22");
		assertEquals(condition21.value(), "value22");
	}

}
