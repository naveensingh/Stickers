/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.linuxauthority.stickers

import android.text.TextUtils
import android.util.JsonReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

internal object ContentFileParser {
    private const val LIMIT_EMOJI_COUNT = 3

    @JvmStatic
    @Throws(IOException::class, IllegalStateException::class)
    fun parseStickerPacks(contentsInputStream: InputStream): List<StickerPack> {
        JsonReader(InputStreamReader(contentsInputStream)).use { reader -> return readStickerPacks(reader) }
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun readStickerPacks(reader: JsonReader): List<StickerPack> {
        val stickerPackList: MutableList<StickerPack> = ArrayList()
        var androidPlayStoreLink: String? = null
        var iosAppStoreLink: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if ("android_play_store_link" == key) {
                androidPlayStoreLink = reader.nextString()
            } else if ("ios_app_store_link" == key) {
                iosAppStoreLink = reader.nextString()
            } else if ("sticker_packs" == key) {
                reader.beginArray()
                while (reader.hasNext()) {
                    val stickerPack = readStickerPack(reader)
                    stickerPackList.add(stickerPack)
                }
                reader.endArray()
            } else {
                throw IllegalStateException("unknown field in json: $key")
            }
        }
        reader.endObject()
        check(stickerPackList.size != 0) { "sticker pack list cannot be empty" }
        for (stickerPack in stickerPackList) {
            stickerPack.setAndroidPlayStoreLink(androidPlayStoreLink)
            stickerPack.setIosAppStoreLink(iosAppStoreLink)
        }
        return stickerPackList
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun readStickerPack(reader: JsonReader): StickerPack {
        reader.beginObject()
        var identifier: String? = null
        var name: String? = null
        var publisher: String? = null
        var trayImageFile: String? = null
        var publisherEmail: String? = null
        var publisherWebsite: String? = null
        var privacyPolicyWebsite: String? = null
        var licenseAgreementWebsite: String? = null
        var imageDataVersion: String? = ""
        var avoidCache = false
        var stickerList: List<Sticker?>? = null
        while (reader.hasNext()) {
            val key = reader.nextName()
            when (key) {
                "identifier" -> identifier = reader.nextString()
                "name" -> name = reader.nextString()
                "publisher" -> publisher = reader.nextString()
                "tray_image_file" -> trayImageFile = reader.nextString()
                "publisher_email" -> publisherEmail = reader.nextString()
                "publisher_website" -> publisherWebsite = reader.nextString()
                "privacy_policy_website" -> privacyPolicyWebsite = reader.nextString()
                "license_agreement_website" -> licenseAgreementWebsite = reader.nextString()
                "stickers" -> stickerList = readStickers(reader)
                "image_data_version" -> imageDataVersion = reader.nextString()
                "avoid_cache" -> avoidCache = reader.nextBoolean()
                else -> reader.skipValue()
            }
        }
        check(!TextUtils.isEmpty(identifier)) { "identifier cannot be empty" }
        check(!TextUtils.isEmpty(name)) { "name cannot be empty" }
        check(!TextUtils.isEmpty(publisher)) { "publisher cannot be empty" }
        check(!TextUtils.isEmpty(trayImageFile)) { "tray_image_file cannot be empty" }
        check(!(stickerList == null || stickerList.size == 0)) { "sticker list is empty" }
        check(!(identifier!!.contains("..") || identifier.contains("/"))) { "identifier should not contain .. or / to prevent directory traversal" }
        check(!TextUtils.isEmpty(imageDataVersion)) { "image_data_version should not be empty" }
        reader.endObject()
        val stickerPack = StickerPack(identifier, name, publisher, trayImageFile, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, avoidCache)
        stickerPack.stickers = stickerList
        return stickerPack
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun readStickers(reader: JsonReader): List<Sticker?> {
        reader.beginArray()
        val stickerList: MutableList<Sticker?> = ArrayList()
        while (reader.hasNext()) {
            reader.beginObject()
            var imageFile: String? = null
            val emojis: MutableList<String> = ArrayList(LIMIT_EMOJI_COUNT)
            while (reader.hasNext()) {
                val key = reader.nextName()
                if ("image_file" == key) {
                    imageFile = reader.nextString()
                } else if ("emojis" == key) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val emoji = reader.nextString()
                        emojis.add(emoji)
                    }
                    reader.endArray()
                } else {
                    throw IllegalStateException("unknown field in json: $key")
                }
            }
            reader.endObject()
            check(!TextUtils.isEmpty(imageFile)) { "sticker image_file cannot be empty" }
            check(imageFile!!.endsWith(".webp")) { "image file for stickers should be webp files, image file is: $imageFile" }
            check(!(imageFile.contains("..") || imageFile.contains("/"))) { "the file name should not contain .. or / to prevent directory traversal, image file is:$imageFile" }
            stickerList.add(Sticker(imageFile, emojis))
        }
        reader.endArray()
        return stickerList
    }
}