package epc.spider.dependency;

import epc.beefeater.Authorizator;
import epc.spider.record.PermissionKeyCalculator;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;

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

}
