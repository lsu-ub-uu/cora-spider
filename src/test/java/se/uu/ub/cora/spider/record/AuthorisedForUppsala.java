package se.uu.ub.cora.spider.record;

import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;

public class AuthorisedForUppsala implements Authorizator {

	@Override
	public boolean isAuthorized(String userId, Set<String> recordCalculateKeys) {
		
		
		//fake uppsala users current keys
		
//		String key = "UPDATE:RECORD_TYPE:SYSTEM:UNIT:*";
		String key = "UPDATE:TYPEWITHUSERGENERATEDID:SYSTEM:UPPSALA:*";
		if(recordCalculateKeys.contains(key)){
			return true;
		}
		return false;
	}

}
