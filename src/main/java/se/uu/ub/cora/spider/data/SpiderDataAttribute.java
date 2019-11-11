/*
 * Copyright 2015, 2019 Uppsala University Library
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

import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataAttributeProvider;

public final class SpiderDataAttribute implements SpiderDataElement {

	private String nameInData;
	private String value;

	private SpiderDataAttribute(String nameInData, String value) {
		this.nameInData = nameInData;
		this.value = value;
	}

	public static SpiderDataAttribute fromDataAttribute(DataAttribute dataAttribute) {
		return new SpiderDataAttribute(dataAttribute);
	}

	public static SpiderDataAttribute withNameInDataAndValue(String nameInData, String value) {
		return new SpiderDataAttribute(nameInData, value);
	}

	private SpiderDataAttribute(DataAttribute dataAttribute) {
		nameInData = dataAttribute.getNameInData();
		value = dataAttribute.getValue();
	}

	@Override
	public String getNameInData() {
		return nameInData;
	}

	public String getValue() {
		return value;
	}

	public DataAttribute toDataAttribute() {
		return DataAttributeProvider.getDataAttributeUsingNameInDataAndValue(nameInData, value);
	}

}
