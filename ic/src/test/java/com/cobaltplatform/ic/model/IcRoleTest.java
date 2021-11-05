package com.cobaltplatform.ic.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Not running on CI Server due to HTTP Call")
public class IcRoleTest {
	@Test
	public void decodePatientToken() {
		var token = "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiI2YWRkZWJjYy1mODI2LTQ2ZmQtYjAxYS0yMzE3OTk0OGY3YjMiLCJleHAiOjE2NDY3ODYxNDUsInJvbGVJZCI6IlBBVElFTlQifQ.N2aC8qKyu7EzudqkZXMS-mGZyRMEhGZa5Wm9jWY69iOI-lV-dTnEneObNgwTk8ZfaZJkVfJgMRCA1hX5_1GUlW69uKkHlUng_3rLY5kUCf_HKI0QUAt7uVJ66ZMUKAMKNzT315LBGrM8_zpdq0UJk9u-Hw4pBfGeUheQ3mifhJklP64ThyQXmtR4Fq1UgwDmXZfUSrapPxIYUbHdpL64bLn7WDdGySYnnQydIe-49nyikwh4uuUlPomj0r-s2o62UwEygK118FcGvmMNDKImhFcn3w0lB71NKevfrCe8J53wsOsImyvAVK4Mk95BDeZlOfs8ha5m4Fhx4I4GJZh9NQ";
		try {
			var role = IcRole.getCobaltClaimsFromJWT(token).get().getIcRole();
			assertEquals(role, IcRole.PATIENT);
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void decodeMHICToken() {
		var token = "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJmM2Q2YzdiOC1hYzc0LTQ2NzktYjc4OC01MDJhMjc4MDQ0NzQiLCJleHAiOjE2NDY3ODc4MTksInJvbGVJZCI6Ik1ISUMifQ.FhywQFsxwFX4mAlNloXcmsXrnDeveBYEnYUgKpGuJNwQkCLWBVLDUj_ecHsVduuzfehQEYVn7Z9CmFj3iYSo7BxmG-iP4tuyI4Jrp3FA8sZhUJgWzQ3rYgAdQ_KSonmWRn1dwfsXwrhFNaayZJc3UpQNH0iB7HJc3raU2n5xpj-YS8m2FtMQMbznDpMSYgVRfP8DBW742d5cmJwyC0x58lOYdrE8wfhCJYNTDYof5VgZPGE_ItqgYfGhRy18NflnJJaUAOcmDrg2J7E4aKeQc9k-HGToeRf-vxsNfqxX6M2g8AY1acEN1zWXggybtEcH6sgQVFvVDLNrBDKP4hVqyg";
		try {
			var role = IcRole.getCobaltClaimsFromJWT(token).get().getIcRole();
			assertEquals(role, IcRole.MHIC);
		} catch (Exception e) {
			fail(e);
		}
	}
}
