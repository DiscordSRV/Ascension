/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.core.paste.service;

import com.discordsrv.common.core.paste.Paste;
import com.discordsrv.common.core.paste.PasteService;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryptedPasteService implements PasteService {

    private final PasteService service;
    private final int keySize;
    private final SecureRandom RANDOM = new SecureRandom();

    public AESEncryptedPasteService(PasteService service, int keySize) {
        this.service = service;
        this.keySize = keySize;
    }

    @Override
    public Paste uploadFile(byte[] fileContent) throws Throwable {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);

        SecretKey secretKey = generateKey();
        byte[] encrypted = encrypt(secretKey, fileContent, iv);

        Paste paste = service.uploadFile(Base64.getEncoder().encode(ArrayUtils.addAll(iv, encrypted)));
        return paste.withDecryptionKey(secretKey.getEncoded());
    }

    private SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);

        return keyGenerator.generateKey();
    }

    private byte[] encrypt(SecretKey key, byte[] content, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(content);
    }
}
