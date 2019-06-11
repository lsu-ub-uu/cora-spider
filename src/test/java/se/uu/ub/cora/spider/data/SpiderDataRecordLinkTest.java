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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class SpiderDataRecordLinkTest {

    SpiderDataRecordLink spiderRecordLink;

    @BeforeMethod
    public void setUp() {
        spiderRecordLink = SpiderDataRecordLink.withNameInData("nameInData");

        SpiderDataAtomic linkedRecordType = SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "myLinkedRecordType");
        spiderRecordLink.addChild(linkedRecordType);

        SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "myLinkedRecordId");
        spiderRecordLink.addChild(linkedRecordId);

    }
    @Test
    public void testInit(){
        assertEquals(spiderRecordLink.getNameInData(), "nameInData");
        assertNotNull(spiderRecordLink.getAttributes());
        assertNotNull(spiderRecordLink.getChildren());
        assertEquals(spiderRecordLink.extractAtomicValue("linkedRecordType"), "myLinkedRecordType");
        assertEquals(spiderRecordLink.extractAtomicValue("linkedRecordId"), "myLinkedRecordId");
        assertNotNull(spiderRecordLink.getActions());
    }

    @Test
    public void testInitWithRepeatId() {
        spiderRecordLink.setRepeatId("hugh");
        assertEquals(spiderRecordLink.getRepeatId(), "hugh");
    }

    @Test
    public void testAddAttribute() {
        spiderRecordLink = SpiderDataRecordLink.withNameInData("nameInData");
        spiderRecordLink.addAttributeByIdWithValue("someId", "someValue");

        Map<String, String> attributes = spiderRecordLink.getAttributes();
        Map.Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertEquals(entry.getKey(), "someId");
        assertEquals(entry.getValue(), "someValue");
    }
    @Test
    public void testInitWithLinkedPath(){
        SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("linkedPath");
        spiderRecordLink.addChild(spiderDataGroup);
        assertNotNull(spiderRecordLink.getFirstChildWithNameInData("linkedPath"));
    }

    @Test
    public void testAddAction() {
        spiderRecordLink.addAction(Action.READ);

        assertTrue(spiderRecordLink.getActions().contains(Action.READ));
        assertFalse(spiderRecordLink.getActions().contains(Action.DELETE));
        // small hack to get 100% coverage on enum
        Action.valueOf(Action.READ.toString());
    }

    @Test
	public void testToDataRecordLink() {
		DataGroup dataRecordLink = spiderRecordLink.toDataGroup();

		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordType"), "myLinkedRecordType");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordId"), "myLinkedRecordId");
	}

    @Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderRecordLink.setRepeatId("essan");
		DataGroup dataRecordLink = spiderRecordLink.toDataGroup();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordType"), "myLinkedRecordType");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordId"), "myLinkedRecordId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}


    @Test
	public void testFromDataRecordLink() {
        DataGroup dataRecordLink = createRecordLinkAsDataGroup();

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);

        assertCorrectFromDataRecordLink(spiderDataRecordLink);
	}

    private DataGroup createRecordLinkAsDataGroup() {
        DataGroup dataRecordLink = DataGroup.withNameInData("nameInData");

        DataAtomic linkedRecordType = DataAtomic.withNameInDataAndValue("linkedRecordType", "myLinkedRecordType");
        dataRecordLink.addChild(linkedRecordType);

        DataAtomic linkedRecordId = DataAtomic.withNameInDataAndValue("linkedRecordId", "myLinkedRecordId");
        dataRecordLink.addChild(linkedRecordId);
        return dataRecordLink;
    }

    private void assertCorrectFromDataRecordLink(SpiderDataRecordLink spiderDataRecordLink) {
        assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");

        SpiderDataAtomic convertedRecordType = (SpiderDataAtomic) spiderDataRecordLink.getFirstChildWithNameInData("linkedRecordType");
        assertEquals(convertedRecordType.getValue(), "myLinkedRecordType");

        SpiderDataAtomic convertedRecordId = (SpiderDataAtomic) spiderDataRecordLink.getFirstChildWithNameInData("linkedRecordId");
        assertEquals(convertedRecordId.getValue(), "myLinkedRecordId");
    }

    @Test
	public void testFromDataRecordLinkWithRepeatId() {
        DataGroup dataRecordLink = createRecordLinkAsDataGroup();
        dataRecordLink.setRepeatId("roi");

        SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
        assertCorrectFromDataRecordLinkWithRepeatId(spiderDataRecordLink);
	}

    @Test
    public void testFromDataGroupWithAttribute() {
        DataGroup dataRecordLink = createRecordLinkAsDataGroup();
        dataRecordLink.addAttributeByIdWithValue("nameInData", "value");

        SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
                .fromDataRecordLink(dataRecordLink);

        Map<String, String> attributes = spiderDataRecordLink.getAttributes();
        Map.Entry<String, String> entry = attributes.entrySet().iterator().next();
        assertEquals(entry.getKey(), "nameInData");
        assertEquals(entry.getValue(), "value");
    }


    private void assertCorrectFromDataRecordLinkWithRepeatId(SpiderDataRecordLink spiderDataRecordLink) {
        assertCorrectFromDataRecordLink(spiderDataRecordLink);
        assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
    }

	@Test
	public void testFromDataRecordLinkWithLinkedRepeatId(){
        DataGroup dataRecordLink = createRecordLinkAsDataGroup();
        DataAtomic linkedRepeatId = DataAtomic.withNameInDataAndValue("linkedRepeatId", "linkedOne");
        dataRecordLink.addChild(linkedRepeatId);

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.
                fromDataRecordLink(dataRecordLink);
        SpiderDataAtomic convertedLinkedRepeatId = (SpiderDataAtomic) spiderDataRecordLink.getFirstChildWithNameInData("linkedRepeatId");
        assertEquals(convertedLinkedRepeatId.getValue(), "linkedOne");
	}

	@Test
	public void testFromDataRecordLinkWithLinkedLinkedPath(){
        DataGroup dataRecordLink = createRecordLinkAsDataGroup();
        dataRecordLink.addChild(DataGroup.withNameInData("linkedPath"));

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getFirstChildWithNameInData("linkedPath").getNameInData(), "linkedPath");
	}

}
