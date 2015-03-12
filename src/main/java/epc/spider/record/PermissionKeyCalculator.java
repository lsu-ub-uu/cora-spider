package epc.spider.record;

import java.util.Set;

import epc.spider.data.SpiderDataGroup;

public interface PermissionKeyCalculator {

	Set<String> calculateKeys(String accessType, String recordType,
			SpiderDataGroup record);

}
