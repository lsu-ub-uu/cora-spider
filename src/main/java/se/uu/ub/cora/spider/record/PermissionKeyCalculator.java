package se.uu.ub.cora.spider.record;

import java.util.Set;

import se.uu.ub.cora.metadataformat.data.DataGroup;

public interface PermissionKeyCalculator {

	Set<String> calculateKeys(String accessType, String recordType, DataGroup record);

	Set<String> calculateKeysForList(String accessType, String recordType);

}
