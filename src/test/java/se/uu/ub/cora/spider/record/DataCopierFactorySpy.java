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

import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.copier.DataCopier;
import se.uu.ub.cora.data.copier.DataCopierFactory;

public class DataCopierFactorySpy implements DataCopierFactory {
	public List<DataElement> dataElements = new ArrayList<>();
	public List<DataCopierSpy> factoredDataCopiers = new ArrayList<>();

	@Override
	public DataCopier factorForDataElement(DataElement dataElement) {
		dataElements.add(dataElement);
		DataCopierSpy dataCopier = new DataCopierSpy(dataElement);
		factoredDataCopiers.add(dataCopier);
		return dataCopier;
	}

}
