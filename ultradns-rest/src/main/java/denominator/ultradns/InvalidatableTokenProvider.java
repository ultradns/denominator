package denominator.ultradns;

import denominator.CheckConnection;
import denominator.Credentials;
import denominator.ultradns.model.TokenResponse;
import denominator.ultradns.util.PropertyUtil;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.common.Preconditions.checkNotNull;

/**
 * gets the last Auth token, expiring if the current time exceed token expiry time.
 */
public class InvalidatableTokenProvider implements Provider<String>, CheckConnection {

    private final denominator.Provider provider;
    private final Session session;
    private final Provider<Credentials> credentials;
    private final AtomicReference<Boolean> sessionValid;
    private final long durationMillis;
    private transient volatile String lastUrl;
    private transient volatile int lastCredentialsHashCode;
    private transient volatile long expirationMillis;
    private transient String token;

    @Inject
    InvalidatableTokenProvider(denominator.Provider provider, Session session,
                               Provider<Credentials> credentials,
                               AtomicReference<Boolean> sessionValid) {
        this.provider = provider;
        this.session = session;
        this.credentials = credentials;
        this.sessionValid = sessionValid;
        this.durationMillis = Long.parseLong(PropertyUtil.getProperty("ultradns.rest.token.expiry.millis"));
    }

    @Override
    public boolean ok() {
        boolean isValid = System.currentTimeMillis() < getExpirationMillis();
        if (!isValid) {
            sessionValid.set(false);
        }
        return isValid;
    }

    @Override
    public String get() {
        String currentUrl = provider.url();
        Credentials currentCreds = credentials.get();
        long currentTime = System.currentTimeMillis();

        if (needsRefresh(currentTime, currentCreds, currentUrl)) {
            setLastUrl(currentUrl);
            setLastCredentialsHashCode(currentCreds.hashCode());
            TokenResponse tokenResponse = auth(currentCreds);
            setExpirationMillis(currentTime + durationMillis);
            String t = tokenResponse.getAccessToken();
            setToken(t);
            sessionValid.set(true);
            return t;
        }
        return getToken();
    }

    private boolean needsRefresh(long currentTime, Credentials currentCreds, String currentUrl) {
        return !sessionValid.get()
            || !currentUrl.equals(getLastUrl()) || currentCreds.hashCode() != getLastCredentialsHashCode()
            || getExpirationMillis() == 0 || currentTime - getExpirationMillis() >= 0;
    }

    private TokenResponse auth(Credentials currentCreds) {
        String username;
        String password;
        if (currentCreds instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listCreds = (List<Object>) currentCreds;
            username = listCreds.get(0).toString();
            password = listCreds.get(1).toString();
        } else if (currentCreds instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapCreds = (Map<String, Object>) currentCreds;
            username = checkNotNull(mapCreds.get("username"), "username").toString();
            password = checkNotNull(mapCreds.get("password"), "password").toString();
        } else {
            throw new IllegalArgumentException("Unsupported credential type: " + currentCreds);
        }
        return session.login("password", username, password);
    }

    interface Session {
        @RequestLine("POST /authorization/token")
        @Headers({ "Content-Type: application/x-www-form-urlencoded" })
        TokenResponse login(@Param("grant_type") String grantType, @Param("username") String userName,
                            @Param("password") String password);
    }

    public String getLastUrl() {
        return lastUrl;
    }

    public void setLastUrl(String lastUrl) {
        this.lastUrl = lastUrl;
    }

    public int getLastCredentialsHashCode() {
        return lastCredentialsHashCode;
    }

    public void setLastCredentialsHashCode(int lastCredentialsHashCode) {
        this.lastCredentialsHashCode = lastCredentialsHashCode;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
