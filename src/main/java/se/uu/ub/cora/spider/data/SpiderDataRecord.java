/*
 * Copyright 2015, 2016 Uppsala University Library
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.data.DataRecord;

public final class SpiderDataRecord implements SpiderData {
	private Set<String> keys = new HashSet<>();
	private SpiderDataGroup spiderDataGroup;
	private List<Action> actions = new ArrayList<>();

	public static SpiderDataRecord withSpiderDataGroup(SpiderDataGroup spiderDataGroup) {
		return new SpiderDataRecord(spiderDataGroup);
	}

	private SpiderDataRecord(SpiderDataGroup spiderDataGroup) {
		this.spiderDataGroup = spiderDataGroup;
	}

	public static SpiderDataRecord fromDataRecord(DataRecord dataRecord) {
		return new SpiderDataRecord(dataRecord);
	}

	private SpiderDataRecord(DataRecord dataRecord) {
		keys.addAll(dataRecord.getKeys());
		spiderDataGroup = SpiderDataGroup.fromDataGroup(dataRecord.getDataGroup());
	}

	public void addKey(String key) {
		keys.add(key);
	}

	public boolean containsKey(String key) {
		return keys.contains(key);
	}

	public Set<String> getKeys() {
		return keys;
	}

	public void setSpiderDataGroup(SpiderDataGroup spiderDataGroup) {
		this.spiderDataGroup = spiderDataGroup;

	}

	public SpiderDataGroup getSpiderDataGroup() {
		return spiderDataGroup;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public List<Action> getActions() {
		return actions;
	}

	public DataRecord toDataRecord() {
		DataRecord dataRecord = new DataRecord();
		dataRecord.setDataGroup(spiderDataGroup.toDataGroup());
		dataRecord.getKeys().addAll(keys);
		return dataRecord;
	}
}
