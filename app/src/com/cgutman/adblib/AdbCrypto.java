/*
Copyright (c) 2013, Cameron Gutman
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

  Neither the name of the {organization} nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Modified: ephemeral setup keys only; key import/export removed.
*/
package com.cgutman.adblib;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;

/**
 * This class encapsulates the ADB cryptography functions and provides
 * an interface for the storage and retrieval of keys.
 * @author Cameron Gutman
 */
public class AdbCrypto {
	
	/** An RSA keypair encapsulated by the AdbCrypto object */
	private KeyPair keyPair;
	
	/** The base 64 conversion interface to use */
	private AdbBase64 base64;
	
	/** The ADB RSA key length in bits */
	public static final int KEY_LENGTH_BITS = 2048;
	
	/** The ADB RSA key length in bytes */
	public static final int KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8;
	
	/** The ADB RSA key length in words */
	public static final int KEY_LENGTH_WORDS = KEY_LENGTH_BYTES / 4;
	
	/** The RSA signature padding as an int array */
	public static final int[] SIGNATURE_PADDING_AS_INT = new int[]
			{
		    0x00,0x01,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,
		    0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0xff,0x00,
		    0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,
		    0x04,0x14
			};
	
	/** The RSA signature padding as a byte array */
	public static byte[] SIGNATURE_PADDING;
	
	static {
		SIGNATURE_PADDING = new byte[SIGNATURE_PADDING_AS_INT.length];
		
		for (int i = 0; i < SIGNATURE_PADDING.length; i++)
			SIGNATURE_PADDING[i] = (byte)SIGNATURE_PADDING_AS_INT[i];
	}
	
	/**
	 * Converts a standard RSAPublicKey object to the special ADB format
	 * @param pubkey RSAPublicKey object to convert
	 * @return Byte array containing the converted RSAPublicKey object
	 */
	private static byte[] convertRsaPublicKeyToAdbFormat(RSAPublicKey pubkey)
	{
		/*
		 * ADB literally just saves the RSAPublicKey struct to a file.
		 * 
		 * typedef struct RSAPublicKey {
         * int len; // Length of n[] in number of uint32_t
         * uint32_t n0inv;  // -1 / n[0] mod 2^32
         * uint32_t n[RSANUMWORDS]; // modulus as little endian array
         * uint32_t rr[RSANUMWORDS]; // R^2 as little endian array
         * int exponent; // 3 or 65537
         * } RSAPublicKey;
		 */

		/* ------ This part is a Java-ified version of RSA_to_RSAPublicKey from adb_host_auth.c ------ */
		BigInteger r32, r, rr, rem, n, n0inv;
		
		r32 = BigInteger.ZERO.setBit(32);
		n = pubkey.getModulus();
		r = BigInteger.ZERO.setBit(KEY_LENGTH_WORDS * 32);
		rr = r.modPow(BigInteger.valueOf(2), n);
		rem = n.remainder(r32);
		n0inv = rem.modInverse(r32);
		
		int myN[] = new int[KEY_LENGTH_WORDS];
		int myRr[] = new int[KEY_LENGTH_WORDS];
		BigInteger res[];
		for (int i = 0; i < KEY_LENGTH_WORDS; i++)
		{
			res = rr.divideAndRemainder(r32);
			rr = res[0];
			rem = res[1];
			myRr[i] = rem.intValue();
			
			res = n.divideAndRemainder(r32);
			n = res[0];
			rem = res[1];
			myN[i] = rem.intValue();
		}

		/* ------------------------------------------------------------------------------------------- */
		
		ByteBuffer bbuf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);

		
		bbuf.putInt(KEY_LENGTH_WORDS);
		bbuf.putInt(n0inv.negate().intValue());
		for (int i : myN)
			bbuf.putInt(i);
		for (int i : myRr)
			bbuf.putInt(i);
		
		bbuf.putInt(pubkey.getPublicExponent().intValue());
		return bbuf.array();
	}
	
	/**
	 * Creates a new AdbCrypto object by generating a new key pair.
	 * @param base64 Implementation of base 64 conversion interface required by ADB 
	 * @return A new AdbCrypto object
	 * @throws NoSuchAlgorithmException If an RSA key factory cannot be found
	 */
	public static AdbCrypto generateAdbKeyPair(AdbBase64 base64) throws NoSuchAlgorithmException
	{
		AdbCrypto crypto = new AdbCrypto();

		KeyPairGenerator rsaKeyPg = KeyPairGenerator.getInstance("RSA");
		rsaKeyPg.initialize(KEY_LENGTH_BITS);
		
		crypto.keyPair = rsaKeyPg.genKeyPair();
		crypto.base64 = base64;
		
		return crypto;
	}
	
	/**
	 * Signs the ADB SHA1 payload with the private key of this object.
	 * @param payload SHA1 payload to sign
	 * @return Signed SHA1 payload
	 * @throws GeneralSecurityException If signing fails
	 */
	public byte[] signAdbTokenPayload(byte[] payload) throws GeneralSecurityException
	{	
		Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
		
		c.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		
		c.update(SIGNATURE_PADDING);
		
		return c.doFinal(payload);
	}
	
	/**
	 * Gets the RSA public key in ADB format.
	 * @return Byte array containing the RSA public key in ADB format.
	 * @throws IOException If the key cannot be retrived
	 */
	public byte[] getAdbPublicKeyPayload() throws IOException
	{
		byte[] convertedKey = convertRsaPublicKeyToAdbFormat((RSAPublicKey)keyPair.getPublic());
		StringBuilder keyString = new StringBuilder(720);
		
		/* The key is base64 encoded with a user@host suffix and terminated with a NUL */
		keyString.append(base64.encodeToString(convertedKey));
		keyString.append(" alexa-na-tv@localhost");
		keyString.append('\0');
		
		return keyString.toString().getBytes("UTF-8");
	}
	
}
