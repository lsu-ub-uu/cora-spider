package epc.spider.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;

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
