package com.mustafacanyucel.firefly_shortcuts.ui.sms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafacanyucel.firefly_shortcuts.AppContainer
import com.mustafacanyucel.firefly_shortcuts.data.db.SmsTemplateEntity
import com.mustafacanyucel.firefly_shortcuts.sms.FieldKind
import com.mustafacanyucel.firefly_shortcuts.sms.FieldMark
import com.mustafacanyucel.firefly_shortcuts.sms.Piece
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import com.mustafacanyucel.firefly_shortcuts.sms.SmsParseResult
import com.mustafacanyucel.firefly_shortcuts.sms.SmsTemplate
import com.mustafacanyucel.firefly_shortcuts.sms.TemplateProblem
import com.mustafacanyucel.firefly_shortcuts.sms.buildParts
import com.mustafacanyucel.firefly_shortcuts.sms.guessDateFormat
import com.mustafacanyucel.firefly_shortcuts.sms.guessDecimalMark
import com.mustafacanyucel.firefly_shortcuts.sms.marksFor
import com.mustafacanyucel.firefly_shortcuts.sms.parse
import com.mustafacanyucel.firefly_shortcuts.sms.problems
import com.mustafacanyucel.firefly_shortcuts.sms.splitSample
import com.mustafacanyucel.firefly_shortcuts.sms.suggestMarks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DATE_KINDS = setOf(FieldKind.DATE, FieldKind.TIME, FieldKind.DATE_TIME)

