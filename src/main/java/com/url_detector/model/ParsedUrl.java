package com.url_detector.model;

import java.util.List;

public class ParsedUrl {

    private final String rawUrl;
    private final String scheme;       // e.g. "https"
    private final String host;         // full host including subdomains
    private final String registrableDomain;
    private final String tld;         
    private final List<String> subdomains; // labels before registrable domain
    private final String path;
    private final String query;
    private final int port;            // -1 if not present
    private final boolean ipBased;     // true when host is an IP address

    public ParsedUrl(String rawUrl, String scheme, String host,
                     String registrableDomain, String tld, List<String> subdomains,
                     String path, String query, int port, boolean ipBased) {
        this.rawUrl = rawUrl;
        this.scheme = scheme;
        this.host = host;
        this.registrableDomain = registrableDomain;
        this.tld = tld;
        this.subdomains = List.copyOf(subdomains);
        this.path = path;
        this.query = query;
        this.port = port;
        this.ipBased = ipBased;
    }

    public String getRawUrl()           { return rawUrl; }
    public String getScheme()           { return scheme; }
    public String getHost()             { return host; }
    public String getRegistrableDomain(){ return registrableDomain; }
    public String getTld()              { return tld; }
    public List<String> getSubdomains() { return subdomains; }
    public String getPath()             { return path; }
    public String getQuery()            { return query; }
    public int getPort()                { return port; }
    public boolean isIpBased()          { return ipBased; }
}
