package com.example.demo1

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DndPermissionDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setTitle("Permission Required")
            .setMessage(
                "This app needs Do Not Disturb access to make Silent mode " +
                "completely silent on Samsung devices.\n\n" +
                "Without this permission, your phone will still vibrate " +
                "even when Silent mode is selected.\n\n" +
                "On the next screen, find this app in the list and toggle it ON."
            )
            .setIcon(R.drawable.ic_silent)
            .setPositiveButton("Grant Permission") { _, _ ->
                (activity as? MainActivity)?.openDndSettings()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(Color.parseColor("#4CAF50")) // GREEN
            negativeButton.setTextColor(Color.parseColor("#FF9800")) // ORANGE
        }

        return dialog
    }
}