package com.websarva.wings.android.todo

import android.content.Context
import androidx.appcompat.app.AlertDialog

class DialogHelper(private val context: Context) {
    fun showErrorDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss() // OKボタンを押したときにダイアログを閉じる
        }
        val dialog = builder.create()
        dialog.show()
    }
}