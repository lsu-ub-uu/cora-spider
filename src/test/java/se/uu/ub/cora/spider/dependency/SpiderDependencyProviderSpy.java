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

import java.lang.reflect.InvocationTargetException;
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
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.RecordTypeHandler;
import se.uu.ub.cora.spider.record.SpiderUploader;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.StreamStorage;

public class SpiderDependencyProviderSpy extends SpiderDependencyProvider {

	public SpiderAuthorizator spiderAuthorizator;
	public PermissionRuleCalculator ruleCalculator;
	public SpiderUploader uploader;
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
	public DataRedactor dataRedactor;
	public RecordTypeHandlerSpy recordTypeHandlerSpy = new RecordTypeHandlerSpy();
	public Map<String, RecordTypeHandlerSpy> mapOfRecordTypeHandlerSpies = new HashMap<>();

	public MethodCallRecorder MCR = new MethodCallRecorder();

	public SpiderDependencyProviderSpy(Map<String, String> initInfo) {
		super(initInfo);
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return spiderAuthorizator;
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return ruleCalculator;
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
	public DataGroupTermCollector getDataGroupTermCollector() {
		MCR.addCall();
		MCR.addReturned(termCollector);
		return termCollector;
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		return recordIndexer;
	}

	@Override
	protected void tryToInitialize() throws Exception {
		tryToInitializeWasCalled = true;
		if (initInfo.containsKey("runtimeException")) {
			throw new RuntimeException(initInfo.get("runtimeException"));
		}
		if (initInfo.containsKey("invocationTargetException")) {
			throw new InvocationTargetException(
					new RuntimeException(initInfo.get("invocationTargetException")));
		}
	}

	@Override
	protected void readInitInfo() {
		readInitInfoWasCalled = true;
	}

	public String getInitInfoFromParent(String key) {
		return initInfo.get(key);
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
		return dataRedactor;
	}

	public void createRecordTypeHandlerSpy(String recordType) {
		mapOfRecordTypeHandlerSpies.put(recordType, new RecordTypeHandlerSpy());
	}
}
