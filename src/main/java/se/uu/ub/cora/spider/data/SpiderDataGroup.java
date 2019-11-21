/*
 * Copyright 2015 Uppsala University Library
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

package se.uu.ub.cora.spider.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;

public class SpiderDataGroup implements SpiderDataElement, SpiderData {

	private String nameInData;
	private Map<String, String> attributes = new HashMap<>();
	private List<SpiderDataElement> children = new ArrayList<>();
	private String repeatId;
	private Predicate<SpiderDataElement> isDataGroup = dataElement -> dataElement instanceof SpiderDataGroup;

	public static SpiderDataGroup withNameInData(String nameInData) {
		return new SpiderDataGroup(nameInData);
	}

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

	private final void convertAndSetChildren(DataGroup dataGroup) {
		for (DataElement dataElement : dataGroup.getChildren()) {
			children.add(convertToSpiderEquivalentDataClass(dataElement));
		}
	}

	private SpiderDataElement convertToSpiderEquivalentDataClass(DataElement dataElement) {
		if (dataElement instanceof DataGroup) {
			return convertDataGroupElementToSpiderEquivalentDataClass((DataGroup) dataElement);
		}
		return SpiderDataAtomic.fromDataAtomic((DataAtomic) dataElement);
	}

	private SpiderDataElement convertDataGroupElementToSpiderEquivalentDataClass(
			DataGroup dataGroup) {
		if (dataGroupIsRecordLink(dataGroup)) {
			return SpiderDataRecordLink.fromDataRecordLink(dataGroup);
		}
		if (dataGroupIsResourceLink(dataGroup)) {
			return SpiderDataResourceLink.fromDataRecordLink(dataGroup);
		}

		return SpiderDataGroup.fromDataGroup(dataGroup);
	}

	private boolean dataGroupIsRecordLink(DataGroup dataGroup) {
		return dataGroup.containsChildWithNameInData("linkedRecordType")
				&& dataGroup.containsChildWithNameInData("linkedRecordId");
	}

	private boolean dataGroupIsResourceLink(DataGroup dataGroup) {
		return dataGroup.containsChildWithNameInData("streamId")
				&& dataGroup.containsChildWithNameInData("filename")
				&& dataGroup.containsChildWithNameInData("filesize")
				&& dataGroup.containsChildWithNameInData("mimeType");
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
		DataGroup dataGroup = DataGroupProvider.getDataGroupUsingNameInData(nameInData);
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
		if (child instanceof SpiderDataGroup) {
			if (child instanceof SpiderDataRecordLink) {
				return ((SpiderDataRecordLink) child).toDataGroup();
			}
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

	public void removeChild(String string) {
		SpiderDataElement firstChildWithNameInData = getFirstChildWithNameInData(string);
		children.remove(firstChildWithNameInData);
	}

	public List<SpiderDataGroup> getAllGroupsWithNameInData(String childNameInData) {
		return getGroupChildrenWithNameInDataStream(childNameInData).collect(Collectors.toList());
	}

	private Stream<SpiderDataGroup> getGroupChildrenWithNameInDataStream(String childNameInData) {
		return getGroupChildrenStream().filter(filterByNameInData(childNameInData))
				.map(SpiderDataGroup.class::cast);
	}

	private Stream<SpiderDataElement> getGroupChildrenStream() {
		return getChildrenStream().filter(isDataGroup);
	}

	private Stream<SpiderDataElement> getChildrenStream() {
		return children.stream();
	}

	private Predicate<SpiderDataElement> filterByNameInData(String childNameInData) {
		return dataElement -> dataElementsNameInDataIs(dataElement, childNameInData);
	}

	private boolean dataElementsNameInDataIs(SpiderDataElement dataElement,
			String childNameInData) {
		return dataElement.getNameInData().equals(childNameInData);
	}
}
