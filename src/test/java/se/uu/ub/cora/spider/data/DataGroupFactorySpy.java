package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;

public class DataGroupFactorySpy implements DataGroupFactory {

	@Override
	public DataGroup factorUsingNameInData(String nameInData) {
		return new DataGroupSpy(nameInData);
	}

	@Override
	public DataGroup factorAsLinkWithNameInDataTypeAndId(String nameInData, String recordType,
			String recordId) {
		// TODO Auto-generated method stub
		return null;
	}

}
