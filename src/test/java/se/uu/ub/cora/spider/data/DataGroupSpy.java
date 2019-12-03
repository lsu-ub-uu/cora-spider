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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;

public class DataGroupSpy implements DataGroup {

	public String nameInData;
	public List<DataElement> children = new ArrayList<>();
	public String repeatId;
	public Map<String, String> attributes = new HashMap<>();

	public DataGroupSpy(String nameInData) {
		this.nameInData = nameInData;
	}

	public DataGroupSpy(String nameInData, String recordType, String recordId) {
		this.nameInData = nameInData;
		addChild(new DataAtomicSpy("linkedRecordType", recordType));
		addChild(new DataAtomicSpy("linkedRecordId", recordId));
	}

	@Override
	public String getRepeatId() {
		return repeatId;
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	@Override
	public String getFirstAtomicValueWithNameInData(String nameInData) {
		for (DataElement dataElement : children) {
			if (nameInData.equals(dataElement.getNameInData())) {
				if (dataElement instanceof DataAtomic) {
					return ((DataAtomic) dataElement).getValue();
				}
			}
		}
		return null;
	}

	@Override
	public DataGroup getFirstGroupWithNameInData(String childNameInData) {
		for (DataElement dataElement : children) {
			if (childNameInData.equals(dataElement.getNameInData())) {
				if (dataElement instanceof DataGroup) {
					return ((DataGroup) dataElement);
				}
			}
		}
		return null;
	}

	@Override
	public void addChild(DataElement dataElement) {
		children.add(dataElement);

	}

	@Override
	public List<DataElement> getChildren() {
		return children;
	}

	@Override
	public boolean containsChildWithNameInData(String nameInData) {
		for (DataElement dataElement : children) {
			if (nameInData.equals(dataElement.getNameInData())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setRepeatId(String repeatId) {
		this.repeatId = repeatId;

	}

	@Override
	public void addAttributeByIdWithValue(String id, String value) {
		attributes.put(id, value);

	}

	@Override
	public DataElement getFirstChildWithNameInData(String nameInData) {
		for (DataElement dataElement : children) {
			if (nameInData.equals(dataElement.getNameInData())) {
				return dataElement;
			}
		}
		return null;
	}

	@Override
	public List<DataGroup> getAllGroupsWithNameInData(String nameInData) {
		List<DataGroup> matchingDataGroups = new ArrayList<>();
		for (DataElement dataElement : children) {
			if (nameInData.equals(dataElement.getNameInData())
					&& dataElement instanceof DataGroup) {
				matchingDataGroups.add((DataGroup) dataElement);
			}
		}
		return matchingDataGroups;
	}

	@Override
	public String getAttribute(String attributeId) {
		return attributes.get(attributeId);
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

	@Override
	public List<DataAtomic> getAllDataAtomicsWithNameInData(String childNameInData) {
		List<DataAtomic> matchingDataAtomics = new ArrayList<>();
		for (DataElement dataElement : children) {
			if (childNameInData.equals(dataElement.getNameInData())
					&& dataElement instanceof DataAtomic) {
				matchingDataAtomics.add((DataAtomic) dataElement);
			}
		}
		return matchingDataAtomics;
	}

	@Override
	public void removeFirstChildWithNameInData(String childNameInData) {
		DataElement foundChild = tryToFindChildToRemove(childNameInData);
		if (foundChild != null) {
			children.remove(foundChild);
		}
	}

	private DataElement tryToFindChildToRemove(String childNameInData) {

		for (DataElement dataElement : children) {
			if (childNameInData.equals(dataElement.getNameInData())) {
				return dataElement;
			}
		}
		return null;
	}

	@Override
	public Collection<DataGroup> getAllGroupsWithNameInDataAndAttributes(String childNameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

}
