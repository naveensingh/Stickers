/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.linuxauthority.stickers

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.*
import timber.log.Timber.d as log

class EntryActivity : BaseActivity() {
    private var progressBar: View? = null
    private var loadListAsyncTask: LoadListAsyncTask? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        overridePendingTransition(0, 0)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        progressBar = findViewById(R.id.entry_activity_progress)
        loadListAsyncTask = LoadListAsyncTask(this)
        loadListAsyncTask!!.execute()
    }

    private fun showStickerPack(stickerPackList: ArrayList<StickerPack>?) {
        progressBar!!.visibility = View.GONE
        if (stickerPackList!!.size > 1) {
            val intent = Intent(this, StickerPackListActivity::class.java)
            intent.putParcelableArrayListExtra(StickerPackListActivity.EXTRA_STICKER_PACK_LIST_DATA, stickerPackList)
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        } else {
            val intent = Intent(this, StickerPackDetailsActivity::class.java)
            intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, false)
            intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, stickerPackList[0])
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun showErrorMessage(errorMessage: String?) {
        progressBar!!.visibility = View.GONE
        log("EntryActivity: error fetching sticker packs, $errorMessage")
        val errorMessageTV = findViewById<TextView>(R.id.error_message)
        errorMessageTV.text = getString(R.string.error_message, errorMessage)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (loadListAsyncTask != null && !loadListAsyncTask!!.isCancelled) {
            loadListAsyncTask!!.cancel(true)
        }
    }

    internal class LoadListAsyncTask(activity: EntryActivity) : AsyncTask<Void?, Void?, Pair<String?, ArrayList<StickerPack>?>>() {
        private val contextWeakReference: WeakReference<EntryActivity> = WeakReference(activity)
        override fun doInBackground(vararg voids: Void?): Pair<String?, ArrayList<StickerPack>?> {
            val stickerPackList: ArrayList<StickerPack>
            return try {
                val context: Context? = contextWeakReference.get()
                if (context != null) {
                    stickerPackList = StickerPackLoader.fetchStickerPacks(context)
                    if (stickerPackList.size == 0) {
                        return Pair("could not find any packs", null)
                    }
                    for (stickerPack in stickerPackList) {
                        StickerPackValidator.verifyStickerPackValidity(context, stickerPack)
                    }
                    Pair(null, stickerPackList)
                } else {
                    Pair("could not fetch sticker packs", null)
                }
            } catch (e: Exception) {
                log("EntryActivity: error fetching sticker packs. Exception: $e")
                Pair(e.message, null)
            }
        }

        override fun onPostExecute(stringListPair: Pair<String?, ArrayList<StickerPack>?>) {
            val entryActivity = contextWeakReference.get()
            if (entryActivity != null) {
                if (stringListPair.first != null) {
                    entryActivity.showErrorMessage(stringListPair.first)
                } else {
                    entryActivity.showStickerPack(stringListPair.second)
                }
            }
        }

    }
}