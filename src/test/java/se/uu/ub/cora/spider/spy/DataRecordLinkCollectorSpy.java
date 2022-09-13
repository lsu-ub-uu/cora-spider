/*
 * Copyright 2015, 2022 Uppsala University Library
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
import java.util.List;
import java.util.function.Supplier;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.RecordToRecordLink;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataRecordLinkCollectorSpy implements DataRecordLinkCollector {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataRecordLinkCollectorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("collectLinks",
				(Supplier<List<RecordToRecordLink>>) () -> new ArrayList<>());
	}

	@Override
	public List<RecordToRecordLink> collectLinks(String metadataId, DataGroup dataGroup,
			String fromRecordType, String fromRecordId) {
		return (List<RecordToRecordLink>) MCR.addCallAndReturnFromMRV("metadataId", metadataId,
				"dataGroup", dataGroup, "fromRecordType", fromRecordType, "fromRecordId",
				fromRecordId);
		// MCR.addCall("metadataId", metadataId, "dataGroup", dataGroup, "fromRecordType",
		// fromRecordType, "fromRecordId", fromRecordId);
		// List<RecordToRecordLink> links = new ArrayList<>();
		// MCR.addReturned(links);
		// return links;
	}

}
