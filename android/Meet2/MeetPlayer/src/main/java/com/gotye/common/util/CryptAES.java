package com.gotye.common.util;

import java.security.Key; 
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher; 
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
  
public class CryptAES {   
    
    private static final String CIPHER_ALGORITHM_ECB = "AES/ECB/PKCS5Padding"; 
    private static final String CIPHER_ALGORITHM_ECB_NOPADDING = "AES/ECB/NoPadding";
    private static final String CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding";   
    
    public static String AES_Encrypt(String keyStr, String plainText) { 
        byte[] encrypt = null; 
        try{ 
            Key key = generateKey(keyStr);  
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encrypt = cipher.doFinal(plainText.getBytes());     
        }catch(Exception e){ 
            e.printStackTrace(); 
        }
        return new String(Base64.encodeBase64(encrypt)); 
    } 
 
    public static String AES_Decrypt(String keyStr, String encryptData) {
        byte[] decrypt = null; 
        try{ 
            Key key = generateKey(keyStr); 
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB_NOPADDING);  
            cipher.init(Cipher.DECRYPT_MODE, key); 
            decrypt = cipher.doFinal(Base64.decodeBase64(encryptData.getBytes()));
        }catch(Exception e){ 
            e.printStackTrace(); 
        } 
        return new String(decrypt).trim(); 
    }
    
    private static byte[] randomIVBytes() {  
        Random ran = new Random();  
        byte[] bytes = new byte[8];  
        for (int i = 0; i < bytes.length; ++i) {  
            bytes[i] = (byte) ran.nextInt(Byte.MAX_VALUE + 1);  
        }  
        return bytes;  
    }  
    
    private static Key generateKey(String key)throws Exception{ 
        try{            
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES"); 
            return keySpec; 
        }catch(Exception e){ 
            e.printStackTrace(); 
            throw e; 
        } 
 
    }
}
