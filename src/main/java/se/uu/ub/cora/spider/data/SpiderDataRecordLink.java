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

import se.uu.ub.cora.bookkeeper.data.DataRecordLink;

public final class SpiderDataRecordLink implements SpiderDataElement {

	private String nameInData;
	private String linkedRecordType;
	private String linkedRecordId;
	private Set<Action> actions = new HashSet<>();
	private String repeatId;
	private String linkedRepeatId;
	private SpiderDataGroup linkedPath;

	public static SpiderDataRecordLink withNameInDataAndLinkedRecordTypeAndLinkedRecordId(
			String nameInData, String linkedRecordType, String linkedRecordId) {
		return new SpiderDataRecordLink(nameInData, linkedRecordType, linkedRecordId);
	}

	private SpiderDataRecordLink(String nameInData, String linkedRecordType, String linkedRecordId) {
		this.nameInData = nameInData;
		this.linkedRecordType = linkedRecordType;
		this.linkedRecordId = linkedRecordId;
	}

	private SpiderDataRecordLink(DataRecordLink dataRecordLink) {
		nameInData = dataRecordLink.getNameInData();
		linkedRecordType = dataRecordLink.getLinkedRecordType();
		linkedRecordId = dataRecordLink.getLinkedRecordId();
		repeatId = dataRecordLink.getRepeatId();
		linkedRepeatId = dataRecordLink.getLinkedRepeatId();
		addLinkedPathFromDataRecordLinkIfItExists(dataRecordLink);
	}

	private void addLinkedPathFromDataRecordLinkIfItExists(DataRecordLink dataRecordLink) {
		if(dataRecordLink.getLinkedPath() != null){
			linkedPath = SpiderDataGroup.fromDataGroup(dataRecordLink.getLinkedPath());
		}
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getLinkedRecordType() {
		return linkedRecordType;
	}

	public String getLinkedRecordId() {
		return linkedRecordId;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public Set<Action> getActions() {
		return actions;
	}

	public DataRecordLink toDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId(nameInData, linkedRecordType, linkedRecordId);
		dataRecordLink.setRepeatId(repeatId);
		dataRecordLink.setLinkedRepeatId(linkedRepeatId);
		addLinkedPathToDataRecordLinkIfItExists(dataRecordLink);
		return dataRecordLink;
	}

	private void addLinkedPathToDataRecordLinkIfItExists(DataRecordLink dataRecordLink) {
		if(linkedPath != null) {
			dataRecordLink.setLinkedPath(linkedPath.toDataGroup());
		}
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

	public void setLinkedRepeatId(String linkedRepeatId) {
		this.linkedRepeatId = linkedRepeatId;
	}

	public String getLinkedRepeatId() {
		return linkedRepeatId;
	}

	public void setLinkedPath(SpiderDataGroup linkedPath) {
		this.linkedPath = linkedPath;
	}

	public SpiderDataGroup getLinkedPath() {
		return linkedPath;
	}
}
