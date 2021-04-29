package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;

public class IndexBatchJobCreator implements SpiderRecordCreator {

	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;

	public IndexBatchJobCreator(DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
	}

	@Override
	public DataRecord createAndStoreRecord(String authToken, String type, DataGroup record) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataGroupToRecordEnhancer getDataGroupToRecordEnhancer() {
		return dataGroupToRecordEnhancer;
	}

}
