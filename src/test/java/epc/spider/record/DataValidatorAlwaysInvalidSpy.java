package epc.spider.record;

import epc.metadataformat.data.DataElement;
import epc.metadataformat.validator.DataValidator;
import epc.metadataformat.validator.ValidationAnswer;

public class DataValidatorAlwaysInvalidSpy implements DataValidator {
	public boolean validateDataWasCalled = false;

	@Override
	public ValidationAnswer validateData(String metadataId, DataElement dataGroup) {
		validateDataWasCalled = true;
		ValidationAnswer validationAnswer = new ValidationAnswer();
		validationAnswer.addErrorMessage("Data always invalid");
		return validationAnswer;
	}

}
