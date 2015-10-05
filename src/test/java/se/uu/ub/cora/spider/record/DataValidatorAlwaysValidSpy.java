package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.metadataformat.data.DataElement;
import se.uu.ub.cora.metadataformat.validator.DataValidator;
import se.uu.ub.cora.metadataformat.validator.ValidationAnswer;

public class DataValidatorAlwaysValidSpy implements DataValidator {
	boolean validateDataWasCalled = false;

	@Override
	public ValidationAnswer validateData(String metadataId, DataElement dataGroup) {
		validateDataWasCalled = true;
		return new ValidationAnswer();
	}

}
