package epc.spider.record.storage;

public class TimeStampIdGenerator implements RecordIdGenerator {

	@Override
	public String getIdForType(String type) {
		return type + ":" + System.nanoTime();
	}
}
