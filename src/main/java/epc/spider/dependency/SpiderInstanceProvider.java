package epc.spider.dependency;

import epc.beefeater.Authorizator;
import epc.metadataformat.validator.DataValidator;
import epc.spider.record.PermissionKeyCalculator;
import epc.spider.record.SpiderRecordHandler;
import epc.spider.record.SpiderRecordHandlerImp;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;

public final class SpiderInstanceProvider {
	private static SpiderDependencyProvider spiderDependencyProvider;

	private SpiderInstanceProvider() {
		// not called
		throw new UnsupportedOperationException();
	}

	public static void setSpiderDependencyProvider(SpiderDependencyProvider spiderDependencyProvider) {
		SpiderInstanceProvider.spiderDependencyProvider = spiderDependencyProvider;
	}

	public static SpiderRecordHandler getSpiderRecordHandler() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		DataValidator dataValidator = spiderDependencyProvider.getDataValidator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		RecordIdGenerator recordIdGenerator = spiderDependencyProvider.getIdGenerator();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordHandlerImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculator(
						authorizator, dataValidator, recordStorage, recordIdGenerator,
						permissionKeyCalculator);
	}

}
