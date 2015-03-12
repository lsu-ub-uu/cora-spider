package epc.spider.record;

import java.util.HashSet;
import java.util.Set;

import epc.spider.data.SpiderDataGroup;

public class RecordPermissionKeyCalculator implements PermissionKeyCalculator {

	@Override
	public Set<String> calculateKeys(String accessType, String recordType,
			SpiderDataGroup record) {
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(),
				"SYSTEM", "*");
		keys.add(key);
		return keys;
	}

}
