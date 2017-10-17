package xlet.android.libraries.network.apirxwrapper;

public interface ApiUrlChecker {
    /**
     * Check if the default header need apply
     *
     * @param targetUrl current api url
     * @return true for using default headers which haven't been overridden
     */
    boolean needAllDefaultHeader(String method, String targetUrl);
}
