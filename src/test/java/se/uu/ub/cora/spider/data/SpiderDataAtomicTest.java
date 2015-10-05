package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import se.uu.ub.cora.metadataformat.data.DataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;

public class SpiderDataAtomicTest {
	@Test
	public void testInit() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData", "value");
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testFromDataAtomic() {
		DataAtomic dataAtomic = DataAtomic.withNameInDataAndValue("nameInData", "value");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getNameInData(), "nameInData");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testToDataAtomic() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withNameInDataAndValue("nameInData", "value");
		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomic.getNameInData(), "nameInData");
		assertEquals(dataAtomic.getValue(), "value");
	}
}
