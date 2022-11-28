package se.uu.ub.cora.spider.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public class DependencyProviderOneCall implements SpiderDependencyProvider {

	private SpiderDependencyProvider dependencyProvider;
	private MetadataHolder metadataHolder;
	private Map<String, DataGroup> recordTypeHolder;

	public DependencyProviderOneCall(SpiderDependencyProvider dependencyProvider) {
		System.out.println("***********starting dependency provider one call **********");
		this.dependencyProvider = dependencyProvider;
		metadataHolder = new MetadataHolderFromStoragePopulator()
				.createAndPopulateMetadataHolderFromMetadataStorage(
						MetadataStorageProvider.getStorageView());
		recordTypeHolder = createRecordTypeHolder(
				MetadataStorageProvider.getStorageView().getRecordTypes());
	}

	@Override
	public RecordStorage getRecordStorage() {
		return dependencyProvider.getRecordStorage();
	}

	@Override
	public RecordArchive getRecordArchive() {
		return dependencyProvider.getRecordArchive();
	}

	@Override
	public StreamStorage getStreamStorage() {
		return dependencyProvider.getStreamStorage();
	}

	@Override
	public RecordIdGenerator getRecordIdGenerator() {
		return dependencyProvider.getRecordIdGenerator();
	}

	@Override
	public SpiderAuthorizator getSpiderAuthorizator() {
		return dependencyProvider.getSpiderAuthorizator();
	}

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

	DataValidatorFactory getDataValidatorFactory() {
		return new DataValidatorFactoryImp();
	}

	@Override
	public DataRecordLinkCollector getDataRecordLinkCollector() {
		return new DataRecordLinkCollectorImp(metadataHolder);
	}

	@Override
	public DataGroupTermCollector getDataGroupTermCollector() {
		return new DataGroupTermCollectorImp(MetadataStorageProvider.getStorageView(),
				metadataHolder);
	}

	@Override
	public PermissionRuleCalculator getPermissionRuleCalculator() {
		return dependencyProvider.getPermissionRuleCalculator();
	}

	// @Override
	// public DataRedactor getDataRedactor() {
	// return dependencyProvider.getDataRedactor();
	// }
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

	@Override
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		return dependencyProvider.getExtendedFunctionalityProvider();
	}

	@Override
	public Authenticator getAuthenticator() {
		return dependencyProvider.getAuthenticator();
	}

	@Override
	public RecordSearch getRecordSearch() {
		return dependencyProvider.getRecordSearch();
	}

	@Override
	public RecordIndexer getRecordIndexer() {
		return dependencyProvider.getRecordIndexer();
	}

	@Override
	public RecordTypeHandler getRecordTypeHandler(String recordTypeId) {
		return dependencyProvider.getRecordTypeHandler(recordTypeId);
	}

	@Override
	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return dependencyProvider.getDataGroupToRecordEnhancer();
	}

	@Override
	public String getInitInfoValueUsingKey(String key) {
		return dependencyProvider.getInitInfoValueUsingKey(key);
	}

	@Override
	public DataGroupToFilter getDataGroupToFilterConverter() {
		return dependencyProvider.getDataGroupToFilterConverter();
	}

}
