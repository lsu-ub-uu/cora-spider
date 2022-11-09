/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.data.internal;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RelationalOperator;

public class DataGroupToFilterImp implements DataGroupToFilter {

	private static final String FROM_NO = "fromNo";
	private static final String TO_NO = "toNo";
	private Filter filter;
	private DataGroup dataGroup;

	@Override
	public Filter convert(DataGroup dataGroup) {
		this.dataGroup = dataGroup;
		filter = new Filter();

		possiblySetFromNo();
		possiblySetToNo();
		possiblySetInclude();
		possiblySetExclude();

		return filter;
	}

	private void possiblySetFromNo() {
		if (dataGroup.containsChildWithNameInData(FROM_NO)) {
			String fromNo = dataGroup.getFirstAtomicValueWithNameInData(FROM_NO);
			filter.fromNo = Long.valueOf(fromNo);
		}
	}

	private void possiblySetToNo() {
		if (dataGroup.containsChildWithNameInData(TO_NO)) {
			String toNo = dataGroup.getFirstAtomicValueWithNameInData(TO_NO);
			filter.toNo = Long.valueOf(toNo);
		}
	}

	private void possiblySetInclude() {
		if (dataGroup.containsChildWithNameInData("include")) {
			filter.include = convertParts("include");
		}
	}

	private List<Part> convertParts(String nameInData) {
		DataGroup dataParts = dataGroup.getFirstGroupWithNameInData(nameInData);
		return convertEachPart(dataParts);
	}

	private List<Part> convertEachPart(DataGroup dataParts) {
		List<Part> parts = new ArrayList<>();
		for (DataGroup dataPart : dataParts.getAllGroupsWithNameInData("part")) {
			parts.add(convertPart(dataPart));
		}
		return parts;
	}

	private Part convertPart(DataGroup dataPart) {
		Part part = new Part();
		for (DataChild dataCondition : dataPart.getChildren()) {
			Condition condition = convertAtomicToCondition((DataAtomic) dataCondition);
			part.conditions.add(condition);
		}
		return part;
	}

	private Condition convertAtomicToCondition(DataAtomic atomic) {
		return new Condition(atomic.getNameInData(), RelationalOperator.EQUAL_TO,
				atomic.getValue());
	}

	private void possiblySetExclude() {
		if (dataGroup.containsChildWithNameInData("exclude")) {
			filter.exclude = convertParts("exclude");
		}
	}

}
