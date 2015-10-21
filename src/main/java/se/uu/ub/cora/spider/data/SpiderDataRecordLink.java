/*
 * Copyright 2015 Uppsala University Library
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

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public final class SpiderDataRecordLink implements SpiderDataElement {

	private String nameInData;
	private String recordType;
	private String recordId;
	private Set<Action> actions = new HashSet<>();
	private String repeatId;

	public static SpiderDataRecordLink withNameInDataAndRecordTypeAndRecordId(String nameInData,
			String recordType, String recordId) {
		return new SpiderDataRecordLink(nameInData, recordType, recordId);
	}

	private SpiderDataRecordLink(String nameInData, String recordType, String recordId) {
		this.nameInData = nameInData;
		this.recordType = recordType;
		this.recordId = recordId;
	}

	private SpiderDataRecordLink(DataRecordLink dataRecordLink) {
		nameInData = dataRecordLink.getNameInData();
		recordType = dataRecordLink.getRecordType();
		recordId = dataRecordLink.getRecordId();
		repeatId = dataRecordLink.getRepeatId();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getRecordType() {
		return recordType;
	}

	public String getRecordId() {
		return recordId;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public Set<Action> getActions() {
		return actions;
	}

	public DataRecordLink toDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndRecordTypeAndRecordId(nameInData, recordType, recordId);
		dataRecordLink.setRepeatId(repeatId);
		return dataRecordLink;
	}

	public static SpiderDataRecordLink fromDataRecordLink(DataRecordLink dataRecordLink) {
		return new SpiderDataRecordLink(dataRecordLink);
	}

	public void setRepeatId(String repeatId) {
		this.repeatId = repeatId;
	}

	public String getRepeatId() {
		return repeatId;
	}

}
