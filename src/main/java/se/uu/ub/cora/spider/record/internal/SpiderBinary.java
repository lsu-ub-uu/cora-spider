/*
 * Copyright 2018, 2023 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.storage.RecordStorage;

public abstract class SpiderBinary {

	protected RecordStorage recordStorage;
	protected String type;
	protected Authenticator authenticator;
	protected String authToken;
	protected User user;

	public void checkRecordTypeIsBinary() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		if (!recordTypeIsBinary(recordTypeDefinition)) {
			throw new MisuseException("It is only possible to upload files to recordType binary");
		}
	}

	private DataGroup getRecordTypeDefinition() {
		return recordStorage.read(List.of("recordType"), type);
	}

	private boolean recordTypeIsBinary(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		String id = recordInfo.getFirstAtomicValueWithNameInData("id");
		return "binary".equals(id);
	}

	protected String extractDataDividerFromData(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink dataDivider = (DataRecordLink) recordInfo
				.getFirstChildWithNameInData("dataDivider");
		return dataDivider.getLinkedRecordId();
	}

	protected void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}
}
