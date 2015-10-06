package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.metadataformat.data.DataAttribute;

public final class SpiderDataAttribute implements SpiderDataElement {

	private String nameInData;
	private String value;

	private SpiderDataAttribute(String nameInData, String value) {
		this.nameInData = nameInData;
		this.value = value;
	}

	public static SpiderDataAttribute fromDataAttribute(DataAttribute dataAttribute) {
		return new SpiderDataAttribute(dataAttribute);
	}

	public static SpiderDataAttribute withNameInDataAndValue(String nameInData, String value) {
		return new SpiderDataAttribute(nameInData, value);
	}

	private SpiderDataAttribute(DataAttribute dataAttribute) {
		nameInData = dataAttribute.getNameInData();
		value = dataAttribute.getValue();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getValue() {
		return value;
	}

	public DataAttribute toDataAttribute() {
		return DataAttribute.withNameInDataAndValue(nameInData, value);
	}

}
