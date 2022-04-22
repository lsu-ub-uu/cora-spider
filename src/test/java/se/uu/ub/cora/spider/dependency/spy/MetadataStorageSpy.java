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
package se.uu.ub.cora.spider.dependency.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.storage.MetadataStorage;

public class MetadataStorageSpy implements MetadataStorage {

	public List<DataGroup> recordTypes = new ArrayList<>();
	public List<DataGroup> metadataElements = new ArrayList<>();
	public boolean getMetadataElementsWasCalled = false;

	@Override
	public Collection<DataGroup> getMetadataElements() {
		getMetadataElementsWasCalled = true;
		metadataElements = new ArrayList<>();
		DataGroup spyDataGroup = createAndAddMetadataGroupToMetedataElementsUsingId(
				"someMetadata1");

		metadataElements.add(spyDataGroup);
		return metadataElements;
	}

	private DataGroup createAndAddMetadataGroupToMetedataElementsUsingId(String id) {
		DataGroup spyDataGroup = createRecordTypeDataGroup("metadata", id);
		spyDataGroup.addChild(new DataAtomicSpy("nameInData", id));
		spyDataGroup.addAttributeByIdWithValue("type", "textVariable");
		spyDataGroup.addChild(new DataAtomicSpy("regEx", "someRegexp"));
		createAndAddTexts(id, spyDataGroup);
		return spyDataGroup;
	}

	private void createAndAddTexts(String id, DataGroup spyDataGroup) {
		DataGroup textId = new DataGroupSpy("textId");
		textId.addChild(new DataAtomicSpy("linkedRecordId", id + "Text"));
		spyDataGroup.addChild(textId);
		DataGroup defTextId = new DataGroupSpy("defTextId");
		defTextId.addChild(new DataAtomicSpy("linkedRecordId", id + "DefText"));
		spyDataGroup.addChild(defTextId);
	}

	@Override
	public Collection<DataGroup> getPresentationElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> getTexts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> getRecordTypes() {
		recordTypes = new ArrayList<>();
		DataGroup spyDataGroup = createRecordTypeDataGroup("spyDataGroup", "someId1");
		recordTypes.add(spyDataGroup);
		DataGroup spyDataGroup2 = createRecordTypeDataGroup("spyDataGroup", "someId2");
		recordTypes.add(spyDataGroup2);
		return recordTypes;
	}

	private DataGroup createRecordTypeDataGroup(String nameInData, String id) {
		DataGroup spyDataGroup = new DataGroupSpy(nameInData);
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		spyDataGroup.addChild(recordInfo);
		return spyDataGroup;
	}

	@Override
	public Collection<DataGroup> getCollectTerms() {
		// TODO Auto-generated method stub
		return null;
	}

}
