package org.dejavu.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * A cryptographic utility for encrypting/decrypting data using AES technique.
 * @author hai
 */
public class LaCrypto {
	private final char [] password;
	/**
	 * Key factory
	 */
	private static final SecretKeyFactory gKeyFactory;
	/**
	 * Key length
	 */
	private static final int gKeyLen = 256;
	/**
	 * Encryption algorithm
	 */
	private static final String gAlgorithm = "AES";
	/**
	 * Cipher transformation
	 */
	private static final String gCipherTransformation = "AES/CBC/PKCS5Padding";
	/**
	 * Creates a new crypto utility.
	 * @param password The password to be used with this utility
	 * @throws LaCryptoException 
	 */
	public LaCrypto(char[] password) throws LaCryptoException {
		super();
		this.password = password;
	}
	/**
	 * Representing the encrypted data, and everything else required to decrypt it,
	 * except for the password itself.
	 */
	public static final class EncryptedData {
		/**
		 * The encrypted data
		 */
		public final byte [] encryptedData;
		/**
		 * The 16-byte initial vector
		 */
		public final byte [] iv;
		/**
		 * Number of iteration to hash the data
		 */
		public final int iterations;
		/**
		 * The 8-byte salt
		 */
		public final byte [] salt;
		/**
		 * Creates a new instance of encrypted data
		 * @param iters The iteration count
		 * @param salt The 8-byte salt
		 * @param iv The 16-byte initial vector
		 * @param data The encrypted data
		 */
		public EncryptedData(int iters, byte [] salt, byte [] iv, byte [] data) {
			this.iv = iv;
			this.encryptedData = data;
			this.iterations = iters;
			this.salt = salt;
		}
		
		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(512).append("{iv:'").append(bytesToString(iv)).append("',");
			ret.append("salt:'").append(bytesToString(salt)).append("',");
			ret.append("iterations:'").append(iterations).append("',");
			return ret.append("encryptedData:'").append(bytesToString(encryptedData)).append("'}").toString();
		}
	}
	/**
	 * Converts a byte array into equivalent, human-readable string.
	 * @param data The byte array to be converted
	 * @return The string representation of the given byte array
	 */
	private static String bytesToString(byte [] data) {
		StringBuilder ret = new StringBuilder(1024);
		if(data != null) {
		boolean comma = false;
			for(byte x : data) {
				if(!comma) {
					comma = true;
				} else {
					ret.append(',');
				}
				ret.append(String.format("0x%02x", x));
			}
		}
		return ret.toString();
	}
	/**
	 * Encrypts some data, in its entirety.
	 * @param data The byte array containing the data to be encrypted
	 * @param salt The 8-byte salt
	 * @param iterations The number of iterations for hashing
	 * @return The encrypted data.
	 * @throws LaCryptoException 
	 */
	public EncryptedData encrypt(byte [] data, byte [] salt, int iterations) throws LaCryptoException {
		try {
			if(gKeyFactory == null) {
				throw new LaCryptoException("No key factory");
			}
			PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, gKeyLen);
			SecretKey secret = new SecretKeySpec(gKeyFactory.generateSecret(keySpec).getEncoded(), gAlgorithm);
			Cipher cip = Cipher.getInstance(gCipherTransformation);
			cip.init(Cipher.ENCRYPT_MODE, secret);
			byte[] iv = cip.getIV();
			System.out.printf("IV is %s\n", bytesToString(iv));
			return new EncryptedData(iterations, salt, iv, cip.doFinal(data));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new LaCryptoException(ex);
		}
	}
	/**
	 * Decrypts some set of encrypted data
	 * @param data The encrypted data
	 * @return The decrypted data.
	 * @throws LaCryptoException 
	 */
	public byte [] decrypt(EncryptedData data) throws LaCryptoException {
		try {
			if(gKeyFactory == null) {
				throw new LaCryptoException("No key factory");
			}
			PBEKeySpec keySpec = new PBEKeySpec(password, data.salt, data.iterations, gKeyLen);
			SecretKey secret = new SecretKeySpec(gKeyFactory.generateSecret(keySpec).getEncoded(), gAlgorithm);
			Cipher cip = Cipher.getInstance(gCipherTransformation);
			cip.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(data.iv));
			return cip.doFinal(data.encryptedData);
		} catch (RuntimeException | InvalidKeySpecException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new LaCryptoException(ex);
		}
	}
	
	/**
	 * Encrypts a stream of data, e.g. a file, to another stream, e.g. another file.
	 * @param clearText The stream of clear-text data to be encrypted
	 * @param encrypted The stream to write out the encrypted data
	 * @param salt The salt to be used
	 * @param iterations The iteration count to be used
	 * @return The initialization vector generated during the encryption process.
	 * @throws LaCryptoException 
	 */
	public byte [] encrypt(InputStream clearText, OutputStream encrypted, byte[] salt, int iterations) throws LaCryptoException {
		try {
			if(gKeyFactory == null) {
				throw new LaCryptoException("No key factory");
			}
			PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, gKeyLen);
			SecretKey secret = new SecretKeySpec(gKeyFactory.generateSecret(keySpec).getEncoded(), gAlgorithm);
			Cipher cip = Cipher.getInstance(gCipherTransformation);
			cip.init(Cipher.ENCRYPT_MODE, secret);
			byte[] iv = cip.getIV();
			byte[] inputBuffer = new byte[4096];
			int bytes = clearText.read(inputBuffer);
			while(bytes > 0) {
				byte[] out = cip.update(inputBuffer, 0, bytes);
				encrypted.write(out);
				bytes = clearText.read(inputBuffer);
			}
			byte[] finalchunk = cip.doFinal();
			if(finalchunk != null) {
				encrypted.write(finalchunk);
			}
			return iv;
		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | IOException | InvalidKeyException ex) {
			throw new LaCryptoException(ex);
		}
 	}
	
	/**
	 * Decrypts a stream of data, e.g. a file, to another stream, e.g. another file.
	 * @param encrypted The stream of encrypted data to be decrypted
	 * @param clearText The stream to write out the decrypted data
	 * @param salt The salt to be used
	 * @param iv The initialisation vector to be used in the decryption process.
	 * @param iterations The iteration count to be used
	 * @throws LaCryptoException 
	 */
	public void decrypt(InputStream encrypted, OutputStream clearText, byte[] salt, byte [] iv, int iterations) throws LaCryptoException {
		try {
			if(gKeyFactory == null) {
				throw new LaCryptoException("No key factory");
			}
			PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, gKeyLen);
			SecretKey secret = new SecretKeySpec(gKeyFactory.generateSecret(keySpec).getEncoded(), gAlgorithm);
			Cipher cip = Cipher.getInstance(gCipherTransformation);
			cip.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			byte[] inputBuffer = new byte[4096];
			int bytes = encrypted.read(inputBuffer);
			while(bytes > 0) {
				byte[] out = cip.update(inputBuffer, 0, bytes);
				clearText.write(out);
				bytes = encrypted.read(inputBuffer);
			}
			byte[] finalchunk = cip.doFinal();
			if(finalchunk != null) {
				clearText.write(finalchunk);
			}
		} catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | IOException | InvalidKeyException ex) {
			throw new LaCryptoException(ex);
		}
 	}
	
	private static final Pattern gSaltPattern = Pattern.compile("^salt=(.+)$");
	private static final Pattern gIvPattern = Pattern.compile("^iv\\s+=(.+)$");
	private static byte [] stringToBytes(String str) {
		int begin = 0;
		int end = begin + 2;
		int len = str.length();
		byte[] ret = new byte[len/2];
		int idx = 0;
		while(end < len) {
			ret[idx++] = (byte)Integer.parseInt(str.substring(begin, end), 16);
			begin += 2;
			end += 2;
		}
		return ret;
	}
	
	public static EncryptedData getEncryptedDataFromOpenSsl(File encrypted, char [] password) throws LaCryptoException {
		try {
			ProcessBuilder b = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-d", "-pass", "pass:" + String.valueOf(password), "-P", "-in", encrypted.toString()});
			Process p = b.start();
			InputStream is = p.getInputStream();
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String line;
				byte [] salt = null;
				byte [] iv = null;
				while ((line = reader.readLine()) != null) {
					if(salt == null) {
						Matcher m = gSaltPattern.matcher(line);
						if(m.find()) {
							salt = stringToBytes(m.group(1));
							continue;
						}
					}
					if(iv == null) {
						Matcher m = gIvPattern.matcher(line);
						if(m.find()) {
							iv = stringToBytes(m.group(1));
							continue;
						}
					}
					if((iv != null)&&(salt != null)) {
						break;
					}
				}
				return new EncryptedData(1, salt, iv, null);
			} finally {
				is.close();
			}
		} catch (IOException ex) {
			throw new LaCryptoException(ex);
		}
	}
	
	public static void main(String[] args) {
		try {
			byte[] salt = new byte[]{0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, (byte)0x80};
			String password = "ashdkjahdk";
			String data = "This is a test";
			int iters = 0xffff;
			LaCrypto crypt = new LaCrypto(password.toCharArray());
			EncryptedData enc = crypt.encrypt(data.getBytes(), salt, iters);
			System.out.printf("Encrypted = %s\n", enc);
			LaCrypto dec = new LaCrypto(password.toCharArray());
			byte[] d = dec.decrypt(enc);
			System.out.printf("From '%s' to '%s'\n", data, new String(d));
			InputStream clear = new FileInputStream(new File("build.xml"));
			byte[] iv = null;
			try {
				OutputStream encrypted = new FileOutputStream(new File("build-enc.xml"));
				try {
					LaCrypto krypt = new LaCrypto(password.toCharArray());
					iv = krypt.encrypt(clear, encrypted, salt, iters);
				} finally {
					encrypted.close();
				}
			} finally {
				clear.close();
			}
			InputStream encrypted = new FileInputStream(new File("build-enc.xml"));
			try {
				OutputStream decrypted = new FileOutputStream(new File("build-dec.xml"));
				try {
					LaCrypto krypt = new LaCrypto(password.toCharArray());
					krypt.decrypt(encrypted, decrypted, salt, iv, iters);
				} finally {
					decrypted.close();
				}
			} finally {
				encrypted.close();
			}
			File openSslFile = new File("build-openssl.xml");
			EncryptedData encData = getEncryptedDataFromOpenSsl(openSslFile, password.toCharArray());
			System.out.printf("Found %s from %s\n", encData, openSslFile);
			LaCrypto krypt = new LaCrypto(password.toCharArray());
			InputStream openSsl = new FileInputStream(openSslFile);
			try {
				OutputStream decc = new FileOutputStream(new File("build-closessl.xml"));
				try {
					krypt.decrypt(openSsl, decc, encData.salt, encData.iv, encData.iterations);
				} finally {
					decc.close();
				}
			} finally {
				openSsl.close();
			}
		} catch (IOException | LaCryptoException ex) {
			Logger.getLogger(LaCrypto.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	static  {
		SecretKeyFactory tmp = null;
		try {
			tmp = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(LaCrypto.class.getName()).log(Level.SEVERE, null, ex);
		}
		gKeyFactory = tmp;
	}
}
