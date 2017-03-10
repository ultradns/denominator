package denominator.ultradns;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.profile.GeoResourceRecordSetApi;
//import UltraDNSRestErrorDecoder.UltraDNSError;
import feign.Feign;
import feign.Logger;
import feign.Request.Options;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import org.apache.commons.lang.StringUtils;
import denominator.ResourceTypeToValue.ResourceTypes;

import static dagger.Provides.Type.SET;

public class UltraDNSRestProvider extends BasicProvider {

  private final String url;

  public UltraDNSRestProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public UltraDNSRestProvider(String url) {
    this.url = StringUtils.isEmpty(url) ? UltraDNSRestPropertyLoader.getProperty("ultradns.rest.cte") : url;
  }

  @Override
  public String url() {
    return url;
  }

  /**
   * harvested from the {@code RESOURCE RECORD TYPE CODES} section of the SOAP user guide, dated
   * 2012-11-04.
   */
  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();

    types.addAll(Arrays.asList(
            ResourceTypes.A.name(),
            ResourceTypes.AAAA.name(),
            ResourceTypes.CNAME.name(),
            ResourceTypes.HINFO.name(),
            ResourceTypes.MX.name(),
            ResourceTypes.NAPTR.name(),
            ResourceTypes.NS.name(),
            ResourceTypes.PTR.name(),
            ResourceTypes.RP.name(),
            ResourceTypes.SOA.name(),
            ResourceTypes.SPF.name(),
            ResourceTypes.SRV.name(),
            ResourceTypes.TXT.name()
            ));

    return types;
  }

  /**
   * directional pools in ultra have types {@code IPV4} and {@code IPV6} which accept both CNAME and
   * address types.
   */
  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();

    profileToRecordTypes.put("geo", Arrays.asList(
            ResourceTypes.A.name(),
            ResourceTypes.AAAA.name(),
            ResourceTypes.CNAME.name(),
            ResourceTypes.HINFO.name(),
            ResourceTypes.MX.name(),
            ResourceTypes.NAPTR.name(),
            ResourceTypes.PTR.name(),
            ResourceTypes.RP.name(),
            ResourceTypes.SRV.name(),
            ResourceTypes.TXT.name()
    ));

    profileToRecordTypes.put("roundRobin", Arrays.asList(
            ResourceTypes.A.name(),
            ResourceTypes.AAAA.name(),
            ResourceTypes.HINFO.name(),
            ResourceTypes.MX.name(),
            ResourceTypes.NAPTR.name(),
            ResourceTypes.NS.name(),
            ResourceTypes.PTR.name(),
            ResourceTypes.RP.name(),
            ResourceTypes.SPF.name(),
            ResourceTypes.SRV.name(),
            ResourceTypes.TXT.name()
    ));

    return profileToRecordTypes;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
    options.put("password", Arrays.asList("username", "password"));
    return options;
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes = {NothingToClose.class,
          UltraDNSRestGeoSupport.class,
          WeightedUnsupported.class,
          ConcatBasicAndQualifiedResourceRecordSets.class,
          FeignModule.class})
  public static final class Module {

    @Provides
    CheckConnection checkConnection(InvalidatableTokenProvider checkConnection) {
      return checkConnection;
    }

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(
            UltraDNSRestGeoResourceRecordSetApi.Factory in) {
      return in;
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(UltraDNSRestZoneApi api) {
      return api;
    }

    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            UltraDNSRestResourceRecordSetApi.Factory factory) {
      return factory;
    }

    @Provides(type = SET)
    QualifiedResourceRecordSetApi.Factory factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
      return in;
    }
  }

  @dagger.Module(injects = UltraDNSRestResourceRecordSetApi.Factory.class,
          complete = false // doesn't bind Provider used by UltraDNSRestTarget
  )
  public static final class FeignModule {

    @Provides
    @Singleton
    InvalidatableTokenProvider.Session session(Feign feign, SessionTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    @Singleton
    UltraDNSRest ultraDNS(Feign feign, UltraDNSRestTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    @Singleton
    AtomicReference<Boolean> sessionValid() {
      return new AtomicReference<Boolean>(false);
    }

    @Provides
    Logger logger() {
      return new Logger.NoOpLogger();
    }

    @Provides
    Logger.Level logLevel() {
      return Logger.Level.NONE;
    }

    @Provides
    @Singleton
    Feign feign(Logger logger, Logger.Level logLevel, UltraDNSRestErrorDecoder errorDecoder) {

      /**
       * {@link UltraDNSRest#updateDirectionalPoolRecord(UltraDNSRest.DirectionalRecord, UltraDNSRest.DirectionalGroup)} and {@link
       * UltraDNSRest#addDirectionalPoolRecord(UltraDNSRest.DirectionalRecord, UltraDNSRest.DirectionalGroup, String)} can take up
       * to 10 minutes to complete.
       */
      Options options = new Options(
              Integer.parseInt(UltraDNSRestPropertyLoader.getProperty("feign.connect.timeout.millis")),
              Integer.parseInt(UltraDNSRestPropertyLoader.getProperty("feign.read.timeout.millis"))
      );

      return Feign.builder()
              .logger(logger)
              .logLevel(logLevel)
              .options(options)
              .encoder(new GsonEncoder())
              .encoder(new FormEncoder(new GsonEncoder()))
              .decoder(new GsonDecoder())
              .errorDecoder(errorDecoder)
              .build();
    }
  }
}