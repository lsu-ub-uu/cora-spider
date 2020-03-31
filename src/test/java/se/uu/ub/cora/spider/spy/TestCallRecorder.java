package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCallRecorder {
	private Map<String, List<Map<String, Object>>> calledMethods = new HashMap<>();

	public void addCall(String methodName, Object... parameters) {
		List<Map<String, Object>> list = possiblyAddMethodName(methodName);
		Map<String, Object> parameter = new HashMap<>();
		int position = 0;
		while (position < parameters.length) {
			parameter.put((String) parameters[position], parameters[position + 1]);
			position = position + 2;
		}
		list.add(parameter);
	}

	public int getNumberOfCallsToMethod(String method) {
		return calledMethods.get(method).size();
	}

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
}
