package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public class SpiderDataRecordLinkTest {
	private SpiderDataRecordLink spiderRecordLink;

	@BeforeMethod
	public void setUp() {
		spiderRecordLink = SpiderDataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData",
				"linkedRecordType", "linkedRecordId");

	}

	@Test
	public void testInit() {
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testInitWithRepeatId() {
		spiderRecordLink.setRepeatId("hugh");
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(spiderRecordLink.getRepeatId(), "hugh");
	}

	@Test
	public void testInitWithLinkedRepeatId(){
		spiderRecordLink.setLinkedRepeatId("1");
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRepeatId(), "1");
	}

	@Test
	public void testInitWithLinkedPath(){
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("linkedPathDataGroup");
		spiderRecordLink.setLinkedPath(spiderDataGroup);
		assertEquals(spiderRecordLink.getLinkedPath().getNameInData(), "linkedPathDataGroup");
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
		assertEquals(dataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(dataRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderRecordLink.setRepeatId("essan");
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(dataRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}

	@Test
	public void testToDataRecordWithLinkedRepeatId(){
		spiderRecordLink.setLinkedRepeatId("one");
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getLinkedRepeatId(), "one");
	}

	@Test
	public void testToDataRecordWithLinkedPath(){
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("linkedPathDataGroup");
		spiderRecordLink.setLinkedPath(spiderDataGroup);
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getLinkedPath().getNameInData(), "linkedPathDataGroup");
	}


	@Test
	public void testFromDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderDataRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testFromDataRecordLinkWithRepeatId() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setRepeatId("roi");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderDataRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
	}

	@Test
	public void testFromDataRecordLinkWithLinkedRepeatId(){
		DataRecordLink dataRecordLink = DataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setLinkedRepeatId("linkedOne");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getLinkedRepeatId(), "linkedOne");
	}

	@Test
	public void testFromDataRecordLinkWithLinkedLinkedPath(){
		DataRecordLink dataRecordLink = DataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setLinkedPath(DataGroup.withNameInData("linkedPath"));
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getLinkedPath().getNameInData(), "linkedPath");
	}
}
