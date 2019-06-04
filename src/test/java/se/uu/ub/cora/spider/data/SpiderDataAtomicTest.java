/*
 * Copyright 2015 Uppsala University Library
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

import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;

public class SpiderDataAtomicTest {
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
		DataAtomic dataAtomic = DataAtomic.withNameInDataAndValue("nameInData", "value");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testFromDataAtomicWithRepeatId() {
		DataAtomic dataAtomic = DataAtomic.withNameInDataAndValue("nameInData", "value");
		dataAtomic.setRepeatId("two");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
		assertEquals(spiderDataAtomic.getRepeatId(), "two");
	}

	@Test
	public void testToDataAtomic() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData",
				"value");
		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomic.getNameInData(), "nameInData");
		assertEquals(dataAtomic.getValue(), "value");
	}

	@Test
	public void testToDataAtomicWithRepeatId() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData",
				"value");
		spiderDataAtomic.setRepeatId("tt");
		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomic.getNameInData(), "nameInData");
		assertEquals(dataAtomic.getValue(), "value");
		assertEquals(dataAtomic.getRepeatId(), "tt");
	}
}
