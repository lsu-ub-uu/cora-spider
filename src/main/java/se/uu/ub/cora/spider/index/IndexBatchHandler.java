package se.uu.ub.cora.spider.index;

import se.uu.ub.cora.data.DataGroup;

/**
 * IndexBatchHandler is responsible for running index batch jobs in batches, reading an appropriate
 * number of records from storage, extracting search terms, and sending the records and the
 * extracted search terms for indexing. <br>
 */
public interface IndexBatchHandler {

	/**
	 * runIndexBatchJob takes a DataGroup with information about the recordType, and filter of
	 * records to index. Calls to runIndexBatchJob MUST return directly and do the main work of
	 * indexing in a different thread from the one calling this method.
	 * 
	 * @param indexBatchJob
	 *            A DataGroup containing information about what to index, including RecordType, the
	 *            number of records to index, and Filter
	 */
	void runIndexBatchJob(DataGroup indexBatchJob);

	/**
	 * Loop <br>
	 * Call recordList with filter in order to get the records to be indexed <br>
	 * Extract ids from the record. <br>
	 * Start/Call indexing of the ids. The index should be in batches <br>
	 * Uppdatera indexBatchJob record with the updated ids. <br>
	 * hantera throttle <br>
	 * endloop.<br>
	 */
}
