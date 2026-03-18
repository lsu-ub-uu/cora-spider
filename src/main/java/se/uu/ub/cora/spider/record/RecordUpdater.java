/*
 * Copyright 2015, 2019 Uppsala University Library
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
import se.uu.ub.cora.spider.binary.Uploader;

public interface RecordUpdater {

	DataRecord updateRecord(String authToken, String type, String id, DataRecordGroup record);

	/**
	 * useUploadAsActionInSecurityChecks is used to change action when doing security checks to use
	 * "upload" instead of the standard "update". This makes it possible to resuse updateRecord from
	 * {@link Uploader} and allow that code to change the recordparts about master, without having
	 * to give those permissions to a normal user.
	 */
	void useUploadAsActionInSecurityChecks();

}
