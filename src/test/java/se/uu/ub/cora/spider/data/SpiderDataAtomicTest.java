/*
 * Copyright 2015, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;

public class SpiderDataAtomicTest {

	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
	}

	@Test
	public void testInit() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData",
				"value");
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testInitWithRepeatId() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData",
				"value");
		spiderDataAtomic.setRepeatId("grr");
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
		assertEquals(spiderDataAtomic.getRepeatId(), "grr");
	}

	@Test
	public void testFromDataAtomic() {
		DataAtomic dataAtomic = new DataAtomicSpy("nameInData", "value");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testFromDataAtomicWithRepeatId() {
		DataAtomic dataAtomic = new DataAtomicSpy("nameInData", "value", "two");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
		assertEquals(spiderDataAtomic.getRepeatId(), "two");
	}

	@Test
	public void testToDataAtomic() {
		String nameInData = "someNameInData";
		String value = "someValue";

		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue(nameInData,
				value);
		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomicFactory.nameInData, nameInData);
		assertEquals(dataAtomicFactory.value, value);
		assertEquals(dataAtomicFactory.reurnedDataAtomic, dataAtomic);
	}

	@Test
	public void testToDataAtomicWithRepeatId() {
		String nameInData = "someNameInData";
		String value = "someValue";
		String repeatId = "tt";

		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue(nameInData,
				value);
		spiderDataAtomic.setRepeatId(repeatId);

		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomicFactory.nameInData, nameInData);
		assertEquals(dataAtomicFactory.value, value);
		assertEquals(dataAtomicFactory.reurnedDataAtomic, dataAtomic);
	}
}
