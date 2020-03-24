/*
 * Copyright 2017 Uppsala University Library
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
package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class DataGroupTermCollectorSpy implements DataGroupTermCollector {
	public boolean collectTermsWasCalled = false;
	public String metadataId = null;
	public DataGroup dataGroup;

	public DataGroup collectedTerms;
	public List<DataGroup> returnedCollectedTerms = new ArrayList<>();

	public List<DataGroup> dataGroups = new ArrayList<>();
	public Map<String, Integer> metadataIdsReadNumberOfTimesMap = new HashMap<>();

	@Override
	public DataGroup collectTerms(String metadataId, DataGroup dataGroup) {
		if (!metadataIdsReadNumberOfTimesMap.containsKey(metadataId)) {
			metadataIdsReadNumberOfTimesMap.put(metadataId, 1);
		} else {
			metadataIdsReadNumberOfTimesMap.put(metadataId,
					metadataIdsReadNumberOfTimesMap.get(metadataId) + 1);
		}
		this.metadataId = metadataId;
		this.dataGroup = dataGroup;
		collectTermsWasCalled = true;

		dataGroups.add(dataGroup);
		collectedTerms = new DataGroupSpy("collectedData");
		returnedCollectedTerms.add(collectedTerms);
		return collectedTerms;
	}
}
