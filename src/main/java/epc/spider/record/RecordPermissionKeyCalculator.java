package epc.spider.record;

import java.util.HashSet;
import java.util.Set;

import epc.metadataformat.data.DataGroup;

public class RecordPermissionKeyCalculator implements PermissionKeyCalculator {

	@Override
	public Set<String> calculateKeys(String accessType, String recordType, DataGroup record) {
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

	@Override
	public Set<String> calculateKeysForList(String accessType, String recordType) {
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

}
