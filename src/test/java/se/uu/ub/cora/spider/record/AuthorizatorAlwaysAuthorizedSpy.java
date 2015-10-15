package se.uu.ub.cora.spider.record;

import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;

public class AuthorizatorAlwaysAuthorizedSpy implements Authorizator {

	public boolean authorizedWasCalled = false;

	@Override
	public boolean isAuthorized(String userId, Set<String> recordCalculateKeys) {
		authorizedWasCalled = true;
		return true;
	}

}
