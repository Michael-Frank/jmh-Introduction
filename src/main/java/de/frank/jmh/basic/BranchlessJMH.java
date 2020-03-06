package de.frank.jmh.basic;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;


public class BranchlessJMH {

    public static void main(String[] args) {
        System.out.println(new BranchlessStringUtils().toLowerCaseAsciiChar("aAbBcCzZ"));
    }

    public static class BranchlessStringUtils extends StringUtils {

        @Override
        public char toUpperAsciiInvariant(char c) {
            int maskA = 'a' - c - 1;
            int maskB = c - 'z' - 1;
            int mask = (maskA & maskB) >> 16;
            return (char) (c ^ (mask & 0x20));
        }

        @Override
        public byte toUpperAsciiInvariant(byte c) {
            int maskA = 'a' - c - 1;
            int maskB = c - 'z' - 1;
            int mask = (maskA & maskB) >> 16;
            return (byte) (c ^ (mask & 0x20));

        }

        @Override
        public byte toLowerAsciiInvariant(byte c) {
            int maskA = 'A' - c - 1;
            int maskZ = c - 'Z' - 1;
            int mask = (maskA & maskZ) >> 16;
            return (byte) (c ^ (mask & 0x20));
        }

        @Override
        public char toLowerAsciiInvariant(char c) {
            int maskA = 'A' - c - 1;
            int maskZ = c - 'Z' - 1;
            int mask = (maskA & maskZ) >> 16;
            return (char) (c ^ (mask & 0x20));
        }
    }

    public static class StringUtils {


        static final MethodHandle ALLOCATE_UNINITIALIZED_ARRAY;

        static {
            ALLOCATE_UNINITIALIZED_ARRAY = mkHandleAllocateUninitializedArray();
        }

        private static MethodHandle mkHandleAllocateUninitializedArray() {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                return lookup.findVirtual(unsafeClass, "allocateUni", MethodType.methodType(Object.class, Class.class, int.class)).bindTo(findUnsafe(unsafeClass));
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException e1) {
                throw new ExceptionInInitializerError(e1);
            }

        }

        private static Object findUnsafe(Class<?> unsafeClass) throws IllegalAccessException {
            Object found = null;
            for (Field field : unsafeClass.getDeclaredFields()) {
                if (field.getType() == unsafeClass) {
                    field.setAccessible(true);
                    found = field.get(null);
                    break;
                }
            }
            if (found == null) throw new IllegalStateException("No instance of Unsafe found");
            return found;
        }


        public String toLowerCaseAsciiBytes(String in) {
            byte[] newX = in.getBytes();
            toLowerCaseAscii(newX);
            try {
                return new String(newX, StandardCharsets.US_ASCII.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("should never happen", e);
            }
        }

        public String toLowerCaseAsciiChar(String in) {
            char[] newX = in.toCharArray();
            toLowerCaseAscii(newX);
            return new String(newX);
        }

        public char[] toLowerCaseAsciiCharArrayInplace(String in) {
            char[] out = new char[0];
            try {
                out = (char[]) ALLOCATE_UNINITIALIZED_ARRAY.invoke(char[].class, in.length());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            for (int i = 0; i < in.length(); i++) {
                out[i] = toLowerAsciiInvariant(in.charAt(i));
            }
            return out;
        }

        public void toLowerCaseAscii(char[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }


        public void toLowerCaseAscii(byte[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }

        public byte toLowerAsciiInvariant(byte c) {
            if (c - 'A' <= 'Z' - 'A') {
                c = (byte) (c | 0x20);
            }
            return c;
        }

        public char toLowerAsciiInvariant(char c) {
            if (c - 'A' <= 'Z' - 'A') {
                c = (char) (c | 0x20);
            }
            return c;
        }

        public char toUpperAsciiInvariant(char c) {
            if ((c - 'a') <= ('z' - 'a')) {
                c = (char) (c & ~0x20);
            }
            return c;
        }

        public byte toUpperAsciiInvariant(byte c) {
            if ((c - 'a') <= ('z' - 'a')) {
                c = (byte) (c & ~0x20);
            }
            return c;
        }

        public static boolean isAscii(char c) {
            return c < 0x80;
        }
    }

}
