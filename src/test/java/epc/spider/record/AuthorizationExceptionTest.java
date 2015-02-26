package epc.spider.record;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthorizationExceptionTest {
	@Test
	public void testInit() {
		AuthorizationException notAuthorized= new AuthorizationException("message");
		
		Assert.assertEquals(notAuthorized.getMessage(), "message");
	}
}
