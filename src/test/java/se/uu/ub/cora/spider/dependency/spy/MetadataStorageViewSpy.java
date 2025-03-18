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
import java.util.Optional;

import se.uu.ub.cora.bookkeeper.metadata.CollectTermHolder;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageView;
import se.uu.ub.cora.bookkeeper.validator.ValidationType;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;

public class MetadataStorageViewSpy implements MetadataStorageView {

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
		spyDataGroup.addChild(new DataAtomicOldSpy("nameInData", id));
		spyDataGroup.addAttributeByIdWithValue("type", "textVariable");
		spyDataGroup.addChild(new DataAtomicOldSpy("regEx", "someRegexp"));
		createAndAddTexts(id, spyDataGroup);
		return spyDataGroup;
	}

	private void createAndAddTexts(String id, DataGroup spyDataGroup) {
		DataGroup textId = new DataGroupOldSpy("textId");
		textId.addChild(new DataAtomicOldSpy("linkedRecordId", id + "Text"));
		spyDataGroup.addChild(textId);
		DataGroup defTextId = new DataGroupOldSpy("defTextId");
		defTextId.addChild(new DataAtomicOldSpy("linkedRecordId", id + "DefText"));
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
		DataGroup spyDataGroup = new DataGroupOldSpy(nameInData);
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", id));
		spyDataGroup.addChild(recordInfo);
		return spyDataGroup;
	}

	@Override
	public Collection<ValidationType> getValidationTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<ValidationType> getValidationType(String validationId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Collection<DataGroup> getCollectTermsAsDataGroup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CollectTermHolder getCollectTermHolder() {
		// TODO Auto-generated method stub
		return null;
	}

}
