package com.seoultech.onde

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog

class MemoDialogFragment(
    private val date: String,
    private val dbHelper: MemoDatabaseHelper,
    private val onSave: () -> Unit // 저장 후 콜백 함수 추가
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.activity_dialog_memo, null)

        val memoEditText: EditText = view.findViewById(R.id.memoEditText)
        val savedMemo = dbHelper.getMemo(date)
        memoEditText.setText(savedMemo) // 저장된 메모가 있으면 불러오기

        builder.setView(view)
            .setTitle("$date\nIs there any Event?")
            .setPositiveButton("Save") { _, _ ->
                val memo = memoEditText.text.toString()
                dbHelper.saveMemo(date, memo)
                onSave() // 저장 후 콜백 호출
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }
}
