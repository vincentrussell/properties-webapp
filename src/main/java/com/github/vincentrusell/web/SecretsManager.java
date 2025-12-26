package com.github.vincentrusell.web;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class SecretsManager {

    public static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    public static final String RSA = "RSA";
    private final String keystorePath;
    private final String keystorePassword;
    private final String keyPassword;
    private final String keyAlias;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keystoreType;


    public SecretsManager(final String keystorePath, final String keystorePassword,
                          final String keystoreType,
                          final String keyPassword, final String keyAlias) throws Exception {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.keyPassword = keyPassword;
        this.keyAlias = keyAlias;
        this.privateKey = getKeyFromKeystore();
        this.publicKey = getPublicKeyFromPrivateKey(privateKey);
    }

    public PublicKey getPublicKeyFromPrivateKey(PrivateKey privateKey) throws Exception {
        if (privateKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey) privateKey;
            BigInteger modulus = rsaPrivateCrtKey.getModulus();
            BigInteger publicExponent = rsaPrivateCrtKey.getPublicExponent();

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            return keyFactory.generatePublic(publicKeySpec);
        }
        throw new IllegalArgumentException("Private key is not an RSAPrivateCrtKey");
    }

    public PrivateKey getKeyFromKeystore() throws Exception {
        try(FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore ks = KeyStore.getInstance(keystoreType); // Use JKS for PrivateKeyEntry
            ks.load(fis, keystorePassword.toCharArray());
            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyPassword.toCharArray());
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyAlias, protParam);
            return privateKeyEntry.getPrivateKey();
        }
    }

    public String encryptString(final String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes); // Encode to String for storage/transmission
    }

    public String decryptString(final String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = Base64.getDecoder().decode(encryptedText); // Decode from String
        byte[] plainBytes = cipher.doFinal(decryptedBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

}
