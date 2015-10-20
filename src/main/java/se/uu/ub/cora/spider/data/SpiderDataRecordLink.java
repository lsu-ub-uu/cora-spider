package se.uu.ub.cora.spider.data;

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public final class SpiderDataRecordLink implements SpiderDataElement {

	private String nameInData;
	private String recordType;
	private String recordId;
	private Set<Action> actions = new HashSet<>();
	private String repeatId;

	public static SpiderDataRecordLink withNameInDataAndRecordTypeAndRecordId(String nameInData,
			String recordType, String recordId) {
		return new SpiderDataRecordLink(nameInData, recordType, recordId);
	}

	private SpiderDataRecordLink(String nameInData, String recordType, String recordId) {
		this.nameInData = nameInData;
		this.recordType = recordType;
		this.recordId = recordId;
	}

	private SpiderDataRecordLink(DataRecordLink dataRecordLink) {
		nameInData = dataRecordLink.getNameInData();
		recordType = dataRecordLink.getRecordType();
		recordId = dataRecordLink.getRecordId();
		repeatId = dataRecordLink.getRepeatId();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getRecordType() {
		return recordType;
	}

	public String getRecordId() {
		return recordId;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public Set<Action> getActions() {
		return actions;
	}

	public DataRecordLink toDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndRecordTypeAndRecordId(nameInData, recordType, recordId);
		dataRecordLink.setRepeatId(repeatId);
		return dataRecordLink;
	}

	public static SpiderDataRecordLink fromDataRecordLink(DataRecordLink dataRecordLink) {
		return new SpiderDataRecordLink(dataRecordLink);
	}

	public void setRepeatId(String repeatId) {
		this.repeatId = repeatId;
	}

	public String getRepeatId() {
		return repeatId;
	}

}
