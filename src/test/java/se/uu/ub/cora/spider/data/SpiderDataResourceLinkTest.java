package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class SpiderDataResourceLinkTest {

	SpiderDataResourceLink spiderResourceLink;

	@BeforeMethod
	public void setUp() {
		spiderResourceLink = SpiderDataResourceLink.withNameInData("nameInData");

		SpiderDataAtomic streamId = SpiderDataAtomic.withNameInDataAndValue("streamId",
				"myStreamId");
		spiderResourceLink.addChild(streamId);

	}

	@Test
	public void testInit() {
		assertEquals(spiderResourceLink.getNameInData(), "nameInData");
		assertNotNull(spiderResourceLink.getAttributes());
		assertNotNull(spiderResourceLink.getChildren());
		assertEquals(spiderResourceLink.extractAtomicValue("streamId"), "myStreamId");
		assertNotNull(spiderResourceLink.getActions());
	}

	@Test
	public void testInitWithRepeatId() {
		spiderResourceLink.setRepeatId("hugh");
		assertEquals(spiderResourceLink.getRepeatId(), "hugh");
	}

	@Test
	public void testAddAction() {
		spiderResourceLink.addAction(Action.READ);

		assertTrue(spiderResourceLink.getActions().contains(Action.READ));
		assertFalse(spiderResourceLink.getActions().contains(Action.DELETE));
		// small hack to get 100% coverage on enum
		Action.valueOf(Action.READ.toString());
	}

	@Test
	public void testToDataRecordLink() {
		DataGroup dataRecordLink = spiderResourceLink.toDataGroup();

		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("streamId"), "myStreamId");
	}

	@Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderResourceLink.setRepeatId("essan");
		DataGroup dataRecordLink = spiderResourceLink.toDataGroup();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("streamId"), "myStreamId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}

	@Test
	public void testFromDataRecordLink() {
		DataGroup dataRecordLink = createResourceLinkAsDataGroup();

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);

		assertCorrectFromDataResourceLink(spiderDataRecordLink);
	}

	private DataGroup createResourceLinkAsDataGroup() {
		DataGroup dataRecordLink = DataGroup.withNameInData("nameInData");

		DataAtomic streamId = DataAtomic.withNameInDataAndValue("streamId", "myStreamId");
		dataRecordLink.addChild(streamId);
		return dataRecordLink;
	}

	private void assertCorrectFromDataResourceLink(SpiderDataRecordLink spiderDataRecordLink) {
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");

		SpiderDataAtomic convertedRecordId = (SpiderDataAtomic) spiderDataRecordLink
				.getFirstChildWithNameInData("streamId");
		assertEquals(convertedRecordId.getValue(), "myStreamId");
	}

	@Test
	public void testFromDataRecordLinkWithRepeatId() {
		DataGroup dataRecordLink = createResourceLinkAsDataGroup();
		dataRecordLink.setRepeatId("roi");

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertCorrectFromDataResourceLinkWithRepeatId(spiderDataRecordLink);
	}

	private void assertCorrectFromDataResourceLinkWithRepeatId(
			SpiderDataRecordLink spiderDataRecordLink) {
		assertCorrectFromDataResourceLink(spiderDataRecordLink);
		assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
	}
}
