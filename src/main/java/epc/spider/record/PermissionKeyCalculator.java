package epc.spider.record;

import java.util.Set;

import epc.metadataformat.data.DataGroup;

public interface PermissionKeyCalculator {

	Set<String> calculateKeys(String accessType, String recordType,
			DataGroup record);

}
