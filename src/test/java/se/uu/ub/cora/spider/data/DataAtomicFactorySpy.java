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
import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicFactory;

public class DataAtomicFactorySpy implements DataAtomicFactory {

	public List<String> nameInDatas = new ArrayList<>();
	public List<String> values = new ArrayList<>();
	public String nameInData;
	public String value;
	public DataAtomic reurnedDataAtomic;
	public List<DataAtomic> returnedDataAtomics = new ArrayList<>();

	@Override
	public DataAtomic factorUsingNameInDataAndValue(String nameInData, String value) {
		this.nameInData = nameInData;
		nameInDatas.add(nameInData);
		this.value = value;
		values.add(value);
		reurnedDataAtomic = new DataAtomicSpy(nameInData, value);
		returnedDataAtomics.add(reurnedDataAtomic);
		return reurnedDataAtomic;
	}

	@Override
	public DataAtomic factorUsingNameInDataAndValueAndRepeatId(String nameInData, String value,
			String repeatId) {
		// TODO Auto-generated method stub
		return null;
	}

}
