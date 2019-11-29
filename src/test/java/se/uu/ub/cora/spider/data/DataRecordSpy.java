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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;

public class DataRecordSpy implements DataRecord {

	public DataGroup dataGroup;
	public Set<String> keys = new LinkedHashSet<>();
	public List<Action> actions = new ArrayList<>();

	public DataRecordSpy(DataGroup dataGroup) {
		this.dataGroup = dataGroup;
	}

	@Override
	public DataGroup getDataGroup() {
		return dataGroup;
	}

	@Override
	public void addKey(String key) {
		keys.add(key);
	}

	@Override
	public Set<String> getKeys() {
		return keys;
	}

	@Override
	public List<Action> getActions() {
		return actions;
	}

	@Override
	public void addAction(Action action) {
		actions.add(action);

	}

}
