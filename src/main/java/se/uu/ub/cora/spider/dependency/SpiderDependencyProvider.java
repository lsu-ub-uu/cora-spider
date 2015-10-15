package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.metadataformat.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.metadataformat.validator.DataValidator;
import se.uu.ub.cora.spider.record.PermissionKeyCalculator;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public interface SpiderDependencyProvider {

	Authorizator getAuthorizator();

	RecordStorage getRecordStorage();

	RecordIdGenerator getIdGenerator();

	PermissionKeyCalculator getPermissionKeyCalculator();

	DataValidator getDataValidator();

	DataRecordLinkCollector getDataRecordLinkCollector();

}
