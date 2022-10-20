/*
 * Copyright 2015, 2021, 2022 Uppsala University Library
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

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public interface SpiderDependencyProvider {

	RecordStorage getRecordStorage();

	RecordArchive getRecordArchive();

	StreamStorage getStreamStorage();

	RecordIdGenerator getRecordIdGenerator();

	SpiderAuthorizator getSpiderAuthorizator();

	DataValidator getDataValidator();

	DataRecordLinkCollector getDataRecordLinkCollector();

	DataGroupTermCollector getDataGroupTermCollector();

	PermissionRuleCalculator getPermissionRuleCalculator();

	DataRedactor getDataRedactor();

	ExtendedFunctionalityProvider getExtendedFunctionalityProvider();

	Authenticator getAuthenticator();

	RecordSearch getRecordSearch();

	RecordIndexer getRecordIndexer();

	RecordTypeHandler getRecordTypeHandler(String recordTypeId);

	DataGroupToRecordEnhancer getDataGroupToRecordEnhancer();

	String getInitInfoValueUsingKey(String key);

}