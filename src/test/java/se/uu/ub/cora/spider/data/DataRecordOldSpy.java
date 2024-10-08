/*
 * Copyright 2019 Uppsala University Library
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
package se.uu.ub.cora.spider.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class DataRecordOldSpy implements DataRecord {
	private DataRecordGroup dataRecordGroup;
	private List<Action> actions = new ArrayList<>();
	private Collection<String> readPermissions;
	private Collection<String> writePermissions;
	public List<String> permissionsSentToRead = new ArrayList<>();

	public MethodCallRecorder MCR = new MethodCallRecorder();

	public DataRecordOldSpy(DataRecordGroup dataRecordGroup) {
		MCR.addCall("dataRecordGroup", dataRecordGroup);
		this.dataRecordGroup = dataRecordGroup;
		MCR.addReturned(dataRecordGroup);
	}

	@Override
	public DataRecordGroup getDataRecordGroup() {
		MCR.addCall();
		MCR.addReturned(dataRecordGroup);
		return dataRecordGroup;
	}

	@Override
	public List<Action> getActions() {
		MCR.addCall();
		MCR.addReturned(actions);
		return actions;
	}

	@Override
	public void addAction(Action action) {
		MCR.addCall("action", action);
		actions.add(action);
	}

	@Override
	public void addReadPermission(String readPermission) {
		MCR.addCall("readPermission", readPermission);
	}

	@Override
	public Set<String> getReadPermissions() {
		MCR.addCall();
		Set<String> setReadPermissions = (Set<String>) readPermissions;
		MCR.addReturned(setReadPermissions);
		return setReadPermissions;
	}

	@Override
	public void addWritePermission(String writePermission) {
		MCR.addCall("writePermission", writePermission);
	}

	@Override
	public Set<String> getWritePermissions() {
		MCR.addCall();
		Set<String> setWritePermissions = (Set<String>) writePermissions;
		MCR.addReturned(setWritePermissions);
		return setWritePermissions;
	}

	@Override
	public void setDataRecordGroup(DataRecordGroup dataRecordGroup) {
		MCR.addCall("dataGroup", dataRecordGroup);
		this.dataRecordGroup = dataRecordGroup;
	}

	@Override
	public void addReadPermissions(Collection<String> readPermissions) {
		MCR.addCall("readPermissions", readPermissions);
		this.readPermissions = readPermissions;
	}

	@Override
	public void addWritePermissions(Collection<String> writePermissions) {
		MCR.addCall("writePermissions", writePermissions);
		this.writePermissions = writePermissions;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasActions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasReadPermissions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasWritePermissions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSearchId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addProtocol(String protocol) {
		// TODO Auto-generated method stub
	}

	@Override
	public Set<String> getProtocols() {
		// TODO Auto-generated method stub
		return null;
	}

}
