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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
//import se.uu.ub.cora.bookkeeper.data.DataRecordLink;

public class SpiderDataGroup implements SpiderDataElement, SpiderData {

	private String nameInData;
	private Map<String, String> attributes = new HashMap<>();
	private List<SpiderDataElement> children = new ArrayList<>();
	private String repeatId;

	public static SpiderDataGroup withNameInData(String nameInData) {
		return new SpiderDataGroup(nameInData);
	}

//	protected SpiderDataGroup(){}

	protected SpiderDataGroup(String nameInData) {
		this.nameInData = nameInData;
	}

	public static SpiderDataGroup fromDataGroup(DataGroup dataGroup) {
		return new SpiderDataGroup(dataGroup);
	}

	protected SpiderDataGroup(DataGroup dataGroup) {
		nameInData = dataGroup.getNameInData();
		repeatId = dataGroup.getRepeatId();
		attributes.putAll(dataGroup.getAttributes());
		convertAndSetChildren(dataGroup);
	}

	private void convertAndSetChildren(DataGroup dataGroup) {
		for (DataElement dataElement : dataGroup.getChildren()) {
			children.add(convertToSpiderEquivalentDataClass(dataElement));
		}
	}

	private SpiderDataElement convertToSpiderEquivalentDataClass(DataElement dataElement) {
		if (dataElement instanceof DataGroup) {
			DataGroup dataGroup = (DataGroup) dataElement;
			if(dataGroupIsRecordLink(dataGroup)){
				return SpiderDataRecordLink.fromDataRecordLink(dataGroup);
			}

			return SpiderDataGroup.fromDataGroup((dataGroup));
		}
//		if (dataElement instanceof DataRecordLink) {
//			return SpiderDataRecordLink.fromDataRecordLink((DataRecordLink) dataElement);
//		}
		return SpiderDataAtomic.fromDataAtomic((DataAtomic) dataElement);
	}

	private boolean dataGroupIsRecordLink(DataGroup dataGroup) {
		return dataGroup.containsChildWithNameInData("linkedRecordType") &&
                dataGroup.containsChildWithNameInData("linkedRecordId");
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public List<SpiderDataElement> getChildren() {
		return children;
	}

	public void addAttributeByIdWithValue(String nameInData, String value) {
		attributes.put(nameInData, value);
	}

	public void addChild(SpiderDataElement dataElement) {
		children.add(dataElement);
	}

	public DataGroup toDataGroup() {
		DataGroup dataGroup = DataGroup.withNameInData(nameInData);
		dataGroup.setRepeatId(repeatId);
		addAttributesToDataGroup(dataGroup);

		addChildrenToDataGroup(dataGroup);

		return dataGroup;
	}

	private void addAttributesToDataGroup(DataGroup dataGroup) {
		for (Entry<String, String> entry : attributes.entrySet()) {
			dataGroup.addAttributeByIdWithValue(entry.getKey(), entry.getValue());
		}
	}

	private void addChildrenToDataGroup(DataGroup dataGroup) {
		for (SpiderDataElement child : children) {
			dataGroup.addChild(convertToCorrectDataElement(child));
		}
	}

	private DataElement convertToCorrectDataElement(SpiderDataElement child) {
		if (child instanceof SpiderDataRecordLink) {
			return ((SpiderDataRecordLink) child).toDataGroup();
		}
		if (child instanceof SpiderDataGroup) {
			return ((SpiderDataGroup) child).toDataGroup();
		}
		return ((SpiderDataAtomic) child).toDataAtomic();
	}

	public boolean containsChildWithNameInData(String nameInData) {
		for (SpiderDataElement dataElement : children) {
			if (dataElement.getNameInData().equals(nameInData)) {
				return true;
			}
		}
		return false;
	}

	public SpiderDataElement getFirstChildWithNameInData(String nameInData) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getNameInData().equals(nameInData)) {
				return spiderDataElement;
			}
		}
		throw new DataMissingException("Requested child " + nameInData + " does not exist");
	}

	public SpiderDataGroup extractGroup(String groupId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getNameInData().equals(groupId)) {
				return (SpiderDataGroup) spiderDataElement;
			}
		}
		throw new DataMissingException("Requested dataGroup " + groupId + " doesn't exist");
	}

	public String extractAtomicValue(String atomicId) {
		for (SpiderDataElement spiderDataElement : getChildren()) {
			if (spiderDataElement.getNameInData().equals(atomicId)) {
				return ((SpiderDataAtomic) spiderDataElement).getValue();
			}
		}
		throw new DataMissingException("Requested dataAtomic " + atomicId + " does not exist");
	}

	public void setRepeatId(String repeatId) {
		this.repeatId = repeatId;
	}

	public String getRepeatId() {
		return repeatId;
	}
}
