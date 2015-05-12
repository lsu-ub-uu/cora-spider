package epc.spider.dependency;

import epc.beefeater.Authorizator;
import epc.spider.record.PermissionKeyCalculator;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;

public interface SpiderDependencyProvider {

	Authorizator getAuthorizator();

	RecordStorage getRecordStorage();

	RecordIdGenerator getIdGenerator();

	PermissionKeyCalculator getPermissionKeyCalculator();

}
