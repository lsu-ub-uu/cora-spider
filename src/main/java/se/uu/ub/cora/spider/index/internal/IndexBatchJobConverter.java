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

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;

public class IndexBatchJobConverter implements BatchJobConverter {

	@Override
	public void updateDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		updateNumOfProcessedRecordsInDataGroup(indexBatchJob, dataGroup);
		updateIndexErrorsInDataGroup(indexBatchJob, dataGroup);
		possiblyUpdateStatus(indexBatchJob, dataGroup);
	}

	private void updateNumOfProcessedRecordsInDataGroup(IndexBatchJob indexBatchJob,
			DataGroup dataGroup) {
		dataGroup.removeFirstChildWithNameInData("numOfProcessedRecords");

		DataAtomic updatedNumOfProcessedRecords = DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("numOfProcessedRecords",
						String.valueOf(indexBatchJob.numOfProcessedRecords));

		dataGroup.addChild(updatedNumOfProcessedRecords);
	}

	private void updateIndexErrorsInDataGroup(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		for (IndexError indexError : indexBatchJob.errors) {
			convertIndexErrorAndAddToDataGroup(dataGroup, indexError);
		}
		setConsecutiveRepeatIdsForErrors(dataGroup);
	}

	private void convertIndexErrorAndAddToDataGroup(DataGroup dataGroup, IndexError indexError) {
		DataGroup errorDataGroup = DataGroupProvider.getDataGroupUsingNameInData("error");
		errorDataGroup.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("recordId",
				indexError.recordId));
		errorDataGroup.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("message",
				indexError.message));
		dataGroup.addChild(errorDataGroup);
	}

	private void setConsecutiveRepeatIdsForErrors(DataGroup dataGroup) {
		for (int i = 0; i < dataGroup.getAllGroupsWithNameInData("error").size(); i++) {
			DataGroup errorDataGroup = dataGroup.getAllGroupsWithNameInData("error").get(i);
			errorDataGroup.setRepeatId(String.valueOf(i));
		}
	}

	private void possiblyUpdateStatus(IndexBatchJob indexBatchJob, DataGroup dataGroup) {
		if (newStatusIsFinished(indexBatchJob)) {
			dataGroup.removeFirstChildWithNameInData("status");
			DataAtomic status = DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("status",
					indexBatchJob.status);
			dataGroup.addChild(status);
		}
	}

	private boolean newStatusIsFinished(IndexBatchJob indexBatchJob) {
		return "finished".equals(indexBatchJob.status);
	}

}
