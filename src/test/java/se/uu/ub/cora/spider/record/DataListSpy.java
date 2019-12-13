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
package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.Data;
import se.uu.ub.cora.data.DataList;

public class DataListSpy implements DataList {

	public List<Data> dataRecords = new ArrayList<>();
	public String fromNo;
	public String toNo;
	public String totalNo;
	private String nameOfDataType;

	public DataListSpy(String nameOfDataType) {
		this.nameOfDataType = nameOfDataType;
	}

	@Override
	public String getFromNo() {
		return fromNo;
	}

	@Override
	public String getToNo() {
		return toNo;
	}

	@Override
	public String getTotalNumberOfTypeInStorage() {
		return totalNo;
	}

	@Override
	public String getContainDataOfType() {
		return nameOfDataType;
	}

	@Override
	public List<Data> getDataList() {
		return dataRecords;
	}

	@Override
	public void addData(Data data) {
		dataRecords.add(data);

	}

	@Override
	public void setFromNo(String fromNo) {
		this.fromNo = fromNo;

	}

	@Override
	public void setToNo(String toNo) {
		this.toNo = toNo;

	}

	@Override
	public void setTotalNo(String totalNo) {
		this.totalNo = totalNo;

	}

}
