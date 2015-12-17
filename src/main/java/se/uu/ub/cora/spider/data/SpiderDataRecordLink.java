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

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

import java.util.HashSet;
import java.util.Set;

//import se.uu.ub.cora.bookkeeper.data.DataRecordLink;

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

	private SpiderDataRecordLink(DataGroup dataRecordLink) {
		nameInData = dataRecordLink.getNameInData();
		linkedRecordType = dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordType");
		linkedRecordId = dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordId");
		repeatId = dataRecordLink.getRepeatId();
		linkedRepeatId = dataRecordLink.getFirstAtomicValueWithNameInData("linkedRepeatId");
		addLinkedPathFromDataRecordLinkIfItExists(dataRecordLink);
	}

	private void addLinkedPathFromDataRecordLinkIfItExists(DataGroup dataRecordLink) {
		if(dataRecordLink.containsChildWithNameInData("linkedPath")){
//		if(dataRecordLink.getLinkedPath() != null){
			linkedPath = SpiderDataGroup.fromDataGroup(dataRecordLink.getFirstGroupWithNameInData("linkedPath"));
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

	public DataGroup toDataRecordLink() {
//		DataRecordLink dataRecordLink = DataRecordLink
//				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId(nameInData, linkedRecordType, linkedRecordId);

		DataGroup dataRecordLink = DataGroup.withNameInData(nameInData);
		DataAtomic linkedRecordTypeChild = DataAtomic.withNameInDataAndValue("linkedRecordType", linkedRecordType);
		dataRecordLink.addChild(linkedRecordTypeChild);

		DataAtomic linkedRecordIdChild = DataAtomic.withNameInDataAndValue("linkedRecordId", linkedRecordId);
		dataRecordLink.addChild(linkedRecordIdChild);

		DataAtomic linkedRepeatIdChild = DataAtomic.withNameInDataAndValue("linkedRepeatId", linkedRepeatId);
		dataRecordLink.addChild(linkedRepeatIdChild);
//		dataRecordLink.setLinkedRepeatId(linkedRepeatId);
		dataRecordLink.setRepeatId(repeatId);
		addLinkedPathToDataRecordLinkIfItExists(dataRecordLink);
		return dataRecordLink;
	}

	private void addLinkedPathToDataRecordLinkIfItExists(DataGroup dataRecordLink) {
		if(linkedPath != null) {
			dataRecordLink.addChild(linkedPath.toDataGroup());
		}
	}

	public static SpiderDataRecordLink fromDataRecordLink(DataGroup dataRecordLink) {
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
