package se.uu.ub.cora.spider.spy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TestCallRecorder is a test helper class used to record and validate calls to methods in spies and
 * similar test helping classes.
 * <p>
 * Spies and similar helper classes should create an internal instance of this class and then record
 * calls to its methods using the {@link #addCall(String, Object...)} method.
 * <p>
 * Tests can then validate correct calls using the {@link #assertParameters(String, int, Object...)
 * method and check the number of calls using the {@link #assertNumberOfCallsToMethod(String, int)}
 * <p>
 * Or get values for a specific call using the {@link #getInParametersAsArray(String, int)} method
 * to use in external asserts and check the number of calls using the
 * {@link #getNumberOfCallsToMethod(String)} method
 */
public class TestCallRecorder {
	private Map<String, List<Map<String, Object>>> calledMethods = new HashMap<>();

	/**
	 * addCall is expected to be used by spies and similar test helper classes to record calls made
	 * to their methods.
	 * 
	 * @param methodName
	 *            A String with the method name
	 * @param parameters
	 *            An Object Varargs with the respective methods and their values. For each parameter
	 *            should first the parameter name, and then the value be added to this call <br>
	 *            Ex: addCall("someMethodName", "parameter1Name", parameter1Value, "parameter2Name",
	 *            parameter2Value )
	 * 
	 */
	public void addCall(String methodName, Object... parameters) {
		List<Map<String, Object>> list = possiblyAddMethodName(methodName);
		Map<String, Object> parameter = new LinkedHashMap<>();
		int position = 0;
		while (position < parameters.length) {
			parameter.put((String) parameters[position], parameters[position + 1]);
			position = position + 2;
		}
		list.add(parameter);
	}

	/**
	 * getNumberOfCallsToMethod is used to get the number of calls made to a method
	 * 
	 * @param methodName
	 *            A String with the method name
	 * @return An int with the number of calls made
	 */
	public int getNumberOfCallsToMethod(String methodName) {
		return calledMethods.get(methodName).size();
	}

	/**
	 * getValueForMethodNameAndCallNumberAndParameterName is use to get the value for a specific
	 * method calls specified parameter
	 * 
	 * @param methodName
	 *            A String with the method name
	 * @param callNumber
	 *            An int with the order number of the call, starting on
	 * @param parameterName
	 *            A String with the parameter name to get the value for
	 * @return An Object with the recorded value
	 */
	public Object getValueForMethodNameAndCallNumberAndParameterName(String methodName,
			int callNumber, String parameterName) {
		List<Map<String, Object>> methodCalls = calledMethods.get(methodName);
		Map<String, Object> parameters = methodCalls.get(callNumber);
		return parameters.get(parameterName);

	}

	private List<Map<String, Object>> possiblyAddMethodName(String methodName) {
		if (!calledMethods.containsKey(methodName)) {
			calledMethods.put(methodName, new ArrayList<>());
		}
		return calledMethods.get(methodName);
	}

	public boolean methodWasCalled(String methodName) {
		return calledMethods.containsKey(methodName);
	}

	public Map<String, Object> getParametersForMethodAndCallNumber(String methodName,
			int callNumber) {
		List<Map<String, Object>> methodCalls = calledMethods.get(methodName);
		return methodCalls.get(callNumber);
	}

	/**
	 * assertParameters is used to validate calls to spies and similar test helpers.
	 * <p>
	 * Strings and Ints are compared using assertEquals
	 * <p>
	 * All other types are compared using assertSame
	 * 
	 * @param methodName
	 *            A String with the methodName to check parameters for
	 * @param callNumber
	 *            An int with the order number of the call, starting on 0
	 * @param expectedValues
	 *            A Varargs Object with the expected parameter values in the order they are used in
	 *            the method.
	 */
	public void assertParameters(String methodName, int callNumber, Object... expectedValues) {
		Object[] inParameters = getInParametersAsArray(methodName, callNumber);

		int position = 0;
		for (Object expectedValue : expectedValues) {
			Object value = inParameters[position];
			assertParameter(expectedValue, value);
			position++;
		}
	}

	private void assertParameter(Object expectedValue, Object value) {
		if (isStringOrInt(expectedValue)) {
			assertEquals(value, expectedValue);
		} else {
			assertSame(value, expectedValue);
		}
	}

	/**
	 * assertParameter is used to validate calls to spies and similar test helpers.
	 * 
	 * @param methodName
	 *            A String with the methodName to check parameters for
	 * @param callNumber
	 *            An int with the order number of the call, starting on 0
	 * @param parameterName
	 *            A String with the parameter name to check the value of
	 * @param expectedValue
	 *            An Object with the expected parameter value
	 */
	public void assertParameter(String methodName, int callNumber, String parameterName,
			Object expectedValue) {

		Object value = getValueForMethodNameAndCallNumberAndParameterName(methodName, callNumber,
				parameterName);

		assertParameter(expectedValue, value);

	}

	/**
	 * It asserts the number of times a method has been called.
	 * 
	 * @param methodName
	 *            Name of the method to assert.
	 * @param calledNumberOfTimes
	 *            Expected number of times that the method has been called.
	 */
	public void assertNumberOfCallsToMethod(String methodName, int calledNumberOfTimes) {
		assertEquals(getNumberOfCallsToMethod(methodName), calledNumberOfTimes);
	}

	private Object[] getInParametersAsArray(String methodName, int callNumber) {
		Object[] inParameters = getParametersForMethodAndCallNumber(methodName, callNumber).values()
				.toArray();
		return inParameters;
	}

	private boolean isStringOrInt(Object assertParameter) {
		return assertParameter instanceof String || isInt(assertParameter);
	}

	private boolean isInt(Object object) {
		try {
			Integer.parseInt((String) object);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

}
