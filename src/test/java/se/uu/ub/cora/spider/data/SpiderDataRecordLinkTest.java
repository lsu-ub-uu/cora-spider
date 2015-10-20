package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public class SpiderDataRecordLinkTest {
	private SpiderDataRecordLink spiderRecordLink;

	@BeforeMethod
	public void setUp() {
		spiderRecordLink = SpiderDataRecordLink.withNameInDataAndRecordTypeAndRecordId("nameInData",
				"recordType", "recordId");

	}

	@Test
	public void testInit() {
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getRecordType(), "recordType");
		assertEquals(spiderRecordLink.getRecordId(), "recordId");
	}

	@Test
	public void testInitWithRepeatId() {
		spiderRecordLink.setRepeatId("hugh");
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getRecordType(), "recordType");
		assertEquals(spiderRecordLink.getRecordId(), "recordId");
		assertEquals(spiderRecordLink.getRepeatId(), "hugh");
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
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();

		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getRecordType(), "recordType");
		assertEquals(dataRecordLink.getRecordId(), "recordId");
	}

	@Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderRecordLink.setRepeatId("essan");
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getRecordType(), "recordType");
		assertEquals(dataRecordLink.getRecordId(), "recordId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}

	@Test
	public void testFromDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndRecordTypeAndRecordId("nameInData", "recordType", "recordId");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getRecordType(), "recordType");
		assertEquals(spiderDataRecordLink.getRecordId(), "recordId");
	}

	@Test
	public void testFromDataRecordLinkWithRepeatId() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndRecordTypeAndRecordId("nameInData", "recordType", "recordId");
		dataRecordLink.setRepeatId("roi");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getRecordType(), "recordType");
		assertEquals(spiderDataRecordLink.getRecordId(), "recordId");
		assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
	}
}
