/*
 * Copyright 2016 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;

public class DataGroupToRecordEnhancerSpy implements DataGroupToRecordEnhancer {

	public User user;
	public String recordType;
	public DataGroup dataGroup;
	public List<DataGroup> enhancedDataGroups = new ArrayList<>();
	public boolean addReadAction = true;

	@Override
	public SpiderDataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		enhancedDataGroups.add(dataGroup);
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;

		SpiderDataRecord spiderDataGroup = SpiderDataRecord
				.withSpiderDataGroup(SpiderDataGroup.fromDataGroup(dataGroup));
		if (addReadAction) {
			spiderDataGroup.addAction(Action.READ);
		}
		return spiderDataGroup;
	}

}
