package me.zort.commandlib.util;

import org.apache.commons.lang.ArrayUtils;

public class NamingStrategy {

    public static String javaToHumanConst(String toConvert) {
        String[] parts = new String[0];

        boolean start = true;
        StringBuilder currentPart = new StringBuilder();
        for (char c : toConvert.toCharArray()) {
            if(Character.isUpperCase(c) || start) {
                c = Character.toUpperCase(c);

                if(currentPart.length() > 0) {
                    parts = (String[]) ArrayUtils.add(parts, currentPart.toString());
                    currentPart = new StringBuilder();
                }
            } else {
                c = Character.toLowerCase(c);
            }

            currentPart.append(c);
            start = false;
        }

        if(currentPart.length() > 0) {
            parts = (String[]) ArrayUtils.add(parts, currentPart.toString());
        }

        return String.join(" ", parts);
    }

}
