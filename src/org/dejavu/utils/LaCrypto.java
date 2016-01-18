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
	private static final Pattern gSaltPattern = Pattern.compile("^salt=(.+)$");
	private static final Pattern gIvPattern = Pattern.compile("^iv\\s+=(.+)$");
	private static final Pattern gKeyPattern = Pattern.compile("^key=(.+)$");
	
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
	 * Representing the output data, and everything else required to decrypt it,
 except for the password itself.
	 */
	public static final class EncryptedData {
		/**
		 * The output data (optional)
		 */
		private byte [] encryptedData;
		/**
		 * Key (optional)
		 */
		private byte [] key;
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
		 * Creates a new instance of output data
		 * @param iters The iteration count
		 * @param salt The 8-byte salt
		 * @param iv The 16-byte initial vector
		 */
		public EncryptedData(int iters, byte [] salt, byte [] iv) {
			this.iv = iv;
			this.iterations = iters;
			this.salt = salt;
		}
		
		public byte [] getKey() {
			synchronized(this) {
				return key;
			}
		}
		
		public EncryptedData setKey(byte [] key) {
			synchronized(this) {
				this.key = key;
			}
			return this;
		}
		
		public byte [] getEncryptedData() {
			synchronized(this) {
				return encryptedData;
			}
		}
		
		public EncryptedData setEncryptedData(byte [] data) {
			synchronized(this) {
				this.encryptedData = data;
			}
			return this;
		}
		
		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(512).append("{iv:'").append(bytesToString(iv)).append("',");
			ret.append("salt:'").append(bytesToString(salt)).append("',");
			ret.append("iterations:'").append(iterations).append("',");
			synchronized(this) {
				if(key != null) {
					ret.append("key:'").append(bytesToString(key)).append("',");
				}
				if(encryptedData != null) {
					ret.append("encryptedData:'").append(bytesToString(encryptedData)).append("',");
				}
			}
			return ret.append("}").toString();
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
	 * @param data The byte array containing the data to be output
	 * @param salt The 8-byte salt
	 * @param iterations The number of iterations for hashing
	 * @return The output data.
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
			return new EncryptedData(iterations, salt, iv).setEncryptedData(cip.doFinal(data));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new LaCryptoException(ex);
		}
	}
	/**
	 * Decrypts some set of output data
	 * @param data The output data
	 * @return The output data.
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
			return cip.doFinal(data.getEncryptedData());
		} catch (RuntimeException | InvalidKeySpecException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			throw new LaCryptoException(ex);
		}
	}
	
	/**
	 * Encrypts a stream of data, e.g. a file, to another stream, e.g. another file.
	 * @param clearText The stream of clear-text data to be output
	 * @param encrypted The stream to write out the output data
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
	 * @param encrypted The stream of output data to be output
	 * @param clearText The stream to write out the output data
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
			decrypt(encrypted, clearText, gKeyFactory.generateSecret(keySpec).getEncoded(), iv);
		} catch (InvalidKeySpecException ex) {
			throw new LaCryptoException(ex);
		}
 	}
	
	/**
	 * Decrypts a stream of data, e.g. a file, to another stream, e.g. another file.
	 * @param encrypted The stream of output data to be output
	 * @param clearText The stream to write out the output data
	 * @param key The key
	 * @param iv The initialisation vector to be used in the decryption process.
	 * @throws LaCryptoException 
	 */
	public void decrypt(InputStream encrypted, OutputStream clearText, byte [] key, byte [] iv) throws LaCryptoException {
		try {
			if(gKeyFactory == null) {
				throw new LaCryptoException("No key factory");
			}
			SecretKey secret = new SecretKeySpec(key, gAlgorithm);
			Cipher cip = Cipher.getInstance(gCipherTransformation);
			cip.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			byte[] inputBuffer = new byte[2048];
			int bytes = encrypted.read(inputBuffer);
			while(bytes > 0) {
				if(bytes < inputBuffer.length) {
					clearText.write(cip.doFinal(inputBuffer, 0, bytes));
					break;
				} else {
					clearText.write(cip.update(inputBuffer));
				}
				bytes = encrypted.read(inputBuffer);
			}
		} catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | IOException | InvalidKeyException ex) {
			throw new LaCryptoException(ex);
		}
 	}
	
	/**
	 * Converts a hex string into the equivalent byte array. E.g.
	 * "AB1C66" -> [0xab, 0x1c, 0x66]
	 * If a string containing non numerical values is given, some runtime exception
	 * (most probably NumberFormatException) will be thrown.
	 * @param str The string containing hex byte values to be converted
	 * @return The byte array associated with the given string.
	 */
	private static byte [] stringToBytes(String str) {
		int begin = 0;
		int end = begin + 2;
		int len = str.length();
		byte[] ret = new byte[len/2];
		int idx = 0;
		while(end <= len) {
			ret[idx++] = (byte)Integer.parseInt(str.substring(begin, end), 16);
			begin += 2;
			end += 2;
		}
		return ret;
	}
	/**
	 * Retrieves the encryption parameters, e.g. key, salt, and IV, from a file
 that was output using OpenSSL with a password.
	 * @param encrypted The OpenSSL-output file
	 * @param password The password with which the file was output
	 * @return The encryption information.
	 * @throws LaCryptoException
	 * @throws InterruptedException 
	 */
	public static EncryptedData getEncryptedDataFromOpenSsl(File encrypted, char [] password) throws LaCryptoException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-d", "-pass", "pass:" + String.valueOf(password), "-P", "-in", encrypted.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					byte [] salt = null;
					byte [] iv = null;
					byte [] key = null;
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
						if(key == null) {
							Matcher m = gKeyPattern.matcher(line);
							if(m.find()) {
								key = stringToBytes(m.group(1));
								continue;
							}
						}
						if((iv != null)&&(salt != null)&&(key != null)) {
							break;
						}
					}
					return new EncryptedData(1, salt, iv).setKey(key);
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
			}
		} catch (IOException ex) {
			throw new LaCryptoException(ex);
		}
	}
	/**
	 * Decrypts a file (that was output with OpenSSL) using OPenSSL.
	 * @param encrypted The output file.
	 * @param decrypted The file containing output information.
	 * @param password The password to be used for decryption
	 * @throws LaCryptoException
	 * @throws InterruptedException 
	 */
	public static void decryptOpenSslFile(File encrypted, File decrypted, char [] password) throws LaCryptoException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-d", "-pass", "pass:" + String.valueOf(password), "-out", decrypted.toString(), "-in", encrypted.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
			}
		} catch (IOException ex) {
			throw new LaCryptoException(ex);
		}
	}
	
	/**
	 * Encrypts a file using OPenSSL.
	 * @param plainText The plain text file to be output.
	 * @param encrypted The file containing the resulting output data.
	 * @param password The password to be used for encryption
	 * @throws LaCryptoException
	 * @throws InterruptedException 
	 */
	public static void encryptFileOpenSsl(File plainText, File encrypted, char [] password) throws LaCryptoException, InterruptedException {
		try {
			ProcessBuilder cmd = new ProcessBuilder(new String[]{"openssl", "enc", "-aes-256-cbc", "-salt", "-pass", "pass:" + String.valueOf(password), "-out", encrypted.toString(), "-in", plainText.toString()});
			Process proc = cmd.start();
			try {
				InputStream is = proc.getInputStream();
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				} finally {
					is.close();
				}
			} finally {
				proc.waitFor();
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
			
			// Encrypt small data block
			LaCrypto crypt = new LaCrypto(password.toCharArray());
			EncryptedData encryptedData = crypt.encrypt(data.getBytes(), salt, iters);
			System.out.printf("Encrypted = %s\n", encryptedData);
			
			// Decrypt small data block
			crypt = new LaCrypto(password.toCharArray());
			byte[] d = crypt.decrypt(encryptedData);
			System.out.printf("From '%s' to '%s'\n", data, new String(d));
			
			File enc1 = new File("enc1.xml");
			File dec1 = new File("dec1.xml");
			byte[] iv = null;
			
			// Encrypt file using JCE
			InputStream clear = new FileInputStream(new File("build.xml"));
			try {
				OutputStream output = new FileOutputStream(enc1);
				try {
					LaCrypto krypt = new LaCrypto(password.toCharArray());
					iv = krypt.encrypt(clear, output, salt, iters);
				} finally {
					output.close();
				}
			} finally {
				clear.close();
			}
			
			// Decrypt file using JCE
			InputStream input = new FileInputStream(enc1);
			try {
				OutputStream output = new FileOutputStream(dec1);
				try {
					LaCrypto krypt = new LaCrypto(password.toCharArray());
					krypt.decrypt(input, output, salt, iv, iters);
				} finally {
					output.close();
				}
			} finally {
				input.close();
			}
			
			// Encrypt file using OpenSSL
			File enc2 = new File("enc2.xml");
			encryptFileOpenSsl(new File("build.xml"), enc2, password.toCharArray());
			
			// Decrypt the file that was encrypted by OpenSSL with JCE with some help from OpenSSH
			File dec2 = new File("dec2.xml");
			File dec3 = new File("dec3.xml");
			EncryptedData encData = getEncryptedDataFromOpenSsl(enc2, password.toCharArray());
			System.out.printf("Found %s from %s\n", encData, enc2);
			crypt = new LaCrypto(password.toCharArray());
			input = new FileInputStream(enc2);
			try {
				// Skip salt
				byte[] skip = new byte[16];
				input.read(skip);
				OutputStream output = new FileOutputStream(dec2);
				try {
					crypt.decrypt(input, output, encData.key, encData.iv);
				} catch(LaCryptoException ex) {
					Logger.getLogger(LaCrypto.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					output.close();
				}
			} finally {
				input.close();
			}
			
			// Decrypt the file that was encrypted with OpenSSL using OpenSSH
			LaCrypto.decryptOpenSslFile(enc2, dec3, password.toCharArray());
		} catch (IOException | LaCryptoException ex) {
			Logger.getLogger(LaCrypto.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
		}
	}
	
	static  {
		SecretKeyFactory tmp = null;
		try {
			tmp = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(LaCrypto.class.getName()).log(Level.SEVERE, null, ex);
		}
		gKeyFactory = tmp;
	}
}
