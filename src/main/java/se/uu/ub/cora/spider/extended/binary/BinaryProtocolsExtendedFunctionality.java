package se.uu.ub.cora.spider.extended.binary;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BinaryProtocolsExtendedFunctionality implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecord dataRecord = data.dataRecord;
		DataGroup binaryGroup = dataRecord.getDataGroup();
		DataGroup adminInfoGroup = binaryGroup.getFirstGroupWithNameInData("adminInfo");
		String visibility = adminInfoGroup.getFirstAtomicValueWithNameInData("visibility");
		if ("published".equals(visibility))
			dataRecord.addProtocol("iiif");
	}
}
