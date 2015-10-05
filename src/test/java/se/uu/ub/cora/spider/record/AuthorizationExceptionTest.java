package se.uu.ub.cora.spider.record;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.AuthorizationException;

public class AuthorizationExceptionTest {
	@Test
	public void testInit() {
		AuthorizationException notAuthorized= new AuthorizationException("message");
		
		Assert.assertEquals(notAuthorized.getMessage(), "message");
	}
}
