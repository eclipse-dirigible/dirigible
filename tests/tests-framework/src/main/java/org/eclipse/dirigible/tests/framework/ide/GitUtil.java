package org.eclipse.dirigible.tests.framework.ide;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtil {

    private static final Pattern REPO_PATTERN = Pattern.compile("([^/]+?)(?:\\.git)?$");

    public static String extractRepoName(String url) {
        Matcher matcher = REPO_PATTERN.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Failed to extract repository name from url " + url);
        }
    }
}
