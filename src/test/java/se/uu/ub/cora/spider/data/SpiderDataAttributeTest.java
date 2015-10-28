package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAttribute;
import se.uu.ub.cora.spider.data.SpiderDataAttribute;

public class SpiderDataAttributeTest {
	@Test
	public void testInit() {
		SpiderDataAttribute spiderDataAttribute = SpiderDataAttribute.withNameInDataAndValue("nameInData", "value");
		assertEquals(spiderDataAttribute.getNameInData(), "nameInData");
		assertEquals(spiderDataAttribute.getValue(), "value");
	}

	@Test
	public void testFromDataAttribute() {
		DataAttribute dataAttribute = DataAttribute.withNameInDataAndValue("nameInData", "value");
		SpiderDataAttribute spiderDataAttribute = SpiderDataAttribute.fromDataAttribute(dataAttribute);
		assertEquals(spiderDataAttribute.getNameInData(), "nameInData");
		assertEquals(spiderDataAttribute.getValue(), "value");
	}

	@Test
	public void testToDataAttribute() {
		SpiderDataAttribute spiderDataAttribute = SpiderDataAttribute.withNameInDataAndValue("nameInData", "value");
		DataAttribute dataAttribute = spiderDataAttribute.toDataAttribute();
		assertEquals(dataAttribute.getNameInData(), "nameInData");
		assertEquals(dataAttribute.getValue(), "value");
	}
}
