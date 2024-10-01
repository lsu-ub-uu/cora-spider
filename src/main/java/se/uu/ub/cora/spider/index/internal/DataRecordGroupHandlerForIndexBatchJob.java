/*
 * Copyright 2021 Uppsala University Library
 * Copyright 2024 Olov McKie
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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;

/**
 * DataRecordGroupHandlerForIndexBatchJob is responsible for
 * 
 * 
 */

public interface DataRecordGroupHandlerForIndexBatchJob {

	/**
	 * updateDataGroup
	 * 
	 * @param indexBatchJob
	 *            An IndexBatchJob containing information about what to index, including RecordType,
	 *            the number of records to index, and Filter
	 * 
	 * @param dataRecordGroup
	 *            Is the updated DataRecordGroup containing the changes from indexBatchJob
	 */
	void updateDataRecordGroup(IndexBatchJob indexBatchJob, DataRecordGroup dataGroup);

	/**
	 * createDataGroup
	 * 
	 * @param indexBatchJob
	 *            An IndexBatchJob containing information about what to index, including RecordType,
	 *            the number of records to index, and Filter
	 * @param filterAsDataGroup
	 *            A DataGroup, filter, as set by the user
	 * @return A {@link DataRecordGroup} with the created indexBathJob
	 */
	DataRecordGroup createDataRecordGroup(IndexBatchJob indexBatchJob, DataGroup filterAsDataGroup);

}
