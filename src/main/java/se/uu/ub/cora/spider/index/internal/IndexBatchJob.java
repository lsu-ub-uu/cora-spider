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

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.storage.Filter;

public class IndexBatchJob {

	public String recordId;
	public String recordTypeToIndex;
	public long totalNumberToIndex = 0;
	public long numberOfProcessedRecords = 0;
	public String status = "started";
	public List<IndexError> errors = new ArrayList<>();
	public Filter filter;

	public IndexBatchJob(String recordTypeToIndex, long totalNumberToIndex, Filter filter) {
		this.recordTypeToIndex = recordTypeToIndex;
		this.totalNumberToIndex = totalNumberToIndex;
		this.filter = filter;
	}
}
