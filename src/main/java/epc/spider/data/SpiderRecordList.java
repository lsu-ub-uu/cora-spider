package epc.spider.data;

import java.util.ArrayList;
import java.util.List;

public final class SpiderRecordList {

	private String containRecordsOfType;
	private List<SpiderDataRecord> records = new ArrayList<>();
	private String totalNo;
	private String fromNo;
	private String toNo;

	public static SpiderRecordList withContainRecordsOfType(String containRecordsOfType) {
		return new SpiderRecordList(containRecordsOfType);
	}

	private SpiderRecordList(String containRecordsOfType) {
		this.containRecordsOfType = containRecordsOfType;
	}

	public String getContainRecordsOfType() {
		return containRecordsOfType;
	}

	public void addRecord(SpiderDataRecord record) {
		records.add(record);

	}

	public List<SpiderDataRecord> getRecords() {
		return records;
	}

	public void setTotalNo(String totalNo) {
		this.totalNo = totalNo;
	}

	public String getTotalNumberOfTypeInStorage() {
		return totalNo;
	}

	public void setFromNo(String fromNo) {
		this.fromNo = fromNo;
	}

	public String getFromNo() {
		return fromNo;
	}

	public void setToNo(String toNo) {
		this.toNo = toNo;
	}

	public String getToNo() {
		return toNo;
	}

}
