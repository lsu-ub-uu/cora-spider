package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
	private List<String> errorMessages = new ArrayList<>();

	public void addErrorMessage(String errorMessage) {
		errorMessages.add(errorMessage);

	}

	public boolean isValid() {
		return errorMessages.isEmpty();
	}

	public boolean isInvalid() {
		return !errorMessages.isEmpty();
	}

	public List<String> getErrorMessages() {
		return copyErrorMessages();
	}

	private List<String> copyErrorMessages() {
		List<String> errorMessagesOut = new ArrayList<>();
		for (String message : errorMessages) {
			errorMessagesOut.add(message);
		}
		return errorMessagesOut;
	}

	public void addErrorMessages(List<String> messagesToAdd) {
		errorMessages.addAll(messagesToAdd);
	}

}
