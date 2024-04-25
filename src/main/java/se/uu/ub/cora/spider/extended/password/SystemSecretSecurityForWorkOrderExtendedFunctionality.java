/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.password;

import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class SystemSecretSecurityForWorkOrderExtendedFunctionality implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		List<DataRecordLink> recordTypeToSearchInList = getListOfLinkedRecordTypes(data);
		for (DataRecordLink dataRecordLink : recordTypeToSearchInList) {
			throwExceptionIfSearchIsForSystemSecret(dataRecordLink);
		}
	}

	private List<DataRecordLink> getListOfLinkedRecordTypes(ExtendedFunctionalityData data) {
		DataGroup workOrder = data.dataGroup;
		return workOrder.getChildrenOfTypeAndName(DataRecordLink.class, "recordType");
	}

	private void throwExceptionIfSearchIsForSystemSecret(DataRecordLink dataRecordLink) {
		if (linkedRecordTypeIsSystemSecret(dataRecordLink)) {
			throw new AuthorizationException("Access denied");
		}
	}

	private boolean linkedRecordTypeIsSystemSecret(DataRecordLink dataRecordLink) {
		return "systemSecret".equals(dataRecordLink.getLinkedRecordId());
	}

}
