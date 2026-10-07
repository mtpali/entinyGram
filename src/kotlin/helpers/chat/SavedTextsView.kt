package desu.inugram.helpers.chat

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.Emoji
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.Utilities
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.PagerSlidingTabStrip
import org.telegram.ui.Components.RecyclerListView

class SavedTextsView(
    context: Context,
    private val fragment: BaseFragment?,
    account: Int,
    private val resourcesProvider: Theme.ResourcesProvider?,
    private val onSelect: Utilities.Callback<String>,
) : FrameLayout(context) {
    private val preferences = ApplicationLoader.applicationContext.getSharedPreferences("inu_saved_texts", Context.MODE_PRIVATE)
    private val key = "texts_${UserConfig.getInstance(account).clientUserId}"
    private val texts = ArrayList<String>()
    private val textAdapter = TextAdapter()
    private val addButton = TextView(context)
    private val emptyView = TextView(context)
    private val listView = RecyclerListView(context, resourcesProvider)

    init {
        layoutDirection = if (LocaleController.isRTL) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        addButton.apply {
            text = LocaleController.getString(R.string.InuSavedTextsAdd)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = AndroidUtilities.bold()
            gravity = Gravity.CENTER
            setOnClickListener { editText(-1) }
        }
        addView(addButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP))
        emptyView.apply {
            text = LocaleController.getString(R.string.InuSavedTextsEmpty)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            gravity = Gravity.CENTER
            setPadding(AndroidUtilities.dp(24f), 0, AndroidUtilities.dp(24f), 0)
        }
        addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat(), Gravity.TOP, 0f, 48f, 0f, 48f))
        listView.layoutManager = LinearLayoutManager(context)
        listView.adapter = textAdapter
        listView.setEmptyView(emptyView)
        listView.setOnItemClickListener { _, position -> texts.getOrNull(position)?.let(onSelect::run) }
        listView.setOnItemLongClickListener { _, position -> showOptions(position); true }
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat(), Gravity.TOP, 0f, 48f, 0f, 48f))
        reload()
        updateColors()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reload()
        updateColors()
    }

    private fun reload() {
        texts.clear()
        try {
            val stored = JSONArray(preferences.getString(key, "[]"))
            for (i in 0 until stored.length()) {
                val text = stored.optString(i)
                if (text.isNotBlank()) texts.add(text)
            }
        } catch (e: Exception) {
            FileLog.e(e)
        }
        textAdapter.notifyDataSetChanged()
    }

    private fun save() {
        preferences.edit().putString(key, JSONArray(texts).toString()).apply()
        textAdapter.notifyDataSetChanged()
    }

    fun updateColors() {
        addButton.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelIconSelected, resourcesProvider))
        addButton.background = Theme.getSelectorDrawable(false)
        emptyView.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelStickerPackSelector, resourcesProvider))
        textAdapter.notifyDataSetChanged()
    }

    fun scrollToTop() {
        listView.smoothScrollToPosition(0)
    }

    private fun showDialog(dialog: AlertDialog) {
        if (fragment != null) fragment.showDialog(dialog) else dialog.show()
    }

    private fun showOptions(position: Int) {
        if (position !in texts.indices) return
        showDialog(AlertDialog.Builder(context, resourcesProvider)
            .setItems(arrayOf(LocaleController.getString(R.string.Edit), LocaleController.getString(R.string.Delete))) { _, which ->
                if (which == 0) {
                    editText(position)
                } else {
                    showDialog(AlertDialog.Builder(context, resourcesProvider)
                        .setTitle(LocaleController.getString(R.string.Delete))
                        .setMessage(LocaleController.getString(R.string.InuSavedTextsDeleteConfirm))
                        .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                        .setPositiveButton(LocaleController.getString(R.string.Delete)) { _, _ ->
                            if (position in texts.indices) { texts.removeAt(position); save() }
                        }.create())
                }
            }.create())
    }

    private fun editText(position: Int) {
        val input = EditTextBoldCursor(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider))
            setCursorColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setSingleLine(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            maxLines = 8
            gravity = Gravity.TOP or Gravity.START
            hint = LocaleController.getString(R.string.InuSavedTextsHint)
            setPadding(0, AndroidUtilities.dp(8f), 0, AndroidUtilities.dp(8f))
            setText(texts.getOrNull(position).orEmpty())
            setSelection(length())
        }
        val container = FrameLayout(context).apply {
            addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), Gravity.TOP, 24f, 8f, 24f, 0f))
        }
        val dialog = AlertDialog.Builder(context, resourcesProvider)
            .setTitle(LocaleController.getString(if (position < 0) R.string.InuSavedTextsAdd else R.string.InuSavedTextsEdit))
            .setView(container)
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .setPositiveButton(LocaleController.getString(R.string.Save)) { _, _ -> }
            .create()
        dialog.setOnShowListener {
            input.requestFocus()
            AndroidUtilities.showKeyboard(input)
        }
        showDialog(dialog)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
            val value = input.text?.toString().orEmpty()
            if (value.isBlank()) {
                AndroidUtilities.shakeView(input)
            } else {
                if (position in texts.indices) texts[position] = value else texts.add(value)
                save()
                dialog.dismiss()
            }
        }
    }

    private inner class TextRow(context: Context) : FrameLayout(context) {
        val preview = TextView(context)
        val more = ImageView(context)

        init {
            minimumHeight = AndroidUtilities.dp(64f)
            preview.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.START
            }
            addView(preview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), Gravity.TOP,
                if (LocaleController.isRTL) 52f else 16f, 12f, if (LocaleController.isRTL) 16f else 52f, 12f))
            more.apply {
                setImageResource(R.drawable.ic_ab_other)
                scaleType = ImageView.ScaleType.CENTER
                contentDescription = LocaleController.getString(R.string.InuSavedTextsOptions)
            }
            addView(more, LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL or (if (LocaleController.isRTL) Gravity.LEFT else Gravity.RIGHT)))
        }
    }

    private inner class TextAdapter : RecyclerListView.SelectionAdapter() {
        override fun getItemCount(): Int = texts.size
        override fun isEnabled(holder: RecyclerView.ViewHolder): Boolean = true
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            RecyclerListView.Holder(TextRow(context).apply { layoutParams = RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT) })

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = holder.itemView as TextRow
            row.preview.text = texts[position]
            row.preview.setTextColor(Theme.getColor(Theme.key_chat_emojiPanelIcon, resourcesProvider))
            row.more.setColorFilter(Theme.getColor(Theme.key_chat_emojiPanelStickerPackSelector, resourcesProvider))
            row.more.background = Theme.getSelectorDrawable(false)
            row.more.setOnClickListener { showOptions(holder.adapterPosition) }
        }
    }

    companion object {
        @JvmStatic
        fun configureTabs(tabs: PagerSlidingTabStrip) {
            tabs.setShouldExpand(true)
            tabs.layoutParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48f, Gravity.BOTTOM, 48f, 0f, 48f, 0f)
            for (i in 0..3) {
                (tabs.getTab(i) as? TextView)?.apply {
                    setSingleLine(true)
                    ellipsize = TextUtils.TruncateAt.END
                }
            }
        }

        @JvmStatic
        fun insertText(input: EditTextBoldCursor?, text: String) {
            val editable = input?.text ?: return
            val start = input.selectionStart.takeIf { it >= 0 } ?: editable.length
            val end = input.selectionEnd.takeIf { it >= 0 } ?: start
            val from = minOf(start, end).coerceIn(0, editable.length)
            val to = maxOf(start, end).coerceIn(from, editable.length)
            val previousLength = editable.length
            editable.replace(from, to, Emoji.replaceEmoji(text, input.paint.fontMetricsInt, false, null))
            val insertedLength = editable.length - previousLength + (to - from)
            input.setSelection((from + insertedLength).coerceIn(0, editable.length))
        }
    }
}
