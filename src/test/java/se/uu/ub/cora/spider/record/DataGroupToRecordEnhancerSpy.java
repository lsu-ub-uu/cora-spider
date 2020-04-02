/*
 * Copyright 2016, 2020 Uppsala University Library
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

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.data.DataRecordSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class DataGroupToRecordEnhancerSpy implements DataGroupToRecordEnhancer {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public User user;
	public String recordType;
	public DataGroup dataGroup;
	public List<DataGroup> enhancedDataGroups = new ArrayList<>();

	public boolean addReadAction = true;

	@Override
	public DataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		MCR.addCall("enhance", "user", user, "recordType", recordType, "dataGroup", dataGroup);

		enhancedDataGroups.add(dataGroup);
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;

		DataRecord dataGroupSpy = new DataRecordSpy(dataGroup);
		if (addReadAction) {
			dataGroupSpy.addAction(Action.READ);
		}
		return dataGroupSpy;
	}

}
