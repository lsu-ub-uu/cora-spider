package epc.spider.data;

import epc.metadataformat.data.DataAtomic;

public final class SpiderDataAtomic implements SpiderDataElement {

	private String dataId;
	private String value;

	private SpiderDataAtomic(String dataId, String value) {
		this.dataId = dataId;
		this.value = value;
	}

	public static SpiderDataAtomic fromDataAtomic(DataAtomic dataAtomic) {
		return new SpiderDataAtomic(dataAtomic);
	}

	public static SpiderDataAtomic withDataIdAndValue(String dataId, String value) {
		return new SpiderDataAtomic(dataId, value);
	}

	private SpiderDataAtomic(DataAtomic dataAtomic) {
		dataId = dataAtomic.getDataId();
		value = dataAtomic.getValue();
	}

	@Override
	public String getDataId() {
		return dataId;
	}

	public String getValue() {
		return value;
	}

	public DataAtomic toDataAtomic() {
		return DataAtomic.withDataIdAndValue(dataId, value);
	}

}
