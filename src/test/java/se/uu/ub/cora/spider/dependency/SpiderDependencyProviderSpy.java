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

import java.util.Map;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.searchtermcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.RecordSearch;
import se.uu.ub.cora.spider.record.SpiderUploader;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public class SpiderDependencyProviderSpy extends SpiderDependencyProvider {

	public RecordStorage recordStorage;
	public SpiderAuthorizator spiderAuthorizator;
	public PermissionRuleCalculator keyCalculator;
	public SpiderUploader uploader;
	public DataValidator dataValidator;
	public DataRecordLinkCollector linkCollector;
	public RecordIdGenerator idGenerator;
	public StreamStorage streamStorage;
	public ExtendedFunctionalityProvider extendedFunctionalityProvider;
	public Authenticator authenticator;
	public RecordSearch recordSearch;
	public DataGroupTermCollector searchTermCollector;
	public RecordIndexer recordIndexer;

	public SpiderDependencyProviderSpy(Map<String, String> initInfo) {
		super(initInfo);
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return spiderAuthorizator;
	}

	@Override
	public RecordStorage getRecordStorage() {
		return recordStorage;
	}

	@Override
	public RecordIdGenerator getIdGenerator() {
		return idGenerator;
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return keyCalculator;
	}

	@Override
	public DataValidator getDataValidator() {
		return dataValidator;
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return linkCollector;
	}

	@Override
	public StreamStorage getStreamStorage() {
		return streamStorage;
	}

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return extendedFunctionalityProvider;
	}

	@Override
	public Authenticator getAuthenticator() {
		return authenticator;
	}

	@Override
	public RecordSearch getRecordSearch() {
		return recordSearch;
	}

	@Override
	public DataGroupTermCollector getDataGroupSearchTermCollector() {
		return searchTermCollector;
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		return recordIndexer;
	}
}
