/*
 * Copyright 2015, 2019, 2026 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;

/**
 * RecordUpdater handles updating of records in the system.
 */
public interface RecordUpdater {

	DataRecord updateRecord(String authToken, String type, String id, DataRecordGroup recordGroup);

	/**
	 * internalUpdateAndStoreRecord is used as it is called from an internal to the system call to
	 * update and store a record in the sytem. Compared to updateRecord this method is not expected
	 * to do any security checks, no validation on data and also not expected to call extended
	 * funtionallity etc. The purpose of this function is store the record, possibly in the archive
	 * and do other storage related activites such as making sure it is indexed.
	 * 
	 * @param recordGroup
	 *            the {@link DataRecordGroup} that is to be updated
	 * @param userId
	 *            a String with the userId that is to be set in the data as the user updating this
	 *            record
	 */
	void internalUpdateRecord(DataRecordGroup recordGroup, String userId);
}
