package denominator.ultradns;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.ultradns.UltraDNSRestException.ZONE_NOT_FOUND;
import static feign.Util.UTF_8;

public class UltraDNSRestErrorDecoderTest {

  private ErrorDecoder errors;

  @Rule
  public final ExpectedException thrown = ExpectedException.none();


  @Before
  public void setUp() throws Exception {
    errors = new UltraDNSRestErrorDecoder(new AtomicReference<Boolean>(false));
  }


  static Response errorResponse(String body) {
    return Response.create(500, "Server Error", Collections.<String, Collection<String>>emptyMap(), body, UTF_8);
  }

  @Test
  public void testDecode() throws Exception {
    thrown.expect(FeignException.class);
    thrown.expectMessage("Zone does not exist in the system.");

    throw errors.decode("UltraDNSRest#No Content", errorResponse(UltraDNSMockResponse
            .getMockErrorResponse(ZONE_NOT_FOUND, "Zone does not exist in the system.")));
  }
}
