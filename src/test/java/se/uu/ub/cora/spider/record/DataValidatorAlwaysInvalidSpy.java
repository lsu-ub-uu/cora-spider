package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;

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
