package com.example

import com.example.encryption.CryptoHelper

fun EncryptedPayloadDTO.toInternal(): CryptoHelper.EncryptedPayload =
    CryptoHelper.EncryptedPayload(
        encryptedData = this.encryptedData, iv = this.iv, mac = this.mac, key = this.key)
