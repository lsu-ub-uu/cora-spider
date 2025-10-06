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

import se.uu.ub.cora.bookkeeper.decorator.DataDecarator;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordDecorator;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public interface SpiderDependencyProvider {

	RecordStorage getRecordStorage();

	RecordArchive getRecordArchive();

	StreamStorage getStreamStorage();

	@Deprecated
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

	RecordTypeHandler getRecordTypeHandlerUsingDataRecordGroup(DataRecordGroup dataRecordGroup);

	DataGroupToRecordEnhancer getDataGroupToRecordEnhancer();

	/**
	 * getDataGroupToFilterConverter creates a new {@link DataGroupToFilter} converter for each call
	 * to this method.
	 * 
	 * @return A new {@link DataGroupToFilter} converter
	 */
	DataGroupToFilter getDataGroupToFilterConverter();

	ResourceArchive getResourceArchive();

	/**
	 * getUniqueValidator method returns a new instance of {@link UniqueValidator}. It needs a
	 * record storage in order to create a new instance
	 * 
	 * @param recordStorage
	 *            is the {@link RecordStorage} that UniqueValidator usues to access the storage.
	 * 
	 * @return A new instance of {@link UniqueValidator}
	 */
	UniqueValidator getUniqueValidator(RecordStorage recordStorage);

	/**
	 * getDataChangeSender method returns a new instance of {@link DataChangedSender}
	 * 
	 * @return A new instance of {@link DataChangedSender}
	 */
	DataChangedSender getDataChangeSender();

	/**
	 * getDataDecorator method returns a new instance of {@link DataDecarator}
	 * 
	 * @return A new instance of {@link DataDecarator}
	 */
	DataDecarator getDataDecorator();

	/**
	 * getRecordDecorator method returns a new instance of {@link RecordDecorator}
	 * 
	 * @return A new instance of {@link RecordDecorator}
	 */
	RecordDecorator getRecordDecorator();

}