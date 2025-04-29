package de.frank.jmh.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {
    //TEST-Helpers
    private static String randomStringWithTokenWord(int stringLength, String token) {
        Random r = ThreadLocalRandom.current();
        StringBuilder result = randomString(stringLength, r);
        if (StringUtils.isNotEmpty(token)) {
            int replacePos = r.nextInt(stringLength - token.length());
            result.replace(replacePos, replacePos + token.length(), token);
        }
        return result.toString();

    }

    public static StringBuilder randomString(int stringLength, Random r) {
        char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ 01234567890!\"§$%&/()=?ÜÖÄ;:_'*,.-#+'".toCharArray();
        return randomString(stringLength, r, ALPHABET);
    }

    @NotNull
    public static StringBuilder randomString(int stringLength, Random r, char[] ALPHABET) {
        StringBuilder withToken = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            withToken.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return withToken;
    }

    public static Set<String> randomUniqueStrings(int count, int len, char[] alphabet) {
        final long possibilities = (long) Math.pow(alphabet.length, len);

        Set<String> r = new HashSet<>();
        for (int i = 0; i < count; i++) {
            //generate random UNIQUE strings!
            //- we just create a simple permutation of the index within the solution space
            //simple permutation to make tokens more random
            long sample = (i * (Integer.MAX_VALUE - 1L/*a prime*/) + 41L) % possibilities;

            char[] result = new char[len];
            numberToString(sample, alphabet, result);
            r.add(new String(result));
        }

        return r;
    }

    private static void numberToString(long generatedNumber, char[] charset, final char[] result) {
        final int charsetLen = charset.length;
        int pos = result.length - 1;
        while (generatedNumber > 0 && pos >= 0) {
            result[pos--] = charset[(int) (generatedNumber % charsetLen)];
            generatedNumber /= charsetLen;
        }
        // padding
        while (pos >= 0) {
            result[pos--] = charset[0];
        }
    }
}
