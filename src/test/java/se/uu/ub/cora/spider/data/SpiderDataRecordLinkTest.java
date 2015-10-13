package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SpiderDataRecordLinkTest {
	private SpiderDataRecordLink spiderRecordLink;

	@BeforeMethod
	public void setUp() {
		spiderRecordLink = SpiderDataRecordLink.withNameInDataAndRecordTypeAndRecordId("nameInData", "recordType", "recordId");

	}

	@Test
	public void testInit() {
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getRecordType(), "recordType");
		assertEquals(spiderRecordLink.getRecordId(), "recordId");
	}

	@Test
	public void testAddAction() {
		spiderRecordLink.addAction(Action.READ);

		assertTrue(spiderRecordLink.getActions().contains(Action.READ));
		assertFalse(spiderRecordLink.getActions().contains(Action.DELETE));
		// small hack to get 100% coverage on enum
		Action.valueOf(Action.READ.toString());
	}
}
