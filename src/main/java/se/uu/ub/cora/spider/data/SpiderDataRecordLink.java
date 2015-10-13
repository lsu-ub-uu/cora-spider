package se.uu.ub.cora.spider.data;

import java.util.HashSet;
import java.util.Set;

public class SpiderDataRecordLink {

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
}
