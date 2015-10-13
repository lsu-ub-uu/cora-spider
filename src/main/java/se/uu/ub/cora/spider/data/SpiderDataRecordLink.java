package se.uu.ub.cora.spider.data;

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public class SpiderDataRecordLink implements SpiderDataElement {

	private String nameInData;
	private String recordType;
	private String recordId;
	private Set<Action> actions = new HashSet<>();

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
		this.nameInData = dataRecordLink.getNameInData();
		this.recordType = dataRecordLink.getRecordType();
		this.recordId = dataRecordLink.getRecordId();
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
		return DataRecordLink.withNameInDataAndRecordTypeAndRecordId(nameInData, recordType,
				recordId);
	}

	public static SpiderDataRecordLink fromDataRecordLink(DataRecordLink dataRecordLink) {
		return new SpiderDataRecordLink(dataRecordLink);
	}

}
