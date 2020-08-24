
package baseandroid.sl.sdk.analytics.encrypt;

public interface IPersistentSecretKey {
    /**
     * 存储公钥
     *
     * @param secreteKey 密钥
     */
    void saveSecretKey(SecreteKey secreteKey);

    /**
     * 获取公钥
     *
     * @return 密钥
     */
    SecreteKey loadSecretKey();
}