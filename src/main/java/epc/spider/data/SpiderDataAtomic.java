package epc.spider.data;

import epc.metadataformat.data.DataAtomic;

public final class SpiderDataAtomic implements SpiderDataElement {

	private String nameInData;
	private String value;

	private SpiderDataAtomic(String nameInData, String value) {
		this.nameInData = nameInData;
		this.value = value;
	}

	public static SpiderDataAtomic fromDataAtomic(DataAtomic dataAtomic) {
		return new SpiderDataAtomic(dataAtomic);
	}

	public static SpiderDataAtomic withNameInDataAndValue(String nameInData, String value) {
		return new SpiderDataAtomic(nameInData, value);
	}

	private SpiderDataAtomic(DataAtomic dataAtomic) {
		nameInData = dataAtomic.getNameInData();
		value = dataAtomic.getValue();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getValue() {
		return value;
	}

	public DataAtomic toDataAtomic() {
		return DataAtomic.withNameInDataAndValue(nameInData, value);
	}

}
