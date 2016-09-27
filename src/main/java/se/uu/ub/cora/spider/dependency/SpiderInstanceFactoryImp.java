/*
 * Copyright 2016 Olov McKie
 * Copyright 2015 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.PermissionKeyCalculator;
import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.SpiderDownloaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordCreatorImp;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordDeleterImp;
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordListReaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordReaderImp;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterImp;
import se.uu.ub.cora.spider.record.SpiderUploader;
import se.uu.ub.cora.spider.record.SpiderUploaderImp;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderInstanceFactoryImp implements SpiderInstanceFactory {

	private SpiderDependencyProvider dependencyProvider;

	private SpiderInstanceFactoryImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	public static SpiderInstanceFactory usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderInstanceFactoryImp(dependencyProvider);
	}

	@Override
	public SpiderRecordReader factorSpiderRecordReader() {
		Authorizator authorizator = dependencyProvider.getAuthorizator();
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = dependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorizator, recordStorage, permissionKeyCalculator);
	}

	@Override
	public SpiderRecordListReader factorSpiderRecordListReader() {
		Authorizator authorizator = dependencyProvider.getAuthorizator();
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = dependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordListReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorizator, recordStorage, permissionKeyCalculator);
	}

	@Override
	public SpiderRecordCreator factorSpiderRecordCreator() {
		Authorizator authorizator = dependencyProvider.getAuthorizator();
		DataValidator dataValidator = dependencyProvider.getDataValidator();
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		RecordIdGenerator recordIdGenerator = dependencyProvider.getIdGenerator();
		PermissionKeyCalculator permissionKeyCalculator = dependencyProvider
				.getPermissionKeyCalculator();
		DataRecordLinkCollector linkCollector = dependencyProvider.getDataRecordLinkCollector();
		ExtendedFunctionalityProvider extendedFunctionalityProvider = dependencyProvider
				.getExtendedFunctionalityProvider();

		return SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollectorAndExtendedFunctionalityProvider(
						authorizator, dataValidator, recordStorage, recordIdGenerator,
						permissionKeyCalculator, linkCollector, extendedFunctionalityProvider);
	}

	@Override
	public SpiderRecordUpdater factorSpiderRecordUpdater() {
		Authorizator authorizator = dependencyProvider.getAuthorizator();
		DataValidator dataValidator = dependencyProvider.getDataValidator();
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = dependencyProvider
				.getPermissionKeyCalculator();
		DataRecordLinkCollector linkCollector = dependencyProvider.getDataRecordLinkCollector();
		ExtendedFunctionalityProvider extendedFunctionalityProvider = dependencyProvider
				.getExtendedFunctionalityProvider();

		return SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollectorAndExtendedFunctionalityProvider(
						authorizator, dataValidator, recordStorage, permissionKeyCalculator,
						linkCollector, extendedFunctionalityProvider);
	}

	@Override
	public SpiderRecordDeleter factorSpiderRecordDeleter() {
		Authorizator authorizator = dependencyProvider.getAuthorizator();
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = dependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordDeleterImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorizator, recordStorage, permissionKeyCalculator);
	}

	@Override
	public SpiderUploader factorSpiderUploader() {
		return SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Override
	public SpiderDownloader factorSpiderDownloader() {
		return SpiderDownloaderImp.usingDependencyProvider(dependencyProvider);
	}

}
