package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.results.format.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.concurrent.*;

/*--

 protobuf generates a *Bytes() version for every String and uses it in
   public void writeTo(com.google.protobuf.CodedOutputStream output)
   and
   public int getSerializedSize()

 The "Bytes()" versions create and return a new unnecessary protobuf ByteString object for each String, as the CodedOutstream can already convert java String to bytes.
 To add insult to the injury:
   - ByteString.copyFromUtf8(someJavaString) uses someJavaString.getBytes(DefaultCharsets.UT8) - which bypasses the caching
   of Strings's Charsetencoder, forcing the creation of new charsetencoders on every invocation.
   - The CodedOutputStream already has an optimized version of writing java strings to an stream with ZERO allocation.

          Mode  Cnt     Score     Error   Units      gc.aloc.rate.norm
 orig     avgt   10    25.203 ±   2.315   ns/op    40.000 ± 0.001 B/op
 fixed    avgt   10    49.612 ±   2.694   ns/op   120.000 ± 0.001 B/op

Benchmark           Mode  Cnt     Score     Error   Units   gc.aloc.rate.norm
Probufcrap.orig     avgt   10    24.643 ±   0.976   ns/op    40.000 B/op
Probufcrap.useUtf8  avgt   10    37.525 ±   1.203   ns/op    40.000 B/op # use ByteString.copyFrom(d,"UTF-8"); instead of ByteString.copyFromUtf8(d); to facilitate javas String internal stringcoder caching
Probufcrap.fixed    avgt   10    52.617 ±   1.715   ns/op   120.000 B/op # do not allocate ByteString at all using GenerateMessagev3 and CodingOutputstreams internal writeString() implementation

Benchmark           Mode  Cnt     Score     Error   Units
Probufcrap.orig     avgt   30    34.654 ±   2.571   ns/op    66.667 ±  12.813 B/op
Probufcrap.useUtf8  avgt   30    34.692 ±   3.483   ns/op    66.667 ±  12.813 B/op
Probufcrap.fixed    avgt   30    57.523 ±   1.891   ns/op   120.000 ±   0.001 B/op
Benchmark           Mode  Cnt     Score     Error   Units
Probufcrap.orig     avgt   30    34.836 ±   4.424   ns/op    80.000 ±   0.001 B/op
Probufcrap.useUtf8  avgt   30    33.190 ±   4.378   ns/op    66.667 ±  12.813 B/op
Probufcrap.fixed    avgt   30    55.540 ±   9.154   ns/op   120.000 ±   0.001 B/op




Trhougput@16Threads Benchmark                         Mode  Cnt          Score          Error   Units
Probufcrap.orig                                      thrpt   30  206737254.876 ± 30431804.846   ops/s
Probufcrap.orig:·gc.alloc.rate                       thrpt   30       8254.568 ±      656.746  MB/sec
Probufcrap.orig:·gc.alloc.rate.norm                  thrpt   30         66.667 ±       12.813    B/op
Probufcrap.orig:·gc.churn.PS_Eden_Space              thrpt   30       8321.621 ±      690.901  MB/sec
Probufcrap.orig:·gc.churn.PS_Eden_Space.norm         thrpt   30         67.238 ±       13.073    B/op
Probufcrap.orig:·gc.churn.PS_Survivor_Space          thrpt   30          0.207 ±        0.036  MB/sec
Probufcrap.orig:·gc.churn.PS_Survivor_Space.norm     thrpt   30          0.002 ±        0.001    B/op
Probufcrap.orig:·gc.count                            thrpt   30        504.000                 counts
Probufcrap.orig:·gc.time                             thrpt   30        300.000                     ms
Probufcrap.useUtf8                                   thrpt   30  176282608.979 ±   894981.075   ops/s
Probufcrap.useUtf8:·gc.alloc.rate                    thrpt   30       8990.624 ±       49.028  MB/sec
Probufcrap.useUtf8:·gc.alloc.rate.norm               thrpt   30         80.000 ±        0.001    B/op
Probufcrap.useUtf8:·gc.churn.PS_Eden_Space           thrpt   30       9051.136 ±      202.344  MB/sec
Probufcrap.useUtf8:·gc.churn.PS_Eden_Space.norm      thrpt   30         80.537 ±        1.713    B/op
Probufcrap.useUtf8:·gc.churn.PS_Survivor_Space       thrpt   30          0.210 ±        0.029  MB/sec
Probufcrap.useUtf8:·gc.churn.PS_Survivor_Space.norm  thrpt   30          0.002 ±        0.001    B/op
Probufcrap.useUtf8:·gc.count                         thrpt   30        487.000                 counts
Probufcrap.useUtf8:·gc.time                          thrpt   30        296.000                     ms
Probufcrap.fixed                                     thrpt   30  108773356.564 ±  4407917.497   ops/s
Probufcrap.fixed:·gc.alloc.rate                      thrpt   30       8339.544 ±      326.827  MB/sec
Probufcrap.fixed:·gc.alloc.rate.norm                 thrpt   30        120.000 ±        0.001    B/op
Probufcrap.fixed:·gc.churn.PS_Eden_Space             thrpt   30       8408.521 ±      375.080  MB/sec
Probufcrap.fixed:·gc.churn.PS_Eden_Space.norm        thrpt   30        120.984 ±        2.317    B/op
Probufcrap.fixed:·gc.churn.PS_Survivor_Space         thrpt   30          0.217 ±        0.037  MB/sec
Probufcrap.fixed:·gc.churn.PS_Survivor_Space.norm    thrpt   30          0.003 ±        0.001    B/op
Probufcrap.fixed:·gc.count                           thrpt   30        483.000                 counts
Probufcrap.fixed:·gc.time                            thrpt   30        303.000                     ms
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = "-XX:+UseParallelGC")
@Threads(16)
@State(Scope.Thread)
public class ProtobufPatchesExperimentJMH {

    private Version_Fixed.GetDataVersionResponse fixed;
    private Version_Orig.GetDataVersionResponse orig;
    private Version_UseUtf8String.GetDataVersionResponse useUTF8;

    private BlackHoleOutputStream blackholeOut;

    public static void main(String[] args) throws RunnerException, IOException {
        Version_Fixed.GetDataVersionResponse fixed = Version_Fixed.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        Version_Orig.GetDataVersionResponse orig = Version_Orig.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        Version_UseUtf8String.GetDataVersionResponse useUTF8 = Version_UseUtf8String.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        //fixed.writeDelimitedTo(new NullOutputStream());
        //orig.writeDelimitedTo(new NullOutputStream());
        //useUTF8.writeDelimitedTo(new NullOutputStream());


        Options opt = new OptionsBuilder()
                .include(ProtobufPatchesExperimentJMH.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ProtobufPatchesExperimentJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }


    @Setup
    public void setup(Blackhole blackhole) {
        fixed = Version_Fixed.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        orig = Version_Orig.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        useUTF8 = Version_UseUtf8String.GetDataVersionResponse.newBuilder().setVersion("abcdefg").build();
        blackholeOut = new BlackHoleOutputStream(blackhole);
    }


    @Benchmark
    public void orig() throws IOException {
        orig.writeDelimitedTo(blackholeOut);
    }

    @Benchmark
    public void fixed() throws IOException {
        fixed.writeDelimitedTo(blackholeOut);
    }

    @Benchmark
    public void useUtf8() throws IOException {
        useUTF8.writeDelimitedTo(blackholeOut);
    }

    private static class BlackHoleOutputStream extends OutputStream {
        private final Blackhole blackhole;

        BlackHoleOutputStream(Blackhole b) {
            this.blackhole = b;
        }

        @Override
        public void write(int b) {
            blackhole.consume(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            blackhole.consume(b);
        }

        @Override
        public void write(byte[] b) {
            blackhole.consume(b);
        }
    }

    public static final class Version_Fixed {
        private Version_Fixed() {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistryLite registry) {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistry registry) {
            registerAllExtensions(
                    (com.google.protobuf.ExtensionRegistryLite) registry);
        }

        public interface GetDataVersionRequestOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionRequest)
                com.google.protobuf.MessageOrBuilder {
        }

        /**
         * <pre>
         * A request for the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionRequest}
         */
        public static final class GetDataVersionRequest extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionRequest)
                GetDataVersionRequestOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionRequest.newBuilder() to construct.
            private GetDataVersionRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionRequest() {
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionRequest(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_Fixed.internal_static_GetDataVersionRequest_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_Fixed.internal_static_GetDataVersionRequest_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_Fixed.GetDataVersionRequest.class, Version_Fixed.GetDataVersionRequest.Builder.class);
            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {
                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_Fixed.GetDataVersionRequest)) {
                    return super.equals(obj);
                }
                Version_Fixed.GetDataVersionRequest other = (Version_Fixed.GetDataVersionRequest) obj;

                boolean result = true;
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionRequest parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionRequest parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_Fixed.GetDataVersionRequest prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A request for the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionRequest}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionRequest)
                    Version_Fixed.GetDataVersionRequestOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_Fixed.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_Fixed.internal_static_GetDataVersionRequest_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_Fixed.GetDataVersionRequest.class, Version_Fixed.GetDataVersionRequest.Builder.class);
                }

                // Construct using Version.GetDataVersionRequest.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_Fixed.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionRequest getDefaultInstanceForType() {
                    return Version_Fixed.GetDataVersionRequest.getDefaultInstance();
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionRequest build() {
                    Version_Fixed.GetDataVersionRequest result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionRequest buildPartial() {
                    Version_Fixed.GetDataVersionRequest result = new Version_Fixed.GetDataVersionRequest(this);
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_Fixed.GetDataVersionRequest) {
                        return mergeFrom((Version_Fixed.GetDataVersionRequest) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_Fixed.GetDataVersionRequest other) {
                    if (other == Version_Fixed.GetDataVersionRequest.getDefaultInstance()) return this;
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_Fixed.GetDataVersionRequest parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_Fixed.GetDataVersionRequest) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionRequest)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionRequest)
            private static final Version_Fixed.GetDataVersionRequest DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_Fixed.GetDataVersionRequest();
            }

            public static Version_Fixed.GetDataVersionRequest getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionRequest>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionRequest>() {
                @java.lang.Override
                public GetDataVersionRequest parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionRequest(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionRequest> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionRequest> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_Fixed.GetDataVersionRequest getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        public interface GetDataVersionResponseOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionResponse)
                com.google.protobuf.MessageOrBuilder {

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            java.lang.String getVersion();

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            com.google.protobuf.ByteString
            getVersionBytes();
        }

        /**
         * <pre>
         * A response that contains the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionResponse}
         */
        public static final class GetDataVersionResponse extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionResponse)
                GetDataVersionResponseOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionResponse.newBuilder() to construct.
            private GetDataVersionResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionResponse() {
                version_ = "";
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionResponse(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            case 10: {
                                java.lang.String s = input.readStringRequireUtf8();

                                version_ = s;
                                break;
                            }
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_Fixed.internal_static_GetDataVersionResponse_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_Fixed.internal_static_GetDataVersionResponse_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_Fixed.GetDataVersionResponse.class, Version_Fixed.GetDataVersionResponse.Builder.class);
            }

            public static final int VERSION_FIELD_NUMBER = 1;
            private java.lang.String version_;

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public java.lang.String getVersion() {
                return version_;
            }

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public com.google.protobuf.ByteString
            getVersionBytes() {
                throw new RuntimeException("Must not be called");

            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                if (version_ != null && !version_.isEmpty()) {
                    com.google.protobuf.GeneratedMessageV3.writeString(output, 1, version_);
                }
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {
                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                if (version_ != null && !version_.isEmpty()) {
                    size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, version_);
                }
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_Fixed.GetDataVersionResponse)) {
                    return super.equals(obj);
                }
                Version_Fixed.GetDataVersionResponse other = (Version_Fixed.GetDataVersionResponse) obj;

                boolean result = true;
                result = result && getVersion()
                        .equals(other.getVersion());
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (37 * hash) + VERSION_FIELD_NUMBER;
                hash = (53 * hash) + getVersion().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionResponse parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionResponse parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Fixed.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_Fixed.GetDataVersionResponse prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A response that contains the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionResponse}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionResponse)
                    Version_Fixed.GetDataVersionResponseOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_Fixed.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_Fixed.internal_static_GetDataVersionResponse_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_Fixed.GetDataVersionResponse.class, Version_Fixed.GetDataVersionResponse.Builder.class);
                }

                // Construct using Version.GetDataVersionResponse.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    version_ = "";

                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_Fixed.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionResponse getDefaultInstanceForType() {
                    return Version_Fixed.GetDataVersionResponse.getDefaultInstance();
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionResponse build() {
                    Version_Fixed.GetDataVersionResponse result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_Fixed.GetDataVersionResponse buildPartial() {
                    Version_Fixed.GetDataVersionResponse result = new Version_Fixed.GetDataVersionResponse(this);
                    result.version_ = version_;
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_Fixed.GetDataVersionResponse) {
                        return mergeFrom((Version_Fixed.GetDataVersionResponse) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_Fixed.GetDataVersionResponse other) {
                    if (other == Version_Fixed.GetDataVersionResponse.getDefaultInstance()) return this;
                    if (!other.getVersion().isEmpty()) {
                        version_ = other.version_;
                        onChanged();
                    }
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_Fixed.GetDataVersionResponse parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_Fixed.GetDataVersionResponse) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                private String version_ = "";

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public java.lang.String getVersion() {
                    java.lang.Object ref = version_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        version_ = s;
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public com.google.protobuf.ByteString
                getVersionBytes() {
                    throw new UnsupportedOperationException();
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersion(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    version_ = value;
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder clearVersion() {

                    version_ = getDefaultInstance().getVersion();
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersionBytes(
                        com.google.protobuf.ByteString value) {
                    throw new UnsupportedOperationException();
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionResponse)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionResponse)
            private static final Version_Fixed.GetDataVersionResponse DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_Fixed.GetDataVersionResponse();
            }

            public static Version_Fixed.GetDataVersionResponse getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionResponse>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionResponse>() {
                @java.lang.Override
                public GetDataVersionResponse parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionResponse(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionResponse> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionResponse> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_Fixed.GetDataVersionResponse getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionRequest_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionRequest_fieldAccessorTable;
        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionResponse_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionResponse_fieldAccessorTable;

        public static com.google.protobuf.Descriptors.FileDescriptor
        getDescriptor() {
            return descriptor;
        }

        private static com.google.protobuf.Descriptors.FileDescriptor
                descriptor;

        static {
            java.lang.String[] descriptorData = {
                    "\n\rversion.proto\"\027\n\025GetDataVersionRequest" +
                            "\")\n\026GetDataVersionResponse\022\017\n\007version\030\001 " +
                            "\001(\tB/\n-com.bmw.psmg.hub.internalservices" +
                            ".api.versionb\006proto3"
            };
            com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                    new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                        public com.google.protobuf.ExtensionRegistry assignDescriptors(
                                com.google.protobuf.Descriptors.FileDescriptor root) {
                            descriptor = root;
                            return null;
                        }
                    };
            com.google.protobuf.Descriptors.FileDescriptor
                    .internalBuildGeneratedFileFrom(descriptorData,
                            new com.google.protobuf.Descriptors.FileDescriptor[]{
                            }, assigner);
            internal_static_GetDataVersionRequest_descriptor =
                    getDescriptor().getMessageTypes().get(0);
            internal_static_GetDataVersionRequest_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionRequest_descriptor,
                    new java.lang.String[]{});
            internal_static_GetDataVersionResponse_descriptor =
                    getDescriptor().getMessageTypes().get(1);
            internal_static_GetDataVersionResponse_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionResponse_descriptor,
                    new java.lang.String[]{"Version",});
        }

        // @@protoc_insertion_point(outer_class_scope)
    }


    public static final class Version_UseUtf8String {
        private Version_UseUtf8String() {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistryLite registry) {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistry registry) {
            registerAllExtensions(
                    (com.google.protobuf.ExtensionRegistryLite) registry);
        }

        public interface GetDataVersionRequestOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionRequest)
                com.google.protobuf.MessageOrBuilder {
        }

        /**
         * <pre>
         * A request for the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionRequest}
         */
        public static final class GetDataVersionRequest extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionRequest)
                GetDataVersionRequestOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionRequest.newBuilder() to construct.
            private GetDataVersionRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionRequest() {
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionRequest(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_UseUtf8String.internal_static_GetDataVersionRequest_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_UseUtf8String.internal_static_GetDataVersionRequest_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_UseUtf8String.GetDataVersionRequest.class, Version_UseUtf8String.GetDataVersionRequest.Builder.class);
            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {
                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_UseUtf8String.GetDataVersionRequest)) {
                    return super.equals(obj);
                }
                Version_UseUtf8String.GetDataVersionRequest other = (Version_UseUtf8String.GetDataVersionRequest) obj;

                boolean result = true;
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_UseUtf8String.GetDataVersionRequest prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A request for the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionRequest}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionRequest)
                    Version_UseUtf8String.GetDataVersionRequestOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_UseUtf8String.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_UseUtf8String.internal_static_GetDataVersionRequest_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_UseUtf8String.GetDataVersionRequest.class, Version_UseUtf8String.GetDataVersionRequest.Builder.class);
                }

                // Construct using Version.GetDataVersionRequest.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_UseUtf8String.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionRequest getDefaultInstanceForType() {
                    return Version_UseUtf8String.GetDataVersionRequest.getDefaultInstance();
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionRequest build() {
                    Version_UseUtf8String.GetDataVersionRequest result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionRequest buildPartial() {
                    Version_UseUtf8String.GetDataVersionRequest result = new Version_UseUtf8String.GetDataVersionRequest(this);
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_UseUtf8String.GetDataVersionRequest) {
                        return mergeFrom((Version_UseUtf8String.GetDataVersionRequest) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_UseUtf8String.GetDataVersionRequest other) {
                    if (other == Version_UseUtf8String.GetDataVersionRequest.getDefaultInstance()) return this;
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_UseUtf8String.GetDataVersionRequest parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_UseUtf8String.GetDataVersionRequest) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionRequest)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionRequest)
            private static final Version_UseUtf8String.GetDataVersionRequest DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_UseUtf8String.GetDataVersionRequest();
            }

            public static Version_UseUtf8String.GetDataVersionRequest getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionRequest>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionRequest>() {
                @java.lang.Override
                public GetDataVersionRequest parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionRequest(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionRequest> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionRequest> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_UseUtf8String.GetDataVersionRequest getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        public interface GetDataVersionResponseOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionResponse)
                com.google.protobuf.MessageOrBuilder {

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            java.lang.String getVersion();

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            com.google.protobuf.ByteString
            getVersionBytes();
        }

        /**
         * <pre>
         * A response that contains the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionResponse}
         */
        public static final class GetDataVersionResponse extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionResponse)
                GetDataVersionResponseOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionResponse.newBuilder() to construct.
            private GetDataVersionResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionResponse() {
                version_ = "";
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionResponse(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            case 10: {
                                java.lang.String s = input.readStringRequireUtf8();

                                version_ = s;
                                break;
                            }
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_UseUtf8String.internal_static_GetDataVersionResponse_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_UseUtf8String.internal_static_GetDataVersionResponse_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_UseUtf8String.GetDataVersionResponse.class, Version_UseUtf8String.GetDataVersionResponse.Builder.class);
            }

            public static final int VERSION_FIELD_NUMBER = 1;
            private volatile java.lang.Object version_;

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public java.lang.String getVersion() {
                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    version_ = s;
                    return s;
                }
            }

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public com.google.protobuf.ByteString
            getVersionBytes() {

                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b;
                    try {
                        b = com.google.protobuf.ByteString.copyFrom((String) ref, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("will never happen");
                    }
                    version_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {

                if (!getVersionBytes().isEmpty()) {
                    com.google.protobuf.GeneratedMessageV3.writeString(output, 1, version_);
                }
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {

                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                if (!getVersionBytes().isEmpty()) {
                    size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, version_);
                }
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_UseUtf8String.GetDataVersionResponse)) {
                    return super.equals(obj);
                }
                Version_UseUtf8String.GetDataVersionResponse other = (Version_UseUtf8String.GetDataVersionResponse) obj;

                boolean result = true;
                result = result && getVersion()
                        .equals(other.getVersion());
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (37 * hash) + VERSION_FIELD_NUMBER;
                hash = (53 * hash) + getVersion().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_UseUtf8String.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_UseUtf8String.GetDataVersionResponse prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A response that contains the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionResponse}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionResponse)
                    Version_UseUtf8String.GetDataVersionResponseOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_UseUtf8String.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_UseUtf8String.internal_static_GetDataVersionResponse_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_UseUtf8String.GetDataVersionResponse.class, Version_UseUtf8String.GetDataVersionResponse.Builder.class);
                }

                // Construct using Version.GetDataVersionResponse.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    version_ = "";

                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_UseUtf8String.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionResponse getDefaultInstanceForType() {
                    return Version_UseUtf8String.GetDataVersionResponse.getDefaultInstance();
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionResponse build() {
                    Version_UseUtf8String.GetDataVersionResponse result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_UseUtf8String.GetDataVersionResponse buildPartial() {
                    Version_UseUtf8String.GetDataVersionResponse result = new Version_UseUtf8String.GetDataVersionResponse(this);
                    result.version_ = version_;
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_UseUtf8String.GetDataVersionResponse) {
                        return mergeFrom((Version_UseUtf8String.GetDataVersionResponse) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_UseUtf8String.GetDataVersionResponse other) {
                    if (other == Version_UseUtf8String.GetDataVersionResponse.getDefaultInstance()) return this;
                    if (!other.getVersion().isEmpty()) {
                        version_ = other.version_;
                        onChanged();
                    }
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_UseUtf8String.GetDataVersionResponse parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_UseUtf8String.GetDataVersionResponse) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                private java.lang.Object version_ = "";

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public java.lang.String getVersion() {
                    java.lang.Object ref = version_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        version_ = s;
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public com.google.protobuf.ByteString
                getVersionBytes() {
                    java.lang.Object ref = version_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b;
                        try {
                            b = com.google.protobuf.ByteString.copyFrom((String) ref, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException("will never happen");
                        }
                        version_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersion(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    version_ = value;
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder clearVersion() {

                    version_ = getDefaultInstance().getVersion();
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersionBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    checkByteStringIsUtf8(value);

                    version_ = value;
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionResponse)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionResponse)
            private static final Version_UseUtf8String.GetDataVersionResponse DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_UseUtf8String.GetDataVersionResponse();
            }

            public static Version_UseUtf8String.GetDataVersionResponse getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionResponse>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionResponse>() {
                @java.lang.Override
                public GetDataVersionResponse parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionResponse(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionResponse> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionResponse> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_UseUtf8String.GetDataVersionResponse getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionRequest_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionRequest_fieldAccessorTable;
        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionResponse_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionResponse_fieldAccessorTable;

        public static com.google.protobuf.Descriptors.FileDescriptor
        getDescriptor() {
            return descriptor;
        }

        private static com.google.protobuf.Descriptors.FileDescriptor
                descriptor;

        static {
            java.lang.String[] descriptorData = {
                    "\n\rversion.proto\"\027\n\025GetDataVersionRequest" +
                            "\")\n\026GetDataVersionResponse\022\017\n\007version\030\001 " +
                            "\001(\tB/\n-com.bmw.psmg.hub.internalservices" +
                            ".api.versionb\006proto3"
            };
            com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                    new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                        public com.google.protobuf.ExtensionRegistry assignDescriptors(
                                com.google.protobuf.Descriptors.FileDescriptor root) {
                            descriptor = root;
                            return null;
                        }
                    };
            com.google.protobuf.Descriptors.FileDescriptor
                    .internalBuildGeneratedFileFrom(descriptorData,
                            new com.google.protobuf.Descriptors.FileDescriptor[]{
                            }, assigner);
            internal_static_GetDataVersionRequest_descriptor =
                    getDescriptor().getMessageTypes().get(0);
            internal_static_GetDataVersionRequest_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionRequest_descriptor,
                    new java.lang.String[]{});
            internal_static_GetDataVersionResponse_descriptor =
                    getDescriptor().getMessageTypes().get(1);
            internal_static_GetDataVersionResponse_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionResponse_descriptor,
                    new java.lang.String[]{"Version",});
        }

        // @@protoc_insertion_point(outer_class_scope)
    }


    public static final class Version_Orig {
        private Version_Orig() {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistryLite registry) {
        }

        public static void registerAllExtensions(
                com.google.protobuf.ExtensionRegistry registry) {
            registerAllExtensions(
                    (com.google.protobuf.ExtensionRegistryLite) registry);
        }

        public interface GetDataVersionRequestOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionRequest)
                com.google.protobuf.MessageOrBuilder {
        }

        /**
         * <pre>
         * A request for the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionRequest}
         */
        public static final class GetDataVersionRequest extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionRequest)
                GetDataVersionRequestOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionRequest.newBuilder() to construct.
            private GetDataVersionRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionRequest() {
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionRequest(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_Orig.internal_static_GetDataVersionRequest_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_Orig.internal_static_GetDataVersionRequest_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_Orig.GetDataVersionRequest.class, Version_Orig.GetDataVersionRequest.Builder.class);
            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {
                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_Orig.GetDataVersionRequest)) {
                    return super.equals(obj);
                }
                Version_Orig.GetDataVersionRequest other = (Version_Orig.GetDataVersionRequest) obj;

                boolean result = true;
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionRequest parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionRequest parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionRequest parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_Orig.GetDataVersionRequest prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A request for the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionRequest}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionRequest)
                    Version_Orig.GetDataVersionRequestOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_Orig.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_Orig.internal_static_GetDataVersionRequest_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_Orig.GetDataVersionRequest.class, Version_Orig.GetDataVersionRequest.Builder.class);
                }

                // Construct using Version.GetDataVersionRequest.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_Orig.internal_static_GetDataVersionRequest_descriptor;
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionRequest getDefaultInstanceForType() {
                    return Version_Orig.GetDataVersionRequest.getDefaultInstance();
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionRequest build() {
                    Version_Orig.GetDataVersionRequest result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionRequest buildPartial() {
                    Version_Orig.GetDataVersionRequest result = new Version_Orig.GetDataVersionRequest(this);
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_Orig.GetDataVersionRequest) {
                        return mergeFrom((Version_Orig.GetDataVersionRequest) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_Orig.GetDataVersionRequest other) {
                    if (other == Version_Orig.GetDataVersionRequest.getDefaultInstance()) return this;
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_Orig.GetDataVersionRequest parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_Orig.GetDataVersionRequest) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionRequest)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionRequest)
            private static final Version_Orig.GetDataVersionRequest DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_Orig.GetDataVersionRequest();
            }

            public static Version_Orig.GetDataVersionRequest getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionRequest>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionRequest>() {
                @java.lang.Override
                public GetDataVersionRequest parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionRequest(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionRequest> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionRequest> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_Orig.GetDataVersionRequest getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        public interface GetDataVersionResponseOrBuilder extends
                // @@protoc_insertion_point(interface_extends:GetDataVersionResponse)
                com.google.protobuf.MessageOrBuilder {

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            java.lang.String getVersion();

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            com.google.protobuf.ByteString
            getVersionBytes();
        }

        /**
         * <pre>
         * A response that contains the current data version of the PSMG Structure DB.
         * </pre>
         * <p>
         * Protobuf type {@code GetDataVersionResponse}
         */
        public static final class GetDataVersionResponse extends
                com.google.protobuf.GeneratedMessageV3 implements
                // @@protoc_insertion_point(message_implements:GetDataVersionResponse)
                GetDataVersionResponseOrBuilder {
            private static final long serialVersionUID = 0L;

            // Use GetDataVersionResponse.newBuilder() to construct.
            private GetDataVersionResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
                super(builder);
            }

            private GetDataVersionResponse() {
                version_ = "";
            }

            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }

            private GetDataVersionResponse(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                this();
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            case 10: {
                                java.lang.String s = input.readStringRequireUtf8();

                                version_ = s;
                                break;
                            }
                            default: {
                                if (!parseUnknownFieldProto3(
                                        input, unknownFields, extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }

            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return Version_Orig.internal_static_GetDataVersionResponse_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return Version_Orig.internal_static_GetDataVersionResponse_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                Version_Orig.GetDataVersionResponse.class, Version_Orig.GetDataVersionResponse.Builder.class);
            }

            public static final int VERSION_FIELD_NUMBER = 1;
            private volatile java.lang.Object version_;

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public java.lang.String getVersion() {
                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    version_ = s;
                    return s;
                }
            }

            /**
             * <pre>
             * The current data version.
             * </pre>
             *
             * <code>string version = 1;</code>
             */
            public com.google.protobuf.ByteString
            getVersionBytes() {
                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    version_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            private byte memoizedIsInitialized = -1;

            @java.lang.Override
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                memoizedIsInitialized = 1;
                return true;
            }

            @java.lang.Override
            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                if (!getVersionBytes().isEmpty()) {
                    com.google.protobuf.GeneratedMessageV3.writeString(output, 1, version_);
                }
                unknownFields.writeTo(output);
            }

            @java.lang.Override
            public int getSerializedSize() {
                int size = memoizedSize;
                if (size != -1) return size;

                size = 0;
                if (!getVersionBytes().isEmpty()) {
                    size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, version_);
                }
                size += unknownFields.getSerializedSize();
                memoizedSize = size;
                return size;
            }

            @java.lang.Override
            public boolean equals(final java.lang.Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof Version_Orig.GetDataVersionResponse)) {
                    return super.equals(obj);
                }
                Version_Orig.GetDataVersionResponse other = (Version_Orig.GetDataVersionResponse) obj;

                boolean result = true;
                result = result && getVersion()
                        .equals(other.getVersion());
                result = result && unknownFields.equals(other.unknownFields);
                return result;
            }

            @java.lang.Override
            public int hashCode() {
                if (memoizedHashCode != 0) {
                    return memoizedHashCode;
                }
                int hash = 41;
                hash = (19 * hash) + getDescriptor().hashCode();
                hash = (37 * hash) + VERSION_FIELD_NUMBER;
                hash = (53 * hash) + getVersion().hashCode();
                hash = (29 * hash) + unknownFields.hashCode();
                memoizedHashCode = hash;
                return hash;
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    java.nio.ByteBuffer data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionResponse parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionResponse parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input);
            }

            public static Version_Orig.GetDataVersionResponse parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return com.google.protobuf.GeneratedMessageV3
                        .parseWithIOException(PARSER, input, extensionRegistry);
            }

            @java.lang.Override
            public Builder newBuilderForType() {
                return newBuilder();
            }

            public static Builder newBuilder() {
                return DEFAULT_INSTANCE.toBuilder();
            }

            public static Builder newBuilder(Version_Orig.GetDataVersionResponse prototype) {
                return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
            }

            @java.lang.Override
            public Builder toBuilder() {
                return this == DEFAULT_INSTANCE
                        ? new Builder() : new Builder().mergeFrom(this);
            }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }

            /**
             * <pre>
             * A response that contains the current data version of the PSMG Structure DB.
             * </pre>
             * <p>
             * Protobuf type {@code GetDataVersionResponse}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:GetDataVersionResponse)
                    Version_Orig.GetDataVersionResponseOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return Version_Orig.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return Version_Orig.internal_static_GetDataVersionResponse_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    Version_Orig.GetDataVersionResponse.class, Version_Orig.GetDataVersionResponse.Builder.class);
                }

                // Construct using Version.GetDataVersionResponse.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }

                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessageV3
                            .alwaysUseFieldBuilders) {
                    }
                }

                @java.lang.Override
                public Builder clear() {
                    super.clear();
                    version_ = "";

                    return this;
                }

                @java.lang.Override
                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return Version_Orig.internal_static_GetDataVersionResponse_descriptor;
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionResponse getDefaultInstanceForType() {
                    return Version_Orig.GetDataVersionResponse.getDefaultInstance();
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionResponse build() {
                    Version_Orig.GetDataVersionResponse result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                @java.lang.Override
                public Version_Orig.GetDataVersionResponse buildPartial() {
                    Version_Orig.GetDataVersionResponse result = new Version_Orig.GetDataVersionResponse(this);
                    result.version_ = version_;
                    onBuilt();
                    return result;
                }

                @java.lang.Override
                public Builder clone() {
                    return (Builder) super.clone();
                }

                @java.lang.Override
                public Builder setField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.setField(field, value);
                }

                @java.lang.Override
                public Builder clearField(
                        com.google.protobuf.Descriptors.FieldDescriptor field) {
                    return (Builder) super.clearField(field);
                }

                @java.lang.Override
                public Builder clearOneof(
                        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                    return (Builder) super.clearOneof(oneof);
                }

                @java.lang.Override
                public Builder setRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        int index, java.lang.Object value) {
                    return (Builder) super.setRepeatedField(field, index, value);
                }

                @java.lang.Override
                public Builder addRepeatedField(
                        com.google.protobuf.Descriptors.FieldDescriptor field,
                        java.lang.Object value) {
                    return (Builder) super.addRepeatedField(field, value);
                }

                @java.lang.Override
                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof Version_Orig.GetDataVersionResponse) {
                        return mergeFrom((Version_Orig.GetDataVersionResponse) other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(Version_Orig.GetDataVersionResponse other) {
                    if (other == Version_Orig.GetDataVersionResponse.getDefaultInstance()) return this;
                    if (!other.getVersion().isEmpty()) {
                        version_ = other.version_;
                        onChanged();
                    }
                    this.mergeUnknownFields(other.unknownFields);
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final boolean isInitialized() {
                    return true;
                }

                @java.lang.Override
                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    Version_Orig.GetDataVersionResponse parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (Version_Orig.GetDataVersionResponse) e.getUnfinishedMessage();
                        throw e.unwrapIOException();
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }

                private java.lang.Object version_ = "";

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public java.lang.String getVersion() {
                    java.lang.Object ref = version_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        version_ = s;
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public com.google.protobuf.ByteString
                getVersionBytes() {
                    java.lang.Object ref = version_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        version_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersion(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    version_ = value;
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder clearVersion() {

                    version_ = getDefaultInstance().getVersion();
                    onChanged();
                    return this;
                }

                /**
                 * <pre>
                 * The current data version.
                 * </pre>
                 *
                 * <code>string version = 1;</code>
                 */
                public Builder setVersionBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    checkByteStringIsUtf8(value);

                    version_ = value;
                    onChanged();
                    return this;
                }

                @java.lang.Override
                public final Builder setUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.setUnknownFieldsProto3(unknownFields);
                }

                @java.lang.Override
                public final Builder mergeUnknownFields(
                        final com.google.protobuf.UnknownFieldSet unknownFields) {
                    return super.mergeUnknownFields(unknownFields);
                }


                // @@protoc_insertion_point(builder_scope:GetDataVersionResponse)
            }

            // @@protoc_insertion_point(class_scope:GetDataVersionResponse)
            private static final Version_Orig.GetDataVersionResponse DEFAULT_INSTANCE;

            static {
                DEFAULT_INSTANCE = new Version_Orig.GetDataVersionResponse();
            }

            public static Version_Orig.GetDataVersionResponse getDefaultInstance() {
                return DEFAULT_INSTANCE;
            }

            private static final com.google.protobuf.Parser<GetDataVersionResponse>
                    PARSER = new com.google.protobuf.AbstractParser<GetDataVersionResponse>() {
                @java.lang.Override
                public GetDataVersionResponse parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return new GetDataVersionResponse(input, extensionRegistry);
                }
            };

            public static com.google.protobuf.Parser<GetDataVersionResponse> parser() {
                return PARSER;
            }

            @java.lang.Override
            public com.google.protobuf.Parser<GetDataVersionResponse> getParserForType() {
                return PARSER;
            }

            @java.lang.Override
            public Version_Orig.GetDataVersionResponse getDefaultInstanceForType() {
                return DEFAULT_INSTANCE;
            }

        }

        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionRequest_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionRequest_fieldAccessorTable;
        private static final com.google.protobuf.Descriptors.Descriptor
                internal_static_GetDataVersionResponse_descriptor;
        private static final
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
                internal_static_GetDataVersionResponse_fieldAccessorTable;

        public static com.google.protobuf.Descriptors.FileDescriptor
        getDescriptor() {
            return descriptor;
        }

        private static com.google.protobuf.Descriptors.FileDescriptor
                descriptor;

        static {
            java.lang.String[] descriptorData = {
                    "\n\rversion.proto\"\027\n\025GetDataVersionRequest" +
                            "\")\n\026GetDataVersionResponse\022\017\n\007version\030\001 " +
                            "\001(\tB/\n-com.bmw.psmg.hub.internalservices" +
                            ".api.versionb\006proto3"
            };
            com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                    new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                        public com.google.protobuf.ExtensionRegistry assignDescriptors(
                                com.google.protobuf.Descriptors.FileDescriptor root) {
                            descriptor = root;
                            return null;
                        }
                    };
            com.google.protobuf.Descriptors.FileDescriptor
                    .internalBuildGeneratedFileFrom(descriptorData,
                            new com.google.protobuf.Descriptors.FileDescriptor[]{
                            }, assigner);
            internal_static_GetDataVersionRequest_descriptor =
                    getDescriptor().getMessageTypes().get(0);
            internal_static_GetDataVersionRequest_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionRequest_descriptor,
                    new java.lang.String[]{});
            internal_static_GetDataVersionResponse_descriptor =
                    getDescriptor().getMessageTypes().get(1);
            internal_static_GetDataVersionResponse_fieldAccessorTable = new
                    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                    internal_static_GetDataVersionResponse_descriptor,
                    new java.lang.String[]{"Version",});
        }

        // @@protoc_insertion_point(outer_class_scope)
    }
}
