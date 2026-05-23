package com.kiettran.stix.feed.http;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Route key combining HTTP method and path template.
 * Path templates may include {paramName} segments, e.g. /api/v1/indicators/{id}.
 */
public record Route(String method, String pathTemplate, Pattern compiled) {

    private static final Pattern PARAM = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    public static Route of(String method, String pathTemplate) {
        String regex = "^" + PARAM.matcher(pathTemplate)
            .replaceAll(m -> "(?<" + m.group(1) + ">[^/]+)") + "$";
        return new Route(method.toUpperCase(Locale.ROOT), pathTemplate, Pattern.compile(regex));
    }

    public boolean matches(String method, String path) {
        if (!this.method.equalsIgnoreCase(method)) return false;
        return compiled.matcher(path).matches();
    }

    /** Returns matcher with named groups when matched, or null otherwise. */
    public Matcher matcher(String path) {
        Matcher m = compiled.matcher(path);
        return m.matches() ? m : null;
    }
}
