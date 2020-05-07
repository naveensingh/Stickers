/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.linuxauthority.stickers

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.linuxauthority.stickers.StickerPackLoader.getStickerAssetUri
import com.linuxauthority.stickers.WhitelistCheck.isWhitelisted
import java.lang.ref.WeakReference

class StickerPackDetailsActivity : AddStickerPackActivity() {
    private var recyclerView: RecyclerView? = null
    private var layoutManager: GridLayoutManager? = null
    private var stickerPreviewAdapter: StickerPreviewAdapter? = null
    private var numColumns = 0
    private var addButton: View? = null
    private var alreadyAddedText: View? = null
    private var stickerPack: StickerPack? = null
    private var divider: View? = null
    private var whiteListCheckAsyncTask: WhiteListCheckAsyncTask? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticker_pack_details)
        val showUpButton = intent.getBooleanExtra(EXTRA_SHOW_UP_BUTTON, false)
        stickerPack = intent.getParcelableExtra(EXTRA_STICKER_PACK_DATA)
        val packNameTextView = findViewById<TextView>(R.id.pack_name)
        val packPublisherTextView = findViewById<TextView>(R.id.author)
        val packTrayIcon = findViewById<ImageView>(R.id.tray_image)
        val packSizeTextView = findViewById<TextView>(R.id.pack_size)
        addButton = findViewById(R.id.add_to_whatsapp_button)
        alreadyAddedText = findViewById(R.id.already_added_text)
        layoutManager = GridLayoutManager(this, 1)
        recyclerView = findViewById(R.id.sticker_list)
        recyclerView?.layoutManager = layoutManager
        recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(pageLayoutListener)
        recyclerView?.addOnScrollListener(dividerScrollListener)
        divider = findViewById(R.id.divider)
        if (stickerPreviewAdapter == null) {
            stickerPreviewAdapter = StickerPreviewAdapter(layoutInflater, R.drawable.sticker_error, resources.getDimensionPixelSize(R.dimen.sticker_pack_details_image_size), resources.getDimensionPixelSize(R.dimen.sticker_pack_details_image_padding), stickerPack!!)
            recyclerView?.adapter = stickerPreviewAdapter
        }
        packNameTextView.text = stickerPack?.name
        packPublisherTextView.text = stickerPack?.publisher
        packTrayIcon.setImageURI(getStickerAssetUri(stickerPack?.identifier, stickerPack?.trayImageFile))
        packSizeTextView.text = Formatter.formatShortFileSize(this, stickerPack!!.totalSize)
        addButton?.setOnClickListener { v: View? -> addStickerPackToWhatsApp(stickerPack!!.identifier, stickerPack!!.name) }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(showUpButton)
            supportActionBar!!.title = if (showUpButton) resources.getString(R.string.title_activity_sticker_pack_details_multiple_pack) else resources.getQuantityString(R.plurals.title_activity_sticker_packs_list, 1)
        }
    }

    private fun launchInfoActivity(publisherWebsite: String, publisherEmail: String, privacyPolicyWebsite: String, licenseAgreementWebsite: String, trayIconUriString: String) {
        val intent = Intent(this@StickerPackDetailsActivity, StickerPackInfoActivity::class.java)
        intent.putExtra(EXTRA_STICKER_PACK_ID, stickerPack!!.identifier)
        intent.putExtra(EXTRA_STICKER_PACK_WEBSITE, publisherWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_EMAIL, publisherEmail)
        intent.putExtra(EXTRA_STICKER_PACK_PRIVACY_POLICY, privacyPolicyWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_LICENSE_AGREEMENT, licenseAgreementWebsite)
        intent.putExtra(EXTRA_STICKER_PACK_TRAY_ICON, trayIconUriString)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_info && stickerPack != null) {
            val trayIconUri = getStickerAssetUri(stickerPack!!.identifier, stickerPack!!.trayImageFile)
            launchInfoActivity(stickerPack!!.publisherWebsite, stickerPack!!.publisherEmail, stickerPack!!.privacyPolicyWebsite, stickerPack!!.licenseAgreementWebsite, trayIconUri.toString())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val pageLayoutListener = OnGlobalLayoutListener { setNumColumns(recyclerView!!.width / recyclerView!!.context.resources.getDimensionPixelSize(R.dimen.sticker_pack_details_image_size)) }

    private fun setNumColumns(numColumns: Int) {
        if (this.numColumns != numColumns) {
            layoutManager!!.spanCount = numColumns
            this.numColumns = numColumns
            if (stickerPreviewAdapter != null) {
                stickerPreviewAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private val dividerScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            updateDivider(recyclerView)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateDivider(recyclerView)
        }

        private fun updateDivider(recyclerView: RecyclerView) {
            val showDivider = recyclerView.computeVerticalScrollOffset() > 0
            if (divider != null) {
                divider!!.visibility = if (showDivider) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        whiteListCheckAsyncTask = WhiteListCheckAsyncTask(this)
        whiteListCheckAsyncTask!!.execute(stickerPack)
    }

    override fun onPause() {
        super.onPause()
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask!!.isCancelled) {
            whiteListCheckAsyncTask!!.cancel(true)
        }
    }

    private fun updateAddUI(isWhitelisted: Boolean) {
        if (isWhitelisted) {
            addButton!!.visibility = View.GONE
            alreadyAddedText!!.visibility = View.VISIBLE
        } else {
            addButton!!.visibility = View.VISIBLE
            alreadyAddedText!!.visibility = View.GONE
        }
    }

    internal class WhiteListCheckAsyncTask(stickerPackListActivity: StickerPackDetailsActivity) : AsyncTask<StickerPack?, Void?, Boolean>() {
        private val stickerPackDetailsActivityWeakReference: WeakReference<StickerPackDetailsActivity> = WeakReference(stickerPackListActivity)
        override fun doInBackground(vararg stickerPacks: StickerPack?): Boolean {
            val stickerPack = stickerPacks[0]
            val stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get()
                    ?: return false
            return isWhitelisted(stickerPackDetailsActivity, stickerPack!!.identifier)
        }

        override fun onPostExecute(isWhitelisted: Boolean) {
            val stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get()
            stickerPackDetailsActivity?.updateAddUI(isWhitelisted)
        }

    }

    companion object {
        /**
         * Do not change below values of below 3 lines as this is also used by WhatsApp
         */
        const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
        const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
        const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"
        const val EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website"
        const val EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email"
        const val EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy"
        const val EXTRA_STICKER_PACK_LICENSE_AGREEMENT = "sticker_pack_license_agreement"
        const val EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon"
        const val EXTRA_SHOW_UP_BUTTON = "show_up_button"
        const val EXTRA_STICKER_PACK_DATA = "sticker_pack"
    }
}