package com.andreidodu.jmodules.helper;

import com.andreidodu.jmodules.exception.InvalidJavaVersionException;

public class ValidationUtil {

    public static void validateJavaVersion(String javaVersion) {
        try {
            int javaVersionInteger = Integer.parseInt(javaVersion);
            if (javaVersionInteger < 9) {
                throw new InvalidJavaVersionException("Invalid java version");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid Java version: " + javaVersion);
            throw new InvalidJavaVersionException("Invalid Java version");
        }
    }

}
