package de.frank.jmh.architecture;

import org.apache.commons.lang.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/*--

Throughput - Values in ops/s
                                  20         200        2000 # Datasize
newEachTime                  501.311     502.365     403.488 # new Cipher("SPEC") is VERY expensive
threadLocal_differentKey   1.724.136   1.687.279   1.144.438 # ..once we have dclLazyLoader Cipher object, the "initialize()" is not that expensive
threadLocal_sameKey       34.499.647  12.428.912   1.548.278 # .. except when encrypting small values over and over again. If you can cache the key as well - do it!.


GC Allocation Rate normalized in B/op
                                 20         200         2000 # Datasize
newEachTime                   7.034       7.472       11.002 # as expected, creating dclLazyLoader new Cipher() each call creates dclLazyLoader lot of garbage too..
threadLocal_differentKey        280         632        4.248
threadLocal_sameKey              96         448        4.064

 */

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class CipherCacheThreadLocalJMH {

    private static final ThreadLocal<Cipher> CIPHERS = ThreadLocal.withInitial(CipherCacheThreadLocalJMH::newCipher);
    private static final ThreadLocal<Cipher> CIPHERS_REUSE_KEY = ThreadLocal.withInitial(CipherCacheThreadLocalJMH::newCipherFixedKey);


    @Param({"20", "200", "2000"})
    private int dataLen;

    private byte[] data;
    private byte[] key;

    @Setup
    public void setup() {
        data = RandomStringUtils.randomAlphanumeric(dataLen).getBytes();
        key = new byte[256 / Byte.SIZE];
        new SecureRandom().nextBytes(key);
    }

    private static Cipher newCipher() {
        try {
            return Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher newCipherFixedKey() {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] key = new byte[256 / Byte.SIZE];
            new SecureRandom().nextBytes(key);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return c;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public byte[] newEachTime() throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher cipher = newCipher();
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(data);
    }

    @Benchmark
    public byte[] threadLocal_differentKey() throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher cipher = CIPHERS.get();
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(data);
    }

    @Benchmark
    public byte[] threadLocal_sameKey() throws BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = CIPHERS_REUSE_KEY.get();
        return cipher.doFinal(data);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(CipherCacheThreadLocalJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        CipherCacheThreadLocalJMH.class.getSimpleName()))
                .jvmArgsAppend("-Dcrypto.policy=unlimited") //since java 8u151
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }
}
