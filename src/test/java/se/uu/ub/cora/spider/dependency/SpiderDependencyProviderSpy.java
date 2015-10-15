package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.metadataformat.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.metadataformat.validator.DataValidator;
import se.uu.ub.cora.spider.record.PermissionKeyCalculator;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderDependencyProviderSpy implements SpiderDependencyProvider {

	@Override
	public Authorizator getAuthorizator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RecordStorage getRecordStorage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RecordIdGenerator getIdGenerator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PermissionKeyCalculator getPermissionKeyCalculator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataValidator getDataValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		// TODO Auto-generated method stub
		return null;
	}

}
