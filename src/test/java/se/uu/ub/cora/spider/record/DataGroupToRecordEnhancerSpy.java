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
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class DataGroupToRecordEnhancerSpy implements DataGroupToRecordEnhancer {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public User user;
	public String recordType;
	public DataRecordGroup dataRecordGroup;
	public List<DataRecordGroup> enhancedDataGroups = new ArrayList<>();
	/**
	 * addReadAction is default true, if set to false will no read action be added. For method
	 * {@link #enhance(User, String, DataRecordGroup, DataRedactor)} will an AuthorizationException
	 * be thrown, if method
	 * {@link #enhanceIgnoringReadAccess(User, String, DataRecordGroup, DataRedactor)} is called,
	 * will no exception be thrown and the enhanced record returned, as is specified in interface
	 * for DataGroupToRecordEnhancer.
	 */
	public boolean addReadAction = true;
	/**
	 * addReadActionOnlyFirst is default false, if set to true will add read action for the first
	 * call to enhance, and following calls will get (an AuthorizationException thrown) instead of
	 * an added action.
	 */
	public boolean addReadActionOnlyFirst = false;

	/**
	 * throwOtherException is default false, if set to true will a call to enhance throw an
	 * exception other than AuthorizationException
	 */
	public boolean throwOtherException = false;

	@Override
	public DataRecord enhance(User user, String recordType, DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		MCR.addCall("user", user, "recordType", recordType, "dataRecordGroup", dataRecordGroup,
				"dataRedactor", dataRedactor);

		if (!addReadAction) {
			throw new AuthorizationException(recordType);
		}

		DataRecord dataGroupSpy = spyEnhanceDataGroupToRecord(user, recordType, dataRecordGroup);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

	private DataRecord spyEnhanceDataGroupToRecord(User user, String recordType,
			DataRecordGroup dataRecordGroup) {
		if (throwOtherException) {
			throw new RuntimeException();
		}

		enhancedDataGroups.add(dataRecordGroup);
		this.user = user;
		this.recordType = recordType;
		this.dataRecordGroup = dataRecordGroup;

		DataRecord dataGroupSpy = new DataRecordOldSpy(dataRecordGroup);
		if (addReadAction) {
			dataGroupSpy.addAction(Action.READ);
			if (addReadActionOnlyFirst) {
				addReadAction = false;
			}
		}
		return dataGroupSpy;
	}

	@Override
	public DataRecord enhanceIgnoringReadAccess(User user, String recordType,
			DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
		MCR.addCall("user", user, "recordType", recordType, "dataRecordGroup", dataRecordGroup,
				"dataRedactor", dataRedactor);
		DataRecord dataGroupSpy = spyEnhanceDataGroupToRecord(user, recordType, dataRecordGroup);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

}
