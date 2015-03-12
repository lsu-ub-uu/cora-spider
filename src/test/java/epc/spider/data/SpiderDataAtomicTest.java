package epc.spider.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;

public class SpiderDataAtomicTest {
	@Test
	public void testInit() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withDataIdAndValue("dataId", "value");
		assertEquals(spiderDataAtomic.getDataId(), "dataId");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testFromDataAtomic() {
		DataAtomic dataAtomic = DataAtomic.withDataIdAndValue("dataId", "value");
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.fromDataAtomic(dataAtomic);
		assertEquals(spiderDataAtomic.getDataId(), "dataId");
		assertEquals(spiderDataAtomic.getValue(), "value");
	}

	@Test
	public void testToDataAtomic() {
		SpiderDataAtomic spiderDataAtomic = SpiderDataAtomic.withDataIdAndValue("dataId", "value");
		DataAtomic dataAtomic = spiderDataAtomic.toDataAtomic();
		assertEquals(dataAtomic.getDataId(), "dataId");
		assertEquals(dataAtomic.getValue(), "value");
	}
}
