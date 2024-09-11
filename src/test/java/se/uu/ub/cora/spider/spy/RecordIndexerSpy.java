/*
 * Copyright 2017, 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.spy;

import java.util.List;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordIndexerSpy implements RecordIndexer {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	// @Deprecated
	// public DataGroup record;
	// @Deprecated
	// public String id;
	// @Deprecated
	// public String type;
	// @Deprecated
	// public boolean indexDataHasBeenCalled = false;
	// @Deprecated
	// public List<String> ids = new ArrayList<>();
	public boolean throwErrorOnEvenCalls = false;

	@Override
	public void indexData(String recordType, String recordId, List<IndexTerm> indexTerms,
			DataRecordGroup dataRecordGroup) {
		MCR.addCall("recordType", recordType, "recordId", recordId, "indexTerms", indexTerms,
				"dataRecordGroup", dataRecordGroup);
		// this.ids = ids;
		// this.record = record;
		if (throwErrorOnEvenCalls) {
			if (MCR.getNumberOfCallsToMethod("indexData") % 2 == 0) {
				throw new RuntimeException("Some error from spy");
			}
		}
	}

	@Override
	public void deleteFromIndex(String type, String id) {
		MCR.addCall("type", type, "id", id);
		// this.type = type;
		// this.id = id;
	}

	@Override
	public void indexDataWithoutExplicitCommit(String recordType, String recordId,
			List<IndexTerm> indexTerms, DataRecordGroup dataRecordGroup) {
		MCR.addCall("recordType", recordType, "recordId", recordId, "indexTerms", indexTerms,
				"dataRecordGroup", dataRecordGroup);
		// this.ids = ids;
		// this.record = record;
		if (throwErrorOnEvenCalls) {
			if (MCR.getNumberOfCallsToMethod("indexDataWithoutExplicitCommit") % 2 == 0) {
				throw new RuntimeException("Some error from spy");
			}
		}
	}

}
