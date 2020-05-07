/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.linuxauthority.stickers

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import java.io.FileNotFoundException
import timber.log.Timber.d as log

class StickerPackInfoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticker_pack_info)
        val trayIconUriString = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_TRAY_ICON)
        val website = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_WEBSITE)
        val email = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_EMAIL)
        val privacyPolicy = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_PRIVACY_POLICY)
        val licenseAgreement = intent.getStringExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_LICENSE_AGREEMENT)
        val trayIcon = findViewById<TextView>(R.id.tray_icon)
        try {
            val inputStream = contentResolver.openInputStream(Uri.parse(trayIconUriString))
            val trayDrawable = BitmapDrawable(resources, inputStream)
            val emailDrawable = getDrawableForAllAPIs(R.drawable.sticker_3rdparty_email)
            trayDrawable.bounds = Rect(0, 0, emailDrawable!!.intrinsicWidth, emailDrawable.intrinsicHeight)
            if (Build.VERSION.SDK_INT > 17) {
                trayIcon.setCompoundDrawablesRelative(trayDrawable, null, null, null)
            } else {
                if (ViewCompat.getLayoutDirection(trayIcon) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    trayIcon.setCompoundDrawables(null, null, trayDrawable, null)
                } else {
                    trayIcon.setCompoundDrawables(trayDrawable, null, null, null)
                }
            }
        } catch (e: FileNotFoundException) {
            log("$TAG: could not find the uri for the tray image:$trayIconUriString")
        }
        setupTextView(website, R.id.view_webpage)
        val sendEmail = findViewById<TextView>(R.id.send_email)
        if (TextUtils.isEmpty(email)) {
            sendEmail.visibility = View.GONE
        } else {
            sendEmail.setOnClickListener { v: View? -> launchEmailClient(email) }
        }
        setupTextView(privacyPolicy, R.id.privacy_policy)
        setupTextView(licenseAgreement, R.id.license_agreement)
    }

    private fun setupTextView(website: String, @IdRes textViewResId: Int) {
        val viewWebpage = findViewById<TextView>(textViewResId)
        if (TextUtils.isEmpty(website)) {
            viewWebpage.visibility = View.GONE
        } else {
            viewWebpage.setOnClickListener { v: View? -> launchWebpage(website) }
        }
    }

    private fun launchEmailClient(email: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null))
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        startActivity(Intent.createChooser(emailIntent, resources.getString(R.string.info_send_email_to_prompt)))
    }

    private fun launchWebpage(website: String) {
        val uri = Uri.parse(website)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun getDrawableForAllAPIs(@DrawableRes id: Int): Drawable? {
        return if (Build.VERSION.SDK_INT >= 21) {
            getDrawable(id)
        } else {
            resources.getDrawable(id)
        }
    }

    companion object {
        private const val TAG = "StickerPackInfoActivity"
    }
}