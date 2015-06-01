package epc.spider.record;

import epc.metadataformat.data.DataElement;
import epc.metadataformat.validator.DataValidator;
import epc.metadataformat.validator.ValidationAnswer;

public class DataValidatorAlwaysValidSpy implements DataValidator {
	boolean validateDataWasCalled = false;

	@Override
	public ValidationAnswer validateData(String metadataId, DataElement dataGroup) {
		validateDataWasCalled = true;
		return new ValidationAnswer();
	}

}
