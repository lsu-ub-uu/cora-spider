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
package se.uu.ub.cora.spider.testspies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataChildFilter;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataResourceLink;

public class DataResourceLinkSpy implements DataGroup, DataResourceLink {

	private String nameInData;
	public List<DataChild> children = new ArrayList<>();
	public List<Action> actions = new ArrayList<>();

	public DataResourceLinkSpy(String nameInData) {
		this.nameInData = nameInData;
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	@Override
	public String getRepeatId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAction(Action action) {
		actions.add(action);

	}

	@Override
	public String getFirstAtomicValueWithNameInData(String nameInData) {
		for (DataChild dataElement : children) {
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
		for (DataChild dataElement : children) {
			if (childNameInData.equals(dataElement.getNameInData())) {
				if (dataElement instanceof DataGroup) {
					return ((DataGroup) dataElement);
				}
			}
		}
		return null;
	}

	@Override
	public void addChild(DataChild dataElement) {
		children.add(dataElement);
	}

	@Override
	public List<DataChild> getChildren() {
		return children;
	}

	@Override
	public boolean containsChildWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setRepeatId(String repeatId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addAttributeByIdWithValue(String id, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public DataChild getFirstChildWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DataGroup> getAllGroupsWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataAttribute getAttribute(String attributeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DataAtomic> getAllDataAtomicsWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeFirstChildWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<DataGroup> getAllGroupsWithNameInDataAndAttributes(String childNameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataAttribute> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addChildren(Collection<DataChild> dataElements) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<DataChild> getAllChildrenWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeAllChildrenWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataAtomic getFirstDataAtomicWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public List<DataChild> getAllChildrenWithNameInDataAndAttributes(String nameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeAllChildrenWithNameInDataAndAttributes(String childNameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasReadAction() {
		return actions.contains(Action.READ);
	}

	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<DataAtomic> getAllDataAtomicsWithNameInDataAndAttributes(
			String childNameInData, DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStreamId(String streamId) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getStreamId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFileName(String filename) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFileSize(String fileSize) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getFileSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMimeType(String mimeType) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<DataChild> getAllChildrenMatchingFilter(DataChildFilter childFilter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeAllChildrenMatchingFilter(DataChildFilter childFilter) {
		// TODO Auto-generated method stub
		return false;
	}

}
