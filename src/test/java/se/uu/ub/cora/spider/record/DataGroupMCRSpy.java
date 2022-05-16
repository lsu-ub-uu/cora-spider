/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataGroupMCRSpy implements DataGroup {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();
	public String atomicValueToReturn = "";
	// public Map<String, String> atomicValues = new HashMap<>();
	public Map<String, DataGroupMCRSpy> groupValues = new HashMap<>();
	public String nameInData;
	/**
	 * returnValues contains return values for all methods. Key is methodName and value can is a
	 * list of objects.
	 * <p>
	 * If no value exists to return for a method is the simplest possible object returned, an empty
	 * string or a spy object
	 */
	public Map<String, List<Object>> returnValues = new HashMap<>();

	public DataGroupMCRSpy() {
		MCR.useMRV(MRV);
		// MRV.setDefaultReturnValuesSupplier("hasReadAction", (Supplier<Boolean>) () -> false);
		// MRV.setDefaultReturnValuesSupplier("getRepeatId", String::new);
		// MRV.setDefaultReturnValuesSupplier("getNameInData", String::new);
		MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> null);
		MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				(Supplier<Boolean>) () -> false);
		// MRV.setDefaultReturnValuesSupplier("getAttribute", DataAttributeSpy::new);
		// MRV.setDefaultReturnValuesSupplier("getAttributes", ArrayList<DataAttribute>::new);
		// MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", String::new);
		// MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", String::new);
	}

	@Override
	public void setRepeatId(String repeatId) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRepeatId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	@Override
	public boolean hasChildren() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsChildWithNameInData(String nameInData) {
		// MethodMockMeNow
		// MethodReturnValues
		// var returnValue = MRV.getReturnValue(nameInData);

		// int numberToReturn = MCR.getNumberOfCallsToMethod("containsChildWithNameInData");
		// MCR.addCall("nameInData", nameInData);
		//
		// if (returnValues.containsKey("containsChildWithNameInData")) {
		// List<Object> list = returnValues.get("containsChildWithNameInData");
		// Object returnValue = list.get(numberToReturn);
		// MCR.addReturned(returnValue);
		// return (boolean) returnValue;
		// }
		//
		// boolean returnValue = groupValues.containsKey(nameInData)
		// || atomicValues.containsKey(nameInData);
		// MCR.addReturned(returnValue);
		// return returnValue;
		// MRC

		MCR.addCall("nameInData", nameInData);
		boolean returnValue = (boolean) MRV.getReturnValue(nameInData);
		MCR.addReturned(returnValue);
		return returnValue;
	}

	@Override
	public void addChild(DataChild dataElement) {

	}

	@Override
	public void addChildren(Collection<DataChild> dataElements) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<DataChild> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DataChild> getAllChildrenWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DataChild> getAllChildrenWithNameInDataAndAttributes(String nameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataChild getFirstChildWithNameInData(String nameInData) {
		MCR.addCall("nameInData", nameInData);

		return null;
	}

	@Override
	public String getFirstAtomicValueWithNameInData(String nameInData) {
		// MCR.addCall("nameInData", nameInData);
		// String returnValue = atomicValues.get(nameInData);
		// MCR.addReturned(returnValue);
		// return returnValue;
		return (String) MCR.addCallAndReturnFromMRV("nameInData", nameInData);
	}

	@Override
	public List<DataAtomic> getAllDataAtomicsWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataGroup getFirstGroupWithNameInData(String nameInData) {
		MCR.addCall("nameInData", nameInData);
		DataGroupMCRSpy returnValue = groupValues.get(nameInData);

		MCR.addReturned(returnValue);
		return returnValue;
	}

	@Override
	public List<DataGroup> getAllGroupsWithNameInData(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> getAllGroupsWithNameInDataAndAttributes(String childNameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeFirstChildWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAllChildrenWithNameInData(String childNameInData) {
		MCR.addCall("childNameInData", childNameInData);

		return false;
	}

	@Override
	public boolean removeAllChildrenWithNameInDataAndAttributes(String childNameInData,
			DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataAtomic getFirstDataAtomicWithNameInData(String childNameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAttributeByIdWithValue(String nameInData, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataAttribute getAttribute(String nameInData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataAttribute> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataAtomic> getAllDataAtomicsWithNameInDataAndAttributes(
			String childNameInData, DataAttribute... childAttributes) {
		// TODO Auto-generated method stub
		return null;
	}

}
