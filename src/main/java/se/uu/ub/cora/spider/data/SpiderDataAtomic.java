package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.metadataformat.data.DataAtomic;

public final class SpiderDataAtomic implements SpiderDataElement {

	private String nameInData;
	private String value;
	private String repeatId;

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
		repeatId = dataAtomic.getRepeatId();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getValue() {
		return value;
	}

	public DataAtomic toDataAtomic() {
		DataAtomic dataAtomic = DataAtomic.withNameInDataAndValue(nameInData, value);
		dataAtomic.setRepeatId(repeatId);
		return dataAtomic;
	}

	public void setRepeatId(String repeatId) {
		this.repeatId = repeatId;
	}

	public String getRepeatId() {
		return repeatId;
	}

}
