/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.record.RecordCreator;

public class IndexBatchJobCreatorOrOtherBetterName implements RecordCreator {

	@Override
	public DataRecord createAndStoreRecord(String authToken, String type, DataGroup filter) {

		// set from to to get 1 record in filter
		// DataList readRecordList = SpiderInstanceProvider.getRecordListReader()
		// .readRecordList(authToken, type, null);
		// String totalNumberOfTypeInStorage = readRecordList.getTotalNumberOfTypeInStorage();
		// set totalNumberOf records in indexBatchJobDataGroup

		// create indexBatchJob dataGroup and send to create in storage
		// createDataGroup();

		// Thread t1 = new Thread(new OurClassThatImplementsRunnable ());
		// t1.start();

		// send to other class in its own thread
		// loop records, send each to indexing
		// list records as specified in indexBatchJob, in groups of 10
		// read indexBatchJob (to see if it should be paused)
		// update indexBatchJob with info about the ten just indexed
		// get next group of 10 repeat

		// when finished write status to indexBatchJob

		return null;
	}

	// public DataGroup createDataGroup() {
	// // spånar lite hur datagruppen ska se ut - kanske inte alls behöver allt detta i
	// // testet
	// DataGroupSpy indexBatchJob = new DataGroupSpy("indexBatchJob");
	// DataGroupSpy recordTypeToIndex = new DataGroupSpy("recordTypeToIndex", "recordType",
	// "someRecordType");
	// indexBatchJob.addChild(recordTypeToIndex);
	//
	// indexBatchJob.addChild(new DataAtomicSpy("status", "active"));
	//
	// // filter måste väl vara en del av indexBatchJob?
	// DataGroupSpy filter = new DataGroupSpy("filter");
	// DataGroupSpy include = new DataGroupSpy("include");
	// DataGroupSpy includePart = new DataGroupSpy("includePart");
	// includePart.addChild(new DataAtomicSpy("domain", "uu"));
	// include.addChild(includePart);
	// filter.addChild(include);
	//
	// indexBatchJob.addChild(filter);
	//
	// // tsStarted
	// // tsFinished
	// // totalNumToIndex (totala anatalet som ska indexeras)
	// // numberIndexed (antal som har indexerats)
	// // errorList (meddelanden med info om poster som inte har kunnat indexeras)
	// return indexBatchJob;
	// }

}
