package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

public class ValidationResultTest {

	@Test
	public void testValidationResultValid() {
		ValidationResult validationResult = new ValidationResult();
		assertTrue(validationResult.isValid());
		assertFalse(validationResult.isInvalid());
	}

	@Test
	public void testValidationResultInvalid() {
		ValidationResult validationResult = new ValidationResult();
		validationResult.addErrorMessage("some error message");
		assertFalse(validationResult.isValid());
		assertTrue(validationResult.isInvalid());
	}

	@Test
	public void testValidationResultGetErrorMessages() {
		String errorMessage = "some error message";
		String errorMessage2 = "some error message 2";

		ValidationResult validationResult = new ValidationResult();
		validationResult.addErrorMessage(errorMessage);
		validationResult.addErrorMessage(errorMessage2);

		List<String> errorMessages = validationResult.getErrorMessages();
		assertSame(errorMessages.get(0), errorMessage);
		assertSame(errorMessages.get(1), errorMessage2);
	}

	@Test
	public void testValidationResultAddListOfErrorsGetErrorMessages() {
		List<String> messagesToAdd = new ArrayList<>();
		messagesToAdd.add("some error message");
		messagesToAdd.add("some error message 2");

		ValidationResult validationResult = new ValidationResult();
		validationResult.addErrorMessages(messagesToAdd);

		List<String> errorMessages = validationResult.getErrorMessages();
		assertEquals(errorMessages, messagesToAdd);
	}
}
