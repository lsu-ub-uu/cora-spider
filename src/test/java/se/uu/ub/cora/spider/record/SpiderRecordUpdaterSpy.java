/*
 * Copyright 2016 Olov McKie
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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;

public class SpiderRecordUpdaterSpy implements RecordUpdater {

	public String authToken;
	public String type;
	public String id;
	public DataGroup record;

	@Override
	public DataRecord updateRecord(String authToken, String type, String id, DataGroup record) {
		this.authToken = authToken;
		this.type = type;
		this.id = id;
		this.record = record;
		// TODO Auto-generated method stub
		return new DataRecordOldSpy(record);
	}

}
