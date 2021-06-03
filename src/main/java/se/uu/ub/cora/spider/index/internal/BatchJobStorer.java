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
package se.uu.ub.cora.spider.index.internal;

/**
 * BatchJobStorer is responsible for storing IndexBatchJobs.
 */
public interface BatchJobStorer {

	/**
	 * store updates an existing indexBatchJob record in storage using the supplied IndexBatchJob.
	 * <p>
	 * This is done by first reading the indexBatchJob record from storage updating the record using
	 * information from the supplied indexBatchJob and then sending the updated record back to
	 * storage.
	 * <p>
	 * The information that is updated is, numberOfProcessedRecords, errors and status
	 * 
	 * @param indexBatchJob
	 *            An {@link IndexBatchJob} containing updated information
	 */
	void store(IndexBatchJob indexBatchJob);

}
