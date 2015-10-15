package se.uu.ub.cora.spider.record;

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.metadataformat.data.DataGroup;

public class KeyCalculatorSpy implements PermissionKeyCalculator {

	public boolean calculateKeysWasCalled = false;

	@Override
	public Set<String> calculateKeys(String accessType, String recordType, DataGroup record) {
		calculateKeysWasCalled = true;
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

	@Override
	public Set<String> calculateKeysForList(String accessType, String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

}
