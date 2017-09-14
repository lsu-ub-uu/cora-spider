/*
 * Copyright 2015, 2016 Uppsala University Library
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
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public abstract class SpiderDependencyProvider {
	protected Map<String, String> initInfo;

	public SpiderDependencyProvider(Map<String, String> initInfo) {
		this.initInfo = initInfo;
	}

	public abstract SpiderAuthorizator getSpiderAuthorizator();

	public abstract RecordStorage getRecordStorage();

	public abstract RecordIdGenerator getIdGenerator();

	public abstract PermissionRuleCalculator getPermissionRuleCalculator();

	public abstract DataValidator getDataValidator();

	public abstract DataRecordLinkCollector getDataRecordLinkCollector();

	public abstract ExtendedFunctionalityProvider getExtendedFunctionalityProvider();

	public abstract StreamStorage getStreamStorage();

	public abstract Authenticator getAuthenticator();

	public abstract RecordSearch getRecordSearch();

	public abstract DataGroupTermCollector getDataGroupSearchTermCollector();

	public abstract RecordIndexer getRecordIndexer();
}
