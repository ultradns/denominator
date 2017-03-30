package denominator.ultradns;

import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

import javax.inject.Inject;

class UltraDNSRestTarget implements Target<UltraDNSRest> {

  private final Provider provider;
  private final InvalidatableTokenProvider lazyToken;

  @Inject
  UltraDNSRestTarget(Provider provider, InvalidatableTokenProvider lazyToken) {
    this.provider = provider;
    this.lazyToken = lazyToken;
  }

  @Override
  public Class<UltraDNSRest> type() {
    return UltraDNSRest.class;
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String url() {
    return provider.url();
  }

  @Override
  public Request apply(RequestTemplate in) {
    in.header("Authorization", "Bearer " + lazyToken.get());
    in.insert(0, url());
    return in.request();
  }
}
