package epc.spider.data;

import java.util.HashSet;
import java.util.Set;

import epc.metadataformat.data.DataRecord;

public final class SpiderDataRecord {
	private Set<String> keys = new HashSet<>();
	private SpiderDataGroup spiderDataGroup;
	private Set<Action> actions = new HashSet<>();

	public static SpiderDataRecord withSpiderDataGroup(SpiderDataGroup spiderDataGroup) {
		return new SpiderDataRecord(spiderDataGroup);
	}

	private SpiderDataRecord(SpiderDataGroup spiderDataGroup) {
		this.spiderDataGroup = spiderDataGroup;
	}

	public static SpiderDataRecord fromDataRecord(DataRecord dataRecord) {
		return new SpiderDataRecord(dataRecord);
	}

	private SpiderDataRecord(DataRecord dataRecord) {
		keys.addAll(dataRecord.getKeys());
		spiderDataGroup = SpiderDataGroup.fromDataGroup(dataRecord.getDataGroup());
	}

	public void addKey(String key) {
		keys.add(key);
	}

	public boolean containsKey(String key) {
		return keys.contains(key);
	}

	public Set<String> getKeys() {
		return keys;
	}

	public void setSpiderDataGroup(SpiderDataGroup spiderDataGroup) {
		this.spiderDataGroup = spiderDataGroup;

	}

	public SpiderDataGroup getSpiderDataGroup() {
		return spiderDataGroup;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public Set<Action> getActions() {
		return actions;
	}

	public DataRecord toDataRecord() {
		DataRecord dataRecord = new DataRecord();
		dataRecord.setDataGroup(spiderDataGroup.toDataGroup());
		dataRecord.getKeys().addAll(keys);
		return dataRecord;
	}
}
