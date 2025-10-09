/*
 * Copyright 2015, 2016, 2019, 2020, 2023, 2024, 2025 Uppsala University Library
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

import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.decorator.DataChildDecoratorFactoryImp;
import se.uu.ub.cora.bookkeeper.decorator.DataDecarator;
import se.uu.ub.cora.bookkeeper.decorator.DataDecoratorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollectorImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorFactory;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorFactoryImp;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactory;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollectorImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactory;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactoryImp;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.BasePermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.authorization.internal.SpiderAuthorizatorImp;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.cache.DataChangedSenderImp;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.data.internal.DataGroupToFilterImp;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityInitializer;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordDecorator;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.record.internal.RecordDecoratorImp;
import se.uu.ub.cora.spider.role.RulesProviderImp;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.spider.unique.UniqueValidatorImp;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.StreamStorageProvider;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.RecordArchiveProvider;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.archive.ResourceArchiveProvider;

public abstract class DependencyProviderAbstract implements SpiderDependencyProvider {
	protected RecordArchiveProvider recordArchiveProvider;
	protected StreamStorageProvider streamStorageProvider;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	protected DependencyProviderAbstract() {
		readInitInfo();
		try {
			tryToInitialize();
		} catch (Exception e) {
			throw new RuntimeException(createExceptionMessage(e), e);
		}
	}

	// Only for test
	public void initializeExtendedFunctionality() {
		ExtendedFunctionalityInitializer initializer = new ExtendedFunctionalityInitializer(this);
		extendedFunctionalityProvider = initializer.getExtendedFunctionalityProvider();
	}

	private String createExceptionMessage(Exception e) {
		return "Error starting " + getImplementingClassName() + ": " + e.getMessage();
	}

	private String getImplementingClassName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public final RecordStorage getRecordStorage() {
		return RecordStorageProvider.getRecordStorage();
	}

	@Override
	public final ResourceArchive getResourceArchive() {
		return ResourceArchiveProvider.getResourceArchive();
	}

	@Override
	public RecordArchive getRecordArchive() {
		return recordArchiveProvider.getRecordArchive();
	}

	public void setRecordArchiveProvider(RecordArchiveProvider recordArchiveProvider) {
		this.recordArchiveProvider = recordArchiveProvider;
	}

	@Override
	public final StreamStorage getStreamStorage() {
		return streamStorageProvider.getStreamStorage();
	}

	public final void setStreamStorageProvider(StreamStorageProvider streamStorageProvider) {
		this.streamStorageProvider = streamStorageProvider;
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return SpiderAuthorizatorImp.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(
				this, new AuthorizatorImp(), new RulesProviderImp(getRecordStorage()));
	}

	@Override
	public DataValidator getDataValidator() {
		DataValidatorFactory dataValidatorFactory = getDataValidatorFactory();
		return dataValidatorFactory.factor();
	}

	DataValidatorFactory getDataValidatorFactory() {
		return new DataValidatorFactoryImp();
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return new DataRecordLinkCollectorImp();
	}

	@Override
	public DataGroupTermCollector getDataGroupTermCollector() {
		return new DataGroupTermCollectorImp();
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return new BasePermissionRuleCalculator();
	}

	@Override
	public DataRedactor getDataRedactor() {
		return createDataRedactorFactory().factor();
	}

	DataRedactorFactory createDataRedactorFactory() {
		return new DataRedactorFactoryImp();
	}

	protected abstract void tryToInitialize() throws Exception;

	protected abstract void readInitInfo();

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return extendedFunctionalityProvider;
	}

	@Override
	public RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
		RecordTypeHandlerFactory recordTypeHandlerFactory = createRecordTypeHandlerFactory();
		return recordTypeHandlerFactory.factorUsingRecordTypeId(recordTypeId);
	}

	RecordTypeHandlerFactory createRecordTypeHandlerFactory() {
		return new RecordTypeHandlerFactoryImp();
	}

	@Override
	public RecordTypeHandler getRecordTypeHandlerUsingDataRecordGroup(
			DataRecordGroup dataRecordGroup) {
		RecordTypeHandlerFactory recordTypeHandlerFactory = createRecordTypeHandlerFactory();
		return recordTypeHandlerFactory.factorUsingDataRecordGroup(dataRecordGroup);
	}

	@Override
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return new DataGroupToRecordEnhancerImp(this);
	}

	@Override
	public DataGroupToFilter getDataGroupToFilterConverter() {
		return new DataGroupToFilterImp();
	}

	@Override
	public UniqueValidator getUniqueValidator(RecordStorage recordStorage) {
		return UniqueValidatorImp.usingRecordStorage(recordStorage);
	}

	@Override
	public DataChangedSender getDataChangeSender() {
		return DataChangedSenderImp.create();
	}

	@Override
	public DataDecarator getDataDecorator() {

		return new DataDecoratorImp(new DataChildDecoratorFactoryImp());
	}

	@Override
	public RecordDecorator getRecordDecorator() {

		return RecordDecoratorImp.usingDependencyProvider(this);
	}
}