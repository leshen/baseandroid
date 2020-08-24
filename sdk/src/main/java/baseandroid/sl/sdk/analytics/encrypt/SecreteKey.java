package baseandroid.sl.sdk.analytics.encrypt;

public class SecreteKey {
    /**
     * 公钥秘钥
     */
    public String key;
    /**
     * 公钥秘钥版本
     */
    public int version;

    public SecreteKey(String secretKey, int secretVersion) {
        this.key = secretKey;
        this.version = secretVersion;
    }

    @Override
    public String toString() {
        return "{ key=\"" + key + "\", \"version\"=" + version + "}";
    }
}
