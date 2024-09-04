/*
 * Copyright 2022, 2023 Uppsala University Library
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
package se.uu.ub.cora.spider.spy;

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
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.DataGroupToFilterSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.OldStreamStorageSpy;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.OldRecordSearchSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SpiderDependencyProviderSpy implements SpiderDependencyProvider {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public SpiderDependencyProviderSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getRecordStorage", RecordStorageSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordArchive", RecordArchiveSpy::new);
		MRV.setDefaultReturnValuesSupplier("getResourceArchive", ResourceArchiveSpy::new);
		MRV.setDefaultReturnValuesSupplier("getStreamStorage", OldStreamStorageSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordIdGenerator", IdGeneratorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator", SpiderAuthorizatorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataValidator", DataValidatorOldSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataRecordLinkCollector",
				DataRecordLinkCollectorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				DataGroupTermCollectorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getPermissionRuleCalculator", RuleCalculatorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataRedactor", DataRedactorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				ExtendedFunctionalityProviderSpy::new);
		MRV.setDefaultReturnValuesSupplier("getAuthenticator", AuthenticatorSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordSearch", OldRecordSearchSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordIndexer", RecordIndexerSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler", RecordTypeHandlerSpy::new);
		MRV.setDefaultReturnValuesSupplier("getRecordTypeHandlerUsingDataRecordGroup",
				RecordTypeHandlerSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataGroupToRecordEnhancer",
				DataGroupToRecordEnhancerSpy::new);
		MRV.setDefaultReturnValuesSupplier("getDataGroupToFilterConverter",
				DataGroupToFilterSpy::new);
		MRV.setDefaultReturnValuesSupplier("getUniqueValidator", UniqueValidatorSpy::new);
	}

	@Override
	public RecordStorage getRecordStorage() {
		return (RecordStorage) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordArchive getRecordArchive() {
		return (RecordArchive) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public ResourceArchive getResourceArchive() {
		return (ResourceArchive) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public StreamStorage getStreamStorage() {
		return (StreamStorage) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordIdGenerator getRecordIdGenerator() {
		return (RecordIdGenerator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return (SpiderAuthorizator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public DataValidator getDataValidator() {
		return (DataValidator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return (DataRecordLinkCollector) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public DataGroupTermCollector getDataGroupTermCollector() {
		return (DataGroupTermCollector) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return (PermissionRuleCalculator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public DataRedactor getDataRedactor() {
		return (DataRedactor) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return (ExtendedFunctionalityProvider) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Authenticator getAuthenticator() {
		return (Authenticator) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordSearch getRecordSearch() {
		return (RecordSearch) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		return (RecordIndexer) MCR.addCallAndReturnFromMRV();
	}

	@Deprecated
	@Override
	public RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
		return (RecordTypeHandler) MCR.addCallAndReturnFromMRV("recordTypeId", recordTypeId);
	}

	@Override
	public RecordTypeHandler getRecordTypeHandlerUsingDataRecordGroup(
			DataRecordGroup dataRecordGroup) {
		return (RecordTypeHandler) MCR.addCallAndReturnFromMRV("dataRecordGroup", dataRecordGroup);
	}

	@Override
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return (DataGroupToRecordEnhancer) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public DataGroupToFilter getDataGroupToFilterConverter() {
		return (DataGroupToFilter) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public UniqueValidator getUniqueValidator(RecordStorage recordStorage) {
		return (UniqueValidator) MCR.addCallAndReturnFromMRV("recordStorage", recordStorage);
	}
}
