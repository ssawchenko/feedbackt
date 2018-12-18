package com.steamclock.feedbackt.activities

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.steamclock.feedbackt.R

class EditFeedbackShareBottomSheet : BottomSheetDialogFragment() {
    interface MethodSelectionListener {
        fun onSaveToGallery()
        fun onEmail()
    }

    var methodSelectionListener: MethodSelectionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view = View.inflate(context, R.layout.bottom_sheet_edit_feedbackt_share, null)
        dialog.setContentView(view)

        // Synthetic properties do not appear to be available here, use findViewById (Kotlin bug)?
        view.findViewById<View>(R.id.share_email).setOnClickListener {
            methodSelectionListener?.onEmail()
            dismiss()
        }

        view.findViewById<View>(R.id.share_save_to_gallery).setOnClickListener {
            methodSelectionListener?.onSaveToGallery()
            dismiss()
        }

        return dialog
    }

    companion object {
        fun newInstance(): EditFeedbackShareBottomSheet {
            return EditFeedbackShareBottomSheet()
        }
    }
}