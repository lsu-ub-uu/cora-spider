/*
 * Copyright 2015 Uppsala University Library
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

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class DataRecordLinkCollectorSpy implements DataRecordLinkCollector {

	public boolean collectLinksWasCalled = false;
	public String metadataId = null;

	public DataGroup collectedDataLinks = new DataGroupSpy("collectedDataLinks");
	public String recordType;
	public String recordId;
	public DataGroup dataGroup;

	@Override
	public DataGroup collectLinks(String metadataId, DataGroup dataGroup, String fromRecordType,
			String fromRecordId) {
		this.metadataId = metadataId;
		this.dataGroup = dataGroup;
		this.recordType = fromRecordType;
		this.recordId = fromRecordId;
		collectLinksWasCalled = true;
		return collectedDataLinks;
	}

}