/** Edits the SMS template with [templateId], or creates one when it's 0. */
class SmsTemplateEditorViewModel(
    private val container: AppContainer,
    private val templateId: Long,
) : ViewModel() {
    val isNew = templateId == 0L

    var name by mutableStateOf("")
    /** Comma-separated, as typed. */
    var senders by mutableStateOf("")
    var action by mutableStateOf(SmsAction.WITHDRAWAL)
    var decimalMark by mutableStateOf<Char?>(null)
    var autoPost by mutableStateOf(false)

    /** The text in the paste box, before it's used as the sample. */
    var draftSample by mutableStateOf("")
    /** An optional second message of the same kind; what differs from the sample gets marked. */
    var draftSecond by mutableStateOf("")
    /** True while waiting for the next incoming SMS to use as the sample. */
    var waitingForSms by mutableStateOf(false)
        private set
    /** The sender of the SMS that was received while waiting, as Android reports it. */
    var receivedFrom by mutableStateOf<String?>(null)
        private set
    private var waitJob: Job? = null

    /** Says what comparing with another message marked. */
    var suggestionNote by mutableStateOf<String?>(null)
        private set
    var sample by mutableStateOf("")
        private set
    var pieces by mutableStateOf(emptyList<Piece>())
        private set
    var marks by mutableStateOf(emptyList<FieldMark>())
        private set

    /** The pieces picked for the next mark. */
    var selection by mutableStateOf<IntRange?>(null)
        private set

    /** True after a single tap, so the next tap extends the selection into a range. */
    private var extending = false

    var testMessage by mutableStateOf("")

    var loaded by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var finished by mutableStateOf(false)
        private set

    /** Saved templates whose sample no longer lines up are reopened with no marks. */
    var marksLost by mutableStateOf(false)
        private set

    private var showErrors by mutableStateOf(false)
    val nameError get() = showErrors && name.isBlank()

    init {
        viewModelScope.launch {
            if (!isNew) {
                val entity = container.database.smsTemplateDao().getById(templateId)
                if (entity == null) {
                    finished = true
                    return@launch
                }
                val t = entity.template
                name = t.name
                senders = t.senders.joinToString(", ")
                action = t.action
                decimalMark = t.decimalMark
                autoPost = t.autoPost
                loadSample(entity.sample)
                val found = marksFor(t, sample, pieces)
                marks = found.orEmpty()
                marksLost = found == null
            }
            loaded = true
        }
    }

    val template: SmsTemplate
        get() = SmsTemplate(
            name = name.trim(),
            senders = senders.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            parts = buildParts(sample, pieces, marks),
            action = action,
            decimalMark = decimalMark.takeIf { marks.any { it.kind == FieldKind.AMOUNT } },
            autoPost = autoPost && action != SmsAction.IGNORE,
        )

    /** What the template reads from its own sample, once something is marked. */
    val sampleResult: SmsParseResult?
        get() = if (marks.isEmpty() && action != SmsAction.IGNORE) null else template.parse(sample)

    val testResult: SmsParseResult? get() = testMessage.takeIf { it.isNotBlank() }?.let { template.parse(it) }

    /** Problems to list under the form. Shown once the user tries to save. */
    val problems: List<String>
        get() {
            if (!showErrors) return emptyList()
            val list = mutableListOf<String>()
            if (sample.isEmpty()) list += "Paste a message from your bank."
            else if (marks.isEmpty() && action != SmsAction.IGNORE) {
                list += "Mark the parts that change from message to message, like the amount and merchant."
            }
            template.problems().forEach { list += it.text }
            val result = sampleResult
            if (sample.isNotEmpty() && result is SmsParseResult.BadField) {
                list += "The ${result.kind.label.lowercase()} \"${result.text}\" can't be read. Check its pattern."
            }
            return list
        }

    /**
     * Waits for the next incoming SMS and puts it in the paste box, adding its sender to the
     * sender names. The message is held in memory only.
     */
    fun waitForSms() {
        val capture = container.smsCapture
        waitJob?.cancel()
        capture.start()
        waitingForSms = true
        receivedFrom = null
        waitJob = viewModelScope.launch {
            val sms = capture.message.filterNotNull().first()
            capture.stop()
            waitingForSms = false
            draftSample = sms.body
            receivedFrom = sms.sender
            val names = senders.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (names.none { it.equals(sms.sender, ignoreCase = true) }) {
                senders = (names + sms.sender).joinToString(", ")
            }
        }
    }

    fun stopWaiting() {
        waitJob?.cancel()
        container.smsCapture.stop()
        waitingForSms = false
    }

    override fun onCleared() {
        if (waitingForSms) container.smsCapture.stop()
    }

    fun useDraftSample() {
        loadSample(draftSample)
        marks = emptyList()
        marksLost = false
        decimalMark = null
        suggestionNote = null
        if (draftSecond.isNotBlank()) {
            testMessage = draftSecond
            markDifferences(draftSecond)
        }
    }

    /** Marks what differs between the sample and the test message, leaving existing marks alone. */
    fun markDifferencesFromTest() = markDifferences(testMessage)

    private fun markDifferences(other: String) {
        val taken = marks.map { it.kind }.filter { it != FieldKind.IGNORE }.toSet()
        val suggested = suggestMarks(sample, pieces, splitSample(other.trim()), taken)
            .filter { s -> marks.none { it.overlaps(s.range) } }
        marks = marks + suggested
        suggested.firstOrNull { it.kind == FieldKind.AMOUNT }
            ?.let { guessDecimalMark(textOf(it)) }
            ?.let { decimalMark = it }
        suggestionNote = if (suggested.isEmpty()) {
            "Nothing new differs between the two messages."
        } else {
            "Marked ${suggested.size} parts that differ between the two messages. Check them, and " +
                "mark anything that happened to be the same in both, like the card ending."
        }
        clearSelection()
    }

    fun changeSample() {
        suggestionNote = null
        draftSample = sample
        sample = ""
        pieces = emptyList()
        marks = emptyList()
        selection = null
    }

    private fun loadSample(text: String) {
        // Line breaks and outer spaces don't matter when matching, so keep the sample simple.
        sample = text.trim()
        pieces = splitSample(sample)
        selection = null
        extending = false
    }

    fun markAt(index: Int): FieldMark? = marks.firstOrNull { index in it.range }

    /**
     * First tap picks a piece, or a whole field if the piece is marked. A second tap after a
     * single piece extends the selection to a range.
     */
    fun tapPiece(index: Int) {
        val current = selection
        if (current != null && extending) {
            selection = minOf(current.first, index)..maxOf(current.first, index)
            extending = false
            return
        }
        val mark = markAt(index)
        selection = mark?.range ?: index..index
        extending = mark == null
    }

    fun clearSelection() {
        selection = null
        extending = false
    }

    val selectedText: String
        get() = selection?.let { sample.substring(pieces[it.first].start, pieces[it.last].end) }.orEmpty()

    val selectionIsMarked: Boolean
        get() = selection?.let { sel -> marks.any { it.overlaps(sel) } } == true

    fun markSelection(kind: FieldKind) {
        val sel = selection ?: return
        val text = selectedText
        val format = if (kind in DATE_KINDS) guessDateFormat(kind, text) else null
        // Only one of each field (except Ignore): marking it again moves it.
        marks = marks.filterNot { it.overlaps(sel) || (kind != FieldKind.IGNORE && it.kind == kind) } +
            FieldMark(sel.first, sel.last, kind, format)
        if (kind == FieldKind.AMOUNT) guessDecimalMark(text)?.let { decimalMark = it }
        clearSelection()
    }

    fun unmarkSelection() {
        val sel = selection ?: return
        marks = marks.filterNot { it.overlaps(sel) }
        clearSelection()
    }

    fun setFormat(mark: FieldMark, format: String) {
        marks = marks.map { if (it == mark) it.copy(format = format) else it }
    }

    fun textOf(mark: FieldMark): String = sample.substring(pieces[mark.first].start, pieces[mark.last].end)

    fun save() {
        if (saving) return
        showErrors = true
        if (name.isBlank() || problems.isNotEmpty()) return
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                container.database.smsTemplateDao().save(SmsTemplateEntity(templateId, template, sample))
                finished = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveError = "Couldn't save: ${e.message}"
                saving = false
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            container.database.smsTemplateDao().delete(templateId)
            finished = true
        }
    }
}

private fun FieldMark.overlaps(other: IntRange) = first <= other.last && other.first <= last

private val TemplateProblem.text: String
    get() = when (this) {
        TemplateProblem.NO_SENDER -> "Add the sender name."
        TemplateProblem.NO_FIXED_TEXT ->
            "Leave the bank's fixed wording unmarked. It's how the template recognises the message."
        TemplateProblem.DUPLICATE_FIELD -> "Each field can be marked only once, except Ignore."
        TemplateProblem.DATE_TWICE -> "Mark either a date and time, or a date and a time separately."
        TemplateProblem.BAD_DATE_FORMAT -> "A date or time pattern doesn't work. Check the patterns."
    }
