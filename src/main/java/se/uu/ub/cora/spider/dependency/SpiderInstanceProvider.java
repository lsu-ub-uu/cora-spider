/*
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
import se.uu.ub.cora.spider.record.*;
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

	public static SpiderRecordListReader getSpiderRecordListReader() {
		Authorizator authorizator = spiderDependencyProvider.getAuthorizator();
		RecordStorage recordStorage = spiderDependencyProvider.getRecordStorage();
		PermissionKeyCalculator permissionKeyCalculator = spiderDependencyProvider
				.getPermissionKeyCalculator();

		return SpiderRecordListReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
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
