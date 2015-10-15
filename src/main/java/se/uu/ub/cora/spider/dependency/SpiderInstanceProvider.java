package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.metadataformat.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.metadataformat.validator.DataValidator;
import se.uu.ub.cora.spider.record.PermissionKeyCalculator;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordCreatorImp;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordDeleterImp;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordReaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterImp;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderInstanceProvider {
	private static SpiderDependencyProvider spiderDependencyProvider;

	private SpiderInstanceProvider() {
		// not called
		throw new UnsupportedOperationException();
	}

	public static void setSpiderDependencyProvider(
			SpiderDependencyProvider spiderDependencyProvider) {
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
		DataRecordLinkCollector linkCollector = spiderDependencyProvider
				.getDataRecordLinkCollector();

		return SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculator(
						authorizator, dataValidator, recordStorage, recordIdGenerator,
						permissionKeyCalculator, linkCollector);
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
