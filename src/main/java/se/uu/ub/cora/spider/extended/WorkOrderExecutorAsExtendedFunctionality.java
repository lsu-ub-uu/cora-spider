package se.uu.ub.cora.spider.extended;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class WorkOrderExecutorAsExtendedFunctionality implements ExtendedFunctionality {

	private RecordIndexer recordIndexer;
	private DataGroupTermCollector collectTermCollector;
	private RecordStorage recordStorage;

	public WorkOrderExecutorAsExtendedFunctionality(SpiderDependencyProvider dependencyProvider) {
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.collectTermCollector = dependencyProvider.getDataGroupSearchTermCollector();
		this.recordStorage = dependencyProvider.getRecordStorage();
	}

	public static WorkOrderExecutorAsExtendedFunctionality usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new WorkOrderExecutorAsExtendedFunctionality(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(String authToken, SpiderDataGroup spiderDataGroup) {
		DataGroup workOrder = spiderDataGroup.toDataGroup();
		String recordType = getRecordTypeToIndexFromWorkOrder(workOrder);

		String metadataId = getMetadataIdFromRecordType(recordType);

		DataGroup dataToIndex = recordStorage.read(recordType,
				workOrder.getFirstAtomicValueWithNameInData("recordId"));
		collectTermCollector.collectTerms(metadataId, dataToIndex);

		recordIndexer.indexData(DataGroup.withNameInData("collectedDataTerm"), dataToIndex);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		DataGroup readRecordType = recordStorage.read("recordType", recordType);
		DataGroup metadataIdLink = readRecordType.getFirstGroupWithNameInData("metadataId");
		return metadataIdLink.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private String getRecordTypeToIndexFromWorkOrder(DataGroup workOrder) {
		DataGroup recordTypeLink = workOrder.getFirstGroupWithNameInData("recordType");
		return recordTypeLink.getFirstAtomicValueWithNameInData("linkedRecordId");
	}
}
