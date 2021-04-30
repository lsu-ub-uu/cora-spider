package se.uu.ub.cora.spider.record.internal;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordCreator;

public class IndexBatchJobCreator implements RecordCreator {

	// TODO: why?? is this here
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;

	public IndexBatchJobCreator(DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
	}

	@Override
	public DataRecord createAndStoreRecord(String authToken, String type, DataGroup record) {
		// TODO Auto-generated method stub
		// RecordCreator recordCreator = SpiderInstanceProvider.getSpiderRecordCreator("");

		return null;
	}

	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return dataGroupToRecordEnhancer;
	}

}
