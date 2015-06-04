package epc.spider.dependency;

import epc.beefeater.Authorizator;
import epc.metadataformat.validator.DataValidator;
import epc.spider.record.PermissionKeyCalculator;
import epc.spider.record.SpiderRecordCreator;
import epc.spider.record.SpiderRecordCreatorImp;
import epc.spider.record.SpiderRecordDeleter;
import epc.spider.record.SpiderRecordDeleterImp;
import epc.spider.record.SpiderRecordReader;
import epc.spider.record.SpiderRecordReaderImp;
import epc.spider.record.SpiderRecordUpdater;
import epc.spider.record.SpiderRecordUpdaterImp;
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

	public static SpiderRecordReader getSpiderRecordReader() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorizator, recordStorage, permissionKeyCalculator);
	}

	public static SpiderRecordCreator getSpiderRecordCreator() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		DataValidator dataValidator = spiderDependencyProvider.getDataValidator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		RecordIdGenerator recordIdGenerator = spiderDependencyProvider.getIdGenerator();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculator(
						authorizator, dataValidator, recordStorage, recordIdGenerator,
						permissionKeyCalculator);
	}

	public static SpiderRecordUpdater getSpiderRecordUpdater() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		DataValidator dataValidator = spiderDependencyProvider.getDataValidator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorizator,
						dataValidator, recordStorage, permissionKeyCalculator);
	}

	public static SpiderRecordDeleter getSpiderRecordDeleter() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordDeleterImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorizator, recordStorage, permissionKeyCalculator);
	}

}
