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

package se.uu.ub.cora.spider.dependency.spy;

import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.spy.RecordStorageMCRSpy;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class SpiderDependencyProviderOldSpy implements SpiderDependencyProvider {

	public SpiderAuthorizator spiderAuthorizator;
	public PermissionRuleCalculator ruleCalculator;
	public Uploader uploader;
	public DataValidator dataValidator;
	public DataRecordLinkCollector linkCollector;
	public RecordIdGenerator idGenerator;
	public StreamStorage streamStorage;
	public ExtendedFunctionalityProvider extendedFunctionalityProvider;
	public Authenticator authenticator;
	public RecordSearch recordSearch;
	public DataGroupTermCollector termCollector;
	public RecordIndexer recordIndexer;
	public boolean readInitInfoWasCalled;
	public boolean tryToInitializeWasCalled;
	public DataRedactorSpy dataRedactor = new DataRedactorSpy();
	public RecordTypeHandlerSpy recordTypeHandlerSpy = new RecordTypeHandlerSpy();
	public Map<String, RecordTypeHandlerSpy> mapOfRecordTypeHandlerSpies = new HashMap<>();

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public RecordStorage recordStorage = new RecordStorageMCRSpy();
	private RecordStorageProviderSpy recordStorageProvider;
	public RecordIdGenerator recordIdGenerator;
	public RecordArchive recordArchive;

	// TODO: remove?
	public SpiderDependencyProviderOldSpy(Map<String, String> initInfo) {
		// super(initInfo);
		recordStorageProvider = new RecordStorageProviderSpy();
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		MCR.addCall();
		MCR.addReturned(spiderAuthorizator);
		return spiderAuthorizator;
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		MCR.addCall();
		MCR.addReturned(ruleCalculator);
		return ruleCalculator;
	}

	@Override
	public DataValidator getDataValidator() {
		MCR.addCall();
		MCR.addReturned(dataValidator);
		return dataValidator;
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		MCR.addCall();
		MCR.addReturned(linkCollector);
		return linkCollector;
	}

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		MCR.addCall();
		MCR.addReturned(extendedFunctionalityProvider);
		return extendedFunctionalityProvider;
	}

	@Override
	public Authenticator getAuthenticator() {
		MCR.addCall();
		MCR.addReturned(authenticator);
		return authenticator;
	}

	@Override
	public RecordSearch getRecordSearch() {
		MCR.addCall();
		MCR.addReturned(recordSearch);
		return recordSearch;
	}

	@Override
	public DataGroupTermCollector getDataGroupTermCollector() {
		MCR.addCall();
		MCR.addReturned(termCollector);
		return termCollector;
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		MCR.addCall();
		MCR.addReturned(recordIndexer);
		return recordIndexer;
	}

	@Override
	public RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
		MCR.addCall("recordTypeId", recordTypeId);
		RecordTypeHandler recordTypeHandlerSpyToReturn = recordTypeHandlerSpy;
		if (mapOfRecordTypeHandlerSpies.containsKey(recordTypeId)) {
			recordTypeHandlerSpyToReturn = mapOfRecordTypeHandlerSpies.get(recordTypeId);
		}
		MCR.addReturned(recordTypeHandlerSpyToReturn);
		return recordTypeHandlerSpyToReturn;
	}

	@Override
	public DataRedactor getDataRedactor() {
		MCR.addCall();
		MCR.addReturned(dataRedactor);
		return dataRedactor;
	}

	public RecordTypeHandlerSpy createRecordTypeHandlerSpy(String recordType) {
		RecordTypeHandlerSpy newRecordTypeHandlerSpy = new RecordTypeHandlerSpy();
		mapOfRecordTypeHandlerSpies.put(recordType, newRecordTypeHandlerSpy);
		return newRecordTypeHandlerSpy;
	}

	@Override
	public RecordStorage getRecordStorage() {
		MCR.addCall();
		MCR.addReturned(recordStorage);
		return recordStorage;
	}

	@Override
	public StreamStorage getStreamStorage() {
		return streamStorage;
	}

	@Override
	public RecordIdGenerator getRecordIdGenerator() {
		MCR.addCall();
		MCR.addReturned(recordIdGenerator);
		return recordIdGenerator;
	}

	@Override
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return null;
	}

	@Override
	public String getInitInfoValueUsingKey(String key) {
		return null;
	}

	@Override
	public RecordArchive getRecordArchive() {
		return recordArchive;
	}

}
