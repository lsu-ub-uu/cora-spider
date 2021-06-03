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

/**
 * BatchRunner is responsible for running batch jobs in batches, reading an appropriate number of
 * records from storage, extracting search terms, and sending the records and the extracted search
 * terms for indexing. <br>
 * 
 * It is responsible to report about the indexed records on each loop of the batch.
 * 
 * 
 */
public interface BatchRunner extends Runnable {

	/**
	 * run methods overrides Runnable run method. It is intended to run in a newly created Thread.
	 * <p>
	 * Given a IndexBatchJob with a value containing the total number of records to index. With this
	 * value run method will start a loop with X number of post which will be retrived from storage,
	 * will be indexed and will be reported as indexed on the IndexBatchJob record
	 */

	@Override
	void run();

}
