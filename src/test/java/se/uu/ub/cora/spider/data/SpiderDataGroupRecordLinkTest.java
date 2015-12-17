package se.uu.ub.cora.spider.data;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

import static org.testng.Assert.*;

public class SpiderDataGroupRecordLinkTest {

    SpiderDataGroupRecordLink spiderRecordLink;

    @BeforeMethod
    public void setUp() {
        spiderRecordLink = SpiderDataGroupRecordLink.withNameInData("nameInData");

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
}
