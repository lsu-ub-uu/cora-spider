/*
 * Copyright 2015, 2023 Uppsala University Library
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
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.DataRedactorOldSpy;
import se.uu.ub.cora.spider.record.RecordDecorator;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SpiderDependencyProviderOldSpy implements SpiderDependencyProvider {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

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
	public DataRedactorOldSpy dataRedactor = new DataRedactorOldSpy();
	public RecordTypeHandlerOldSpy recordTypeHandlerSpy = new RecordTypeHandlerOldSpy();
	public Map<String, RecordTypeHandlerOldSpy> mapOfRecordTypeHandlerSpies = new HashMap<>();

	public RecordStorage recordStorage = new RecordStorageSpy();
	public RecordIdGenerator recordIdGenerator;
	public RecordArchive recordArchive;

	public SpiderDependencyProviderOldSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getRecordTypeHandlerUsingDataRecordGroup",
				() -> recordTypeHandlerSpy);
		MRV.setDefaultReturnValuesSupplier("getInitInfoValueUsingKey", () -> "someInitValue");
		MRV.setDefaultReturnValuesSupplier("getUniqueValidator", UniqueValidatorSpy::new);
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

	@Deprecated
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
	public RecordTypeHandler getRecordTypeHandlerUsingDataRecordGroup(
			DataRecordGroup dataRecordGroup) {
		return (RecordTypeHandler) MCR.addCallAndReturnFromMRV("dataRecordGroup", dataRecordGroup);
	}

	@Override
	public DataRedactor getDataRedactor() {
		MCR.addCall();
		MCR.addReturned(dataRedactor);
		return dataRedactor;
	}

	public RecordTypeHandlerOldSpy createRecordTypeHandlerSpy(String recordType) {
		RecordTypeHandlerOldSpy newRecordTypeHandlerSpy = new RecordTypeHandlerOldSpy();
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
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return null;
	}

	@Override
	public RecordArchive getRecordArchive() {
		return recordArchive;
	}

	@Override
	public DataGroupToFilter getDataGroupToFilterConverter() {
		MCR.addCall();
		DataGroupToFilter dataGroupToFilter = new DataGroupToFilterSpy();
		MCR.addReturned(dataGroupToFilter);
		return dataGroupToFilter;
	}

	@Override
	public ResourceArchive getResourceArchive() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UniqueValidator getUniqueValidator(RecordStorage recordStorage) {
		return (UniqueValidator) MCR.addCallAndReturnFromMRV("recordStorage", recordStorage);
	}

	@Override
	public DataChangedSender getDataChangeSender() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataDecarator getDataDecorator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RecordDecorator getRecordDecorator() {
		// TODO Auto-generated method stub
		return null;
	}
}
