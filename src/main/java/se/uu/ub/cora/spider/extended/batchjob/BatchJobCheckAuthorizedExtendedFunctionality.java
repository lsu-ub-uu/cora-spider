/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.batchjob;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BatchJobCheckAuthorizedExtendedFunctionality implements ExtendedFunctionality {

	private SpiderDependencyProvider dependencyProvider;

	public BatchJobCheckAuthorizedExtendedFunctionality(
			SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String recordTypeForBatch = getRecordTypeToIndex(data);
		checkUserIsAuthorizedForIndex(data.user, recordTypeForBatch);
	}

	private String getRecordTypeToIndex(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		return dataRecordGroup.getFirstAtomicValueWithNameInData("recordTypeToIndex");
	}

	private void checkUserIsAuthorizedForIndex(User user, String recordTypeForBatch) {
		SpiderAuthorizator authorizator = dependencyProvider.getSpiderAuthorizator();
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, "index", recordTypeForBatch);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

}
