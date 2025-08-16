package com.example.otel.http.ins.model;

public class SpConstants {
    public static final String HTTP_METHOD_HEAD = "HEAD";
    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";

    private SpConstants() {}

    public static final String REDIRECT_REQUEST_METHOD = "sp-redirect-request-method";
    public static final String REDIRECT_REFERER = "sp-redirect-referer";
    public static final String REDIRECT_PATTERN = "sp-redirect-pattern";
    public static final String SP_TARGET_RECORD_ID_TEMP = "sp-target-record-id";
    /**
     * dubbo stream protocol:triple
     */
    public static final String DUBBO_STREAM_PROTOCOL = ":tri";
    /**
     * dubbo stream protocol:streaming
     */
    public static final String DUBBO_STREAM_NAME = "streaming";

    public static final String UUID_SIGNATURE = "java.util.UUID.randomUUID";
    public static final String CURRENT_TIME_MILLIS_SIGNATURE = "java.lang.System.currentTimeMillis";
    public static final String NEXT_INT_SIGNATURE = "java.util.Random.nextInt";
    public static final String SERIALIZE_SKIP_INFO_CONFIG_KEY = "serializeSkipInfoList";
    public static final String SCHEDULE_REPLAY = "sp-schedule-replay";
    public static final String SP_EXTENSION_ATTRIBUTE = "sp-extension-attribute";
    public static final String GSON_SERIALIZER = "gson";
    public static final String GSON_REQUEST_SERIALIZER = "gson-request";
    public static final String JACKSON_SERIALIZER = "jackson";
    public static final String JACKSON_SERIALIZER_WITH_TYPE = "jackson-with-type";
    public static final String JACKSON_REQUEST_SERIALIZER = "jackson-request";
    public static final String SP_SERIALIZER = "sp-serializer";
    public static final String CONFIG_DEPENDENCY = "sp_replay_prepare_dependency";
    public static final String PREFIX = "sp-";
    public static final String CONFIG_VERSION = "configBatchNo";
    public static final String SKIP_FLAG = "sp-skip-flag";
    public static final String ORIGINAL_REQUEST = "sp-original-request";
    public static final String MERGE_RECORD_NAME = "sp.mergeRecord";
    public static final String MERGE_RECORD_THRESHOLD = "sp.merge.record.threshold";
    public static final String DISABLE_MERGE_RECORD = "sp.disable.merge.record";
    public static final int MERGE_RECORD_THRESHOLD_DEFAULT = 10;
    public static final String MERGE_TYPE = "java.util.ArrayList-io.softprobe.inst.runtime.model.MergeDTO";
    public static final String MERGE_SPLIT_COUNT = "sp.merge.split.count";
    public static final long MEMORY_SIZE_1MB = 1024L * 1024L;
    public static final long MEMORY_SIZE_5MB = 5 * 1024L * 1024L;
    public static final long MEMORY_SIZE_20MB = 20 * 1024L * 1024L;
    public static final String EXCEED_MAX_SIZE_TITLE = "exceed.max.size";
    public static final String EXCEED_MAX_SIZE_FLAG = "isExceedMaxSize";
    public static final String RECORD_SIZE_LIMIT = "sp.record.size.limit";
    public static final String SERVLET_V3 = "ServletV3";
    public static final String SERVLET_V5 = "ServletV5";
    public static final String MERGE_MOCKER_TYPE = "java.util.ArrayList-io.softprobe.agent.bootstrap.model.SpMocker";
    public static final int DB_SQL_MAX_LEN = 5000;
    public static final String DISABLE_SQL_PARSE = "sp.disable.sql.parse";
    public static final String SPRING_SCAN_PACKAGES = "sp.spring.scan.packages";
    public static final String OPERATION_KEY = "operation.key";

    public static final String SKIP_MOCK = "skipMock";
    public static final String MUST_MOCK = "mustMock";

    // disable mock 龙腾
    public static final String DISABLE_MOCK = "sp-ignore-dependencies";
}