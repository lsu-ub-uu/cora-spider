/*
 y * Copyright 2015, 2016, 2019, 2020 Uppsala University Library
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollectorImp;
import se.uu.ub.cora.bookkeeper.metadata.MetadataHolder;
import se.uu.ub.cora.bookkeeper.metadata.MetadataHolderFromStoragePopulator;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupRedactor;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupRedactorImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupWrapperFactory;
import se.uu.ub.cora.bookkeeper.recordpart.DataGroupWrapperFactoryImp;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactorImp;
import se.uu.ub.cora.bookkeeper.recordpart.MatcherFactory;
import se.uu.ub.cora.bookkeeper.recordpart.MatcherFactoryImp;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageProvider;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageView;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollectorImp;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactory;
import se.uu.ub.cora.bookkeeper.validator.DataValidatorFactoryImp;
import se.uu.ub.cora.bookkeeper.validator.MetadataMatchData;
import se.uu.ub.cora.bookkeeper.validator.MetadataMatchDataImp;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.BasePermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizatorImp;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.data.internal.DataGroupToFilterImp;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityInitializer;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.internal.DataGroupToRecordEnhancerImp;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactory;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerImp;
import se.uu.ub.cora.spider.role.RulesProviderImp;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.StreamStorageProvider;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.archive.RecordArchiveProvider;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;
import se.uu.ub.cora.storage.idgenerator.RecordIdGeneratorProvider;

public abstract class DependencyProviderAbstract implements SpiderDependencyProvider {
	protected Map<String, String> initInfo;
	protected RecordArchiveProvider recordArchiveProvider;
	protected StreamStorageProvider streamStorageProvider;
	protected RecordIdGeneratorProvider recordIdGeneratorProvider;
	private Logger log = LoggerProvider.getLoggerForClass(DependencyProviderAbstract.class);
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	protected DependencyProviderAbstract(Map<String, String> initInfo) {
		this.initInfo = initInfo;
		readInitInfo();
		try {
			tryToInitialize();
		} catch (InvocationTargetException e) {
			throw new RuntimeException(createInvocationErrorExceptionMessage(e), e);
		} catch (Exception e) {
			throw new RuntimeException(createExceptionMessage(e), e);
		}
	}

	public void initializeExtendedFunctionality() {
		ExtendedFunctionalityInitializer initializer = new ExtendedFunctionalityInitializer(this);
		extendedFunctionalityProvider = initializer.getExtendedFunctionalityProvider();
		System.out.println(
				"SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE SPIKE ");
		spike();
	}

	private MetadataHolder metadataHolder;
	private Map<String, DataGroup> recordTypeHolder;

	private void spike() {
		metadataHolder = new MetadataHolderFromStoragePopulator()
				.createAndPopulateMetadataHolderFromMetadataStorage(
						MetadataStorageProvider.getStorageView());
		recordTypeHolder = createRecordTypeHolder(
				MetadataStorageProvider.getStorageView().getRecordTypes());
	}

	private String createInvocationErrorExceptionMessage(InvocationTargetException e) {
		return "Error starting " + getImplementingClassName() + ": "
				+ e.getTargetException().getMessage();
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

	public final void setRecordIdGeneratorProvider(
			RecordIdGeneratorProvider recordIdGeneratorProvider) {
		this.recordIdGeneratorProvider = recordIdGeneratorProvider;
	}

	@Override
	public final RecordIdGenerator getRecordIdGenerator() {
		return recordIdGeneratorProvider.getRecordIdGenerator();
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return SpiderAuthorizatorImp.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(
				this, new AuthorizatorImp(), new RulesProviderImp(getRecordStorage()));
	}

	// @Override
	// public DataValidator getDataValidator() {
	// // MetadataStorageView metadataStorage = MetadataStorageProvider.getStorageView();
	// //
	// // Map<String, DataGroup> recordTypeHolder = createRecordTypeHolder(
	// // metadataStorage.getRecordTypes());
	// // MetadataHolder metadataHolder = createMetadataHolder(metadataStorage);
	// //
	// // DataValidatorFactory dataValidatorFactory = getDataValidatorFactory();
	// // return dataValidatorFactory.factor(metadataStorage, recordTypeHolder, metadataHolder);
	// return null;
	// }
	@Override
	public DataValidator getDataValidator() {
		// return dependencyProvider.getDataValidator();
		MetadataStorageView metadataStorage = MetadataStorageProvider.getStorageView();

		// Map<String, DataGroup> recordTypeHolder = createRecordTypeHolder(
		// metadataStorage.getRecordTypes());
		// MetadataHolder metadataHolder = createMetadataHolder(metadataStorage);

		DataValidatorFactory dataValidatorFactory = getDataValidatorFactory();
		return dataValidatorFactory.factor(metadataStorage, recordTypeHolder, metadataHolder);
	}

	DataValidatorFactory getDataValidatorFactory() {
		return new DataValidatorFactoryImp();
	}

	private Map<String, DataGroup> createRecordTypeHolder(Collection<DataGroup> recordTypes) {
		Map<String, DataGroup> recordTypeHolder = new HashMap<>();
		for (DataGroup dataGroup : recordTypes) {
			addInfoForRecordTypeToHolder(recordTypeHolder, dataGroup);
		}
		return recordTypeHolder;
	}

	private void addInfoForRecordTypeToHolder(Map<String, DataGroup> recordTypeHolder,
			DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		recordTypeHolder.put(recordId, dataGroup);
	}

	// private MetadataHolder createMetadataHolder(MetadataStorageView metadataStorage) {
	// return new MetadataHolderFromStoragePopulator()
	// .createAndPopulateMetadataHolderFromMetadataStorage(metadataStorage);
	// }
	private MetadataHolder createMetadataHolder(MetadataStorageView metadataStorage) {
		return metadataHolder;
	}

	// @Override
	// public DataRecordLinkCollector getDataRecordLinkCollector() {
	// // return new DataRecordLinkCollectorImp(MetadataStorageProvider.getStorageView());
	// // MetadataHolder metadataHolder = new MetadataHolderFromStoragePopulator()
	// // .createAndPopulateMetadataHolderFromMetadataStorage(
	// // MetadataStorageProvider.getStorageView());
	// // return new DataRecordLinkCollectorImp(metadataHolder);
	// return null;
	// }
	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return new DataRecordLinkCollectorImp(metadataHolder);
	}

	// @Override
	// public DataGroupTermCollector getDataGroupTermCollector() {
	// // return new DataGroupTermCollectorImp(MetadataStorageProvider.getStorageView());
	// // MetadataHolder metadataHolder = new MetadataHolderFromStoragePopulator()
	// // .createAndPopulateMetadataHolderFromMetadataStorage(
	// // MetadataStorageProvider.getStorageView());
	// // return new DataGroupTermCollectorImp(MetadataStorageProvider.getStorageView(),
	// // metadataHolder);
	// return null;
	// }
	@Override
	public DataGroupTermCollector getDataGroupTermCollector() {
		return new DataGroupTermCollectorImp(MetadataStorageProvider.getStorageView(),
				metadataHolder);
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return new BasePermissionRuleCalculator();
	}

	protected void ensureKeyExistsInInitInfo(String key) {
		if (keyNotFoundInInitInfo(key)) {
			String message = createErrorMessage(key);
			log.logFatalUsingMessage(message);
			throw new SpiderInitializationException(message);
		}
	}

	private String createErrorMessage(String key) {
		String simpleName = this.getClass().getSimpleName();
		return "InitInfo in " + simpleName + " must contain: " + key;
	}

	private boolean keyNotFoundInInitInfo(String key) {
		return !initInfo.containsKey(key);
	}

	@Override
	public DataRedactor getDataRedactor() {
		// MetadataStorageView metadataStorage = MetadataStorageProvider.getStorageView();
		// MetadataHolder metadataHolder = createMetadataHolder(metadataStorage);
		DataGroupRedactor dataGroupRedactor = new DataGroupRedactorImp();
		DataGroupWrapperFactory wrapperFactory = new DataGroupWrapperFactoryImp();
		MetadataMatchData metadataMatchData = MetadataMatchDataImp
				.withMetadataHolder(metadataHolder);
		MatcherFactory matcherFactory = new MatcherFactoryImp(metadataMatchData);
		return new DataRedactorImp(metadataHolder, dataGroupRedactor, wrapperFactory,
				matcherFactory);
	}

	protected abstract void tryToInitialize() throws Exception;

	protected abstract void readInitInfo();

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return extendedFunctionalityProvider;
	}

	// @Override
	// public RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
	// RecordStorage recordStorage = getRecordStorage();
	// RecordTypeHandlerFactory recordTypeHandlerFactory = new RecordTypeHandlerFactoryImp(
	// recordStorage);
	// return RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(recordTypeHandlerFactory,
	// recordStorage, recordTypeId);
	// }
	Map<String, RecordTypeHandler> typeHandlers = new HashMap<>();

	@Override
	public synchronized RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
		if (typeHandlers.containsKey(recordTypeId)) {
			return typeHandlers.get(recordTypeId);
		}
		RecordStorage recordStorage = getRecordStorage();
		RecordTypeHandlerFactory recordTypeHandlerFactory = new RecordTypeHandlerFactoryImp(
				recordStorage);
		RecordTypeHandler typeHandler = RecordTypeHandlerImp.usingRecordStorageAndRecordTypeId(
				recordTypeHandlerFactory, recordStorage, recordTypeId);
		typeHandlers.put(recordTypeId, typeHandler);
		return typeHandler;
	}

	@Override
	public String getInitInfoValueUsingKey(String key) {
		ensureKeyExistsInInitInfo(key);
		return initInfo.get(key);
	}

	@Override
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return new DataGroupToRecordEnhancerImp(this);
	}

	@Override
	public DataGroupToFilter getDataGroupToFilterConverter() {
		return new DataGroupToFilterImp();
	}

}
