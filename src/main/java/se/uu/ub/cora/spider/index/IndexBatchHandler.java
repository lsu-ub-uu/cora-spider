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
package se.uu.ub.cora.spider.index;

import se.uu.ub.cora.spider.index.internal.IndexBatchJob;

/**
 * IndexBatchHandler is responsible for starting index batch job in a single thread. <br>
 * 
 * IndexBatchHandler is also responsible for running index batch jobs in batches, reading an
 * appropriate number of records from storage, extracting search terms, and sending the records and
 * the extracted search terms for indexing. <br>
 * 
 */
public interface IndexBatchHandler {

	/**
	 * runIndexBatchJob takes an IndexBatchJob with information about the recordType, and filter of
	 * records to index. Calls to runIndexBatchJob MUST return control directly and do the main work
	 * of indexing in a different thread from the one calling this method.
	 * <p>
	 * Note, a record must exist in RecordStorage with the recordId as set in indexBatchJob (and
	 * recordType indexBatchJob), before a call to this method can be made.
	 * 
	 * @param indexBatchJob
	 *            An IndexBatchJob containing information about what to index, including RecordId,
	 *            RecordTypeToIndex, the number of records to index, and Filter
	 */
	void runIndexBatchJob(IndexBatchJob indexBatchJob);
}
