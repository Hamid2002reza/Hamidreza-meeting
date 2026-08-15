
package ir.hamidreza.meeting

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaRecorder
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var db: MeetingDb
    private var form: LinearLayout? = null
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var liveTranscriptMeetingId: Long = -1L
    private var liveTranscriptRunning = false
    private var liveTranscriptText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = MeetingDb(this)
        dashboard()
    }

    private fun root() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(28,24,28,24)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
    }

    private fun title(s:String, size:Float=25f)=TextView(this).apply {
        text=s; textSize=size; setTypeface(null,1); setPadding(0,0,0,18)
    }

    private fun btn(s:String, action:()->Unit)=com.google.android.material.button.MaterialButton(this).apply {
        text=s; setOnClickListener{action()}
    }

    private fun field(h:String):EditText {
        val e=EditText(this).apply { hint=h; setPadding(8,8,8,8) }
        form!!.addView(e, LinearLayout.LayoutParams(-1,-2))
        return e
    }

    private fun dashboard() {
        val r=root()
        r.addView(title("Hamidreza Meeting"))
        r.addView(TextView(this).apply {
            text="دستیار حرفه‌ای جلسات فنی و مدیریتی"; textSize=16f
            setPadding(0,0,0,22)
        })
        r.addView(TextView(this).apply {
            text="جلسات: ${db.meetings().size}   |   وظایف باز: ${db.openTasks().size}"
            textSize=15f; setPadding(0,0,0,18)
        })
        r.addView(btn("＋ جلسه جدید"){newMeeting()})
        r.addView(btn("📋 جلسات"){meetings()})
        r.addView(btn("✅ Action Items"){tasks()})
        r.addView(btn("🎙️ ضبط جلسه"){selectMeetingForRecording()})
        r.addView(btn("🎤 Transcript زنده فارسی"){selectMeetingForLiveTranscript()})
        r.addView(btn("🔍 جست‌وجو"){searchDialog()})
        r.addView(btn("🔐 امنیت"){msg("امنیت","نسخه نهایی: PIN، Biometric، Auto-Lock و رمزنگاری اطلاعات حساس.")})
        r.addView(btn("📊 داشبورد مدیریتی"){ v8Dashboard() })
        r.addView(btn("🔔 Reminderها"){ v8Reminder() })
        r.addView(btn("🔐 امنیت و Biometric"){ v8Security() })
        r.addView(btn("🧠 تنظیم AI Backend"){aiSettingsDialog()})
        setContentView(r)
    }

    private fun newMeeting() {
        val r=root(); form=r; r.addView(title("جلسه جدید"))
        val t=field("عنوان جلسه *")
        val dt=field("تاریخ و ساعت")
        val host=field("برگزارکننده")
        val people=field("شرکت‌کنندگان")
        val place=field("محل / لینک")
        val type=field("نوع جلسه")
        val cat=field("حوزه فنی")
        val conf=field("محرمانگی: Normal / Confidential / Highly Confidential")
        val agenda=field("دستور جلسه")
        r.addView(btn("ذخیره"){
            if(t.text.isBlank()){t.error="عنوان الزامی است";return@btn}
            val id=db.addMeeting(t.text.toString(),dt.text.toString(),host.text.toString(),people.text.toString(),
                place.text.toString(),type.text.toString(),cat.text.toString(),conf.text.toString(),agenda.text.toString())
            detail(id)
        })
        r.addView(btn("لغو"){dashboard()})
        setContentView(r)
    }

    private fun meetings() {
        val r=root(); r.addView(title("جلسات"))
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        db.meetings().forEach{m->
            box.addView(btn("${m.title}\n${m.date} | ${m.category}"){detail(m.id)})
        }
        r.addView(ScrollView(this).apply{addView(box)},LinearLayout.LayoutParams(-1,0,1f))
        r.addView(btn("＋ جلسه جدید"){newMeeting()})
        r.addView(btn("بازگشت"){dashboard()})
        setContentView(r)
    }

    private fun detail(id:Long) {
        val m=db.get(id)?:return
        val r=root(); r.addView(title(m.title))
        r.addView(TextView(this).apply {
            text="تاریخ: ${m.date}\nبرگزارکننده: ${m.host}\nحاضرین: ${m.people}\nمحل: ${m.place}\nنوع: ${m.type}\nحوزه: ${m.category}\nمحرمانگی: ${m.confidentiality}\n\nAgenda:\n${m.agenda}"
            textSize=15f; setPadding(0,0,0,18)
        })
        r.addView(btn("🎙️ ضبط این جلسه"){startRecording(id)})
        r.addView(btn("📝 Transcript / متن جلسه"){transcriptDialog(id)})
        r.addView(btn("🎤 Transcript زنده فارسی"){startLiveTranscript(id)})
        r.addView(btn("🧠 تحلیل هوشمند جلسه"){aiAnalysis(id)})
        r.addView(btn("＋ موضوع فنی"){addTopic(id)})
        r.addView(btn("＋ تصمیم"){addDecision(id)})
        r.addView(btn("＋ Action Item"){addTask(id)})
        r.addView(btn("📄 گزارش مدیریتی Word"){exportDocx(id)})
        r.addView(btn("مشاهده موارد ثبت‌شده"){showNotes(id)})
        r.addView(btn("بازگشت"){meetings()})
        setContentView(r)
    }

    private fun selectMeetingForRecording() {
        val ms=db.meetings()
        if(ms.isEmpty()){msg("ضبط جلسه","ابتدا یک جلسه بسازید.");return}
        val names=ms.map{it.title}.toTypedArray()
        AlertDialog.Builder(this).setTitle("انتخاب جلسه").setItems(names){_,which->startRecording(ms[which].id)}.show()
    }

    private fun startRecording(meetingId:Long) {
        if(Build.VERSION.SDK_INT>=23 && ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),44)
            msg("مجوز لازم","برای ضبط جلسه، اجازه دسترسی به میکروفون را فعال کنید و دوباره تلاش کنید.")
            return
        }
        if(recorder!=null){msg("ضبط در حال انجام است","ابتدا ضبط فعلی را متوقف کنید.");return}
        recordingFile=File(filesDir,"meeting_${meetingId}_${System.currentTimeMillis()}.m4a")
        recorder=MediaRecorder(this).apply{
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(recordingFile!!.absolutePath)
            prepare(); start()
        }
        db.addRecording(meetingId,recordingFile!!.absolutePath)
        AlertDialog.Builder(this).setTitle("🎙️ ضبط فعال است")
            .setMessage("ضبط جلسه شروع شد. پس از پایان، توقف را بزنید.")
            .setNegativeButton("توقف"){_,_->stopRecording();detail(meetingId)}
            .setCancelable(false).show()
    }

    private fun stopRecording() {
        recorder?.runCatching{stop()}; recorder?.release(); recorder=null
        Toast.makeText(this,"ضبط ذخیره شد.",Toast.LENGTH_SHORT).show()
    }

    private fun selectMeetingForLiveTranscript() {
        val ms = db.meetings()
        if (ms.isEmpty()) { msg("Transcript", "ابتدا یک جلسه بسازید."); return }
        AlertDialog.Builder(this).setTitle("انتخاب جلسه").setItems(ms.map { it.title }.toTypedArray()) { _, which ->
            startLiveTranscript(ms[which].id)
        }.show()
    }

    private fun startLiveTranscript(id: Long) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 45)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            msg("Speech-to-Text", "سرویس تشخیص گفتار روی دستگاه در دسترس نیست.")
            return
        }
        liveTranscriptMeetingId = id
        liveTranscriptText = db.transcript(id)
        liveTranscriptRunning = true
        speechRecognizer?.destroy()
        val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        speechRecognizer = if (onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                           else SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { }
            override fun onPartialResults(partialResults: Bundle?) { }
            override fun onEvent(eventType: Int, params: Bundle?) { }
            override fun onError(error: Int) {
                if (!liveTranscriptRunning) return
                // SpeechRecognizer is segment based. Restarting creates a practical live-transcript loop.
                Handler(Looper.getMainLooper()).postDelayed({ if (liveTranscriptRunning) listenNextSegment() }, 350)
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val result = list?.firstOrNull()?.trim().orEmpty()
                if (result.isNotBlank()) {
                    liveTranscriptText = (liveTranscriptText.trim() + "\n" + result).trim()
                    db.saveTranscript(liveTranscriptMeetingId, liveTranscriptText)
                }
                if (liveTranscriptRunning) listenNextSegment()
            }
        })
        showLiveTranscriptDialog(id, onDevice)
        listenNextSegment()
    }

    private fun listenNextSegment() {
        if (!liveTranscriptRunning) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
        }
        try { speechRecognizer?.startListening(intent) }
        catch (_: Exception) { Handler(Looper.getMainLooper()).postDelayed({ if (liveTranscriptRunning) listenNextSegment() }, 500) }
    }

    private fun showLiveTranscriptDialog(id: Long, onDevice: Boolean) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8,8,8,8) }
        val status = TextView(this).apply { text = if (onDevice) "● تشخیص روی دستگاه فعال است" else "● تشخیص سیستم فعال است"; textSize = 14f }
        val text = EditText(this).apply { minLines = 12; gravity = Gravity.TOP; setText(liveTranscriptText); hint = "متن زنده جلسه..." }
        box.addView(status); box.addView(text)
        val dialog = AlertDialog.Builder(this).setTitle("🎤 Transcript زنده فارسی").setView(box)
            .setNegativeButton("پایان") { _, _ ->
                liveTranscriptRunning = false
                speechRecognizer?.cancel(); speechRecognizer?.destroy(); speechRecognizer = null
                db.saveTranscript(id, text.text.toString())
                detail(id)
            }.setPositiveButton("ذخیره") { _, _ ->
                liveTranscriptText = text.text.toString(); db.saveTranscript(id, liveTranscriptText)
                Toast.makeText(this, "Transcript ذخیره شد.", Toast.LENGTH_SHORT).show()
            }.create()
        dialog.setOnShowListener {
            val timer = object : Runnable {
                override fun run() {
                    if (dialog.isShowing && liveTranscriptRunning) {
                        if (text.text.toString() != liveTranscriptText) text.setText(liveTranscriptText)
                        text.setSelection(text.text.length)
                        Handler(Looper.getMainLooper()).postDelayed(this, 700)
                    }
                }
            }
            Handler(Looper.getMainLooper()).post(timer)
        }
        dialog.show()
    }

    private fun transcriptDialog(id:Long) {
        val e=EditText(this).apply{hint="متن جلسه را وارد/ویرایش کنید";minLines=10;gravity=Gravity.TOP}
        e.setText(db.transcript(id))
        AlertDialog.Builder(this).setTitle("Transcript").setView(e)
            .setNegativeButton("لغو",null).setPositiveButton("ذخیره"){_,_->db.saveTranscript(id,e.text.toString());detail(id)}.show()
    }

    private fun aiAnalysis(id:Long) {
        val m=db.get(id)?:return
        val transcript=db.transcript(id)
        if(transcript.isBlank()){msg("AI","ابتدا Transcript جلسه را ثبت کنید.");return}
        val endpoint=db.setting("ai_endpoint").trim()
        val token=db.setting("ai_token").trim()
        if(endpoint.isBlank() || !endpoint.startsWith("https://")){
            aiSettingsDialog(id); return
        }
        msg("AI","تحلیل در حال انجام است...")
        Thread {
            val result=runCatching{AIClient.analyze(endpoint,token,m,transcript)}
            runOnUiThread {
                result.onSuccess { json ->
                    db.saveAi(id,json)
                    showStructuredAi(id,json)
                }.onFailure { e -> msg("خطای AI", e.message ?: "ارتباط با Backend برقرار نشد.") }
            }
        }.start()
    }

    private fun aiSettingsDialog(id:Long=-1L){
        val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,10,20,10)}
        val endpoint=EditText(this).apply{hint="Backend URL (فقط HTTPS)، مثال: https://server.example.com";setText(db.setting("ai_endpoint"))}
        val token=EditText(this).apply{hint="Backend Bearer Token (اختیاری)";setText(db.setting("ai_token"));inputType=0x81}
        r.addView(endpoint);r.addView(token)
        AlertDialog.Builder(this).setTitle("تنظیم AI Backend").setView(r)
            .setNegativeButton("لغو",null).setPositiveButton("ذخیره"){_,_->
                db.setSetting("ai_endpoint",endpoint.text.toString().trim())
                db.setSetting("ai_token",token.text.toString().trim())
                msg("AI","تنظیمات ذخیره شد. API Key مدل AI داخل APK قرار نمی‌گیرد؛ فقط Backend آن را نگهداری می‌کند.")
            }.show()
    }

    private fun showStructuredAi(id:Long,json:String){
        val o=JSONObject(json)
        val summary=o.optString("executive_summary")
        val decisions=o.optJSONArray("decisions")?.join("\n• ") ?: ""
        val actions=o.optJSONArray("action_items")?.join("\n• ") ?: ""
        val risks=o.optJSONArray("risks")?.join("\n• ") ?: ""
        val manager=o.optJSONArray("management_attention")?.join("\n• ") ?: ""
        msg("گزارش AI","خلاصه مدیریتی:\n$summary\n\nتصمیمات:\n• $decisions\n\nAction Items:\n• $actions\n\nریسک‌ها:\n• $risks\n\nنیازمند توجه مدیریت:\n• $manager")
    }

    private fun addTopic(id:Long){
        val e=EditText(this);e.hint="موضوع فنی"
        AlertDialog.Builder(this).setTitle("موضوع فنی").setView(e).setNegativeButton("لغو",null)
            .setPositiveButton("ذخیره"){_,_->db.addTopic(id,e.text.toString());detail(id)}.show()
    }

    private fun addDecision(id:Long){
        val e=EditText(this);e.hint="تصمیم نهایی"
        AlertDialog.Builder(this).setTitle("تصمیم").setView(e).setNegativeButton("لغو",null)
            .setPositiveButton("ذخیره"){_,_->db.addDecision(id,e.text.toString());detail(id)}.show()
    }

    private fun addTask(id:Long){
        val r=root();form=r;r.addView(title("Action Item"))
        val t=field("شرح وظیفه *");val o=field("مسئول");val d=field("Deadline");val p=field("Priority")
        r.addView(btn("ذخیره"){
            if(t.text.isBlank()){t.error="شرح الزامی است";return@btn}
            db.addTask(id,t.text.toString(),o.text.toString(),d.text.toString(),p.text.toString());detail(id)
        })
        r.addView(btn("لغو"){detail(id)});setContentView(r)
    }

    private fun showNotes(id:Long){
        val s="موضوعات:\n${db.topics(id).joinToString("\n"){ "• $it" }.ifBlank{"—"}}\n\nتصمیمات:\n${db.decisions(id).joinToString("\n"){ "• $it" }.ifBlank{"—"}}"
        msg("موارد جلسه",s)
    }

    private fun tasks(){
        val r=root();r.addView(title("Action Items باز"))
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        db.openTasks().forEach{t->box.addView(btn("${t.title}\n${t.owner} | ${t.due} | ${t.priority}"){db.complete(t.id);tasks()})}
        r.addView(ScrollView(this).apply{addView(box)},LinearLayout.LayoutParams(-1,0,1f))
        r.addView(btn("بازگشت"){dashboard()});setContentView(r)
    }

    private fun searchDialog(){
        val e=EditText(this);e.hint="عنوان، IP، Ticket، موضوع، مسئول..."
        AlertDialog.Builder(this).setTitle("جست‌وجو").setView(e).setNegativeButton("لغو",null)
            .setPositiveButton("جست‌وجو"){_,_->showSearch(e.text.toString())}.show()
    }

    private fun showSearch(q:String){
        val hits=db.search(q)
        msg("نتیجه جست‌وجو",if(hits.isEmpty())"نتیجه‌ای پیدا نشد." else hits.joinToString("\n\n"))
    }

    private fun exportDocx(id:Long){
        val m=db.get(id)?:return
        val decisions=db.decisions(id)
        val tasks=db.tasks(id)
        val topics=db.topics(id)
        val file=File(cacheDir,"Hamidreza_Meeting_${System.currentTimeMillis()}.docx")
        val paragraphs=mutableListOf(
            "گزارش مدیریتی جلسه",
            "1. مشخصات جلسه",
            "عنوان: ${m.title}",
            "تاریخ و ساعت: ${m.date}",
            "برگزارکننده: ${m.host}",
            "حاضرین: ${m.people}",
            "حوزه: ${m.category}",
            "محرمانگی: ${m.confidentiality}",
            "2. خلاصه مدیریتی",
            m.agenda.ifBlank{"خلاصه مدیریتی ثبت نشده است."},
            "3. تصمیمات کلیدی"
        )
        paragraphs += if(decisions.isEmpty()) listOf("موردی ثبت نشده است.") else decisions.map{"• $it"}
        paragraphs += listOf("4. اقدامات موردنیاز")
        paragraphs += if(tasks.isEmpty()) listOf("موردی ثبت نشده است.") else tasks.map{"• ${it.title} | مسئول: ${it.owner} | Deadline: ${it.due} | اولویت: ${it.priority} | وضعیت: ${it.status}"}
        paragraphs += listOf("5. مشکلات و ریسک‌ها","در نسخه فعلی بخش ریسک مستقل تکمیل نشده است.","6. موارد نیازمند تصمیم مدیریت","موردی ثبت نشده است.","7. نتیجه‌گیری","پیگیری اقدامات باز مطابق Deadlineهای تعیین‌شده انجام شود.")
        DocxWriter.write(file,paragraphs)
        val uri=Uri.parse("content://ir.hamidreza.meeting.file/${file.name}")
        // در این نسخه، فایل داخل حافظه برنامه ساخته می‌شود؛ اشتراک‌گذاری کامل با FileProvider در مرحله Release اضافه می‌شود.
        msg("گزارش Word آماده شد","فایل DOCX ساخته شد:\n${file.name}\n\nمسیر داخلی:\n${file.absolutePath}")
    }

    override fun onDestroy() {
        liveTranscriptRunning = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        super.onDestroy()
    }

    private fun msg(t:String,s:String)=AlertDialog.Builder(this).setTitle(t).setMessage(s).setPositiveButton("بستن",null).show()

    private fun v8Dashboard() {
        val r=root()
        r.addView(title("داشبورد مدیریتی"))
        val open=db.openTasks().size
        val meetings=db.meetings().size
        r.addView(TextView(this).apply {
            text="جلسات ثبت‌شده: $meetings\nAction Item باز: $open\n\nوضعیت: برای بررسی دقیق‌تر، گزارش هر جلسه را از صفحه همان جلسه تولید کنید."
            textSize=16f
        })
        r.addView(btn("بازگشت"){dashboard()})
        setContentView(r)
    }

    private fun v8Reminder() {
        val e=EditText(this).apply{hint="عنوان Reminder"}
        AlertDialog.Builder(this).setTitle("Reminder").setView(e)
            .setNegativeButton("لغو",null)
            .setPositiveButton("ذخیره"){_,_->msg("Reminder","Reminder «${e.text}» ثبت شد. در نسخه Release به AlarmManager متصل می‌شود.")}
            .show()
    }

    private fun v8Security() {
        val r=root()
        r.addView(title("امنیت"))
        r.addView(TextView(this).apply{text="PIN و Biometric برای قفل برنامه در این نسخه به‌عنوان لایه امنیتی UI در نظر گرفته شده‌اند.";textSize=15f})
        r.addView(btn("Biometric"){ 
            val bm=BiometricManager.from(this)
            val ok=bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            if(ok==BiometricManager.BIOMETRIC_SUCCESS){
                val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
                    override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){msg("امنیت","احراز هویت موفق بود.")}
                })
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Hamidreza Meeting")
                    .setSubtitle("احراز هویت برای دسترسی به جلسات")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build())
            } else msg("Biometric","Biometric یا قفل صفحه دستگاه در دسترس نیست.")
        })
        r.addView(btn("بازگشت"){dashboard()})
        setContentView(r)
    }
}

object DocxWriter {
    fun write(file:File, lines:List<String>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val body=StringBuilder()
            body.append("""<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
            lines.forEach { line ->
                val esc=line.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                body.append("<w:p><w:pPr><w:bidi/></w:pPr><w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">$esc</w:t></w:r></w:p>")
            }
            body.append("<w:sectPr/></w:body></w:document>")
            zip.write(body.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}

data class Meeting(val id:Long,val title:String,val date:String,val host:String,val people:String,val place:String,val type:String,val category:String,val confidentiality:String,val agenda:String)
data class Task(val id:Long,val title:String,val owner:String,val due:String,val priority:String,val status:String)

class MeetingDb(c:Context):android.database.sqlite.SQLiteOpenHelper(c,"hamidreza_meeting.db",null,10){
    override fun onCreate(d:android.database.sqlite.SQLiteDatabase){
        d.execSQL("CREATE TABLE meetings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,date TEXT,host TEXT,people TEXT,place TEXT,type TEXT,category TEXT,confidentiality TEXT,agenda TEXT)")
        d.execSQL("CREATE TABLE topics(id INTEGER PRIMARY KEY AUTOINCREMENT,meeting_id INTEGER,text TEXT)")
        d.execSQL("CREATE TABLE decisions(id INTEGER PRIMARY KEY AUTOINCREMENT,meeting_id INTEGER,text TEXT)")
        d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,meeting_id INTEGER,title TEXT,owner TEXT,due TEXT,priority TEXT,status TEXT)")
        d.execSQL("CREATE TABLE recordings(id INTEGER PRIMARY KEY AUTOINCREMENT,meeting_id INTEGER,path TEXT,created_at INTEGER)")
        d.execSQL("CREATE TABLE transcripts(meeting_id INTEGER PRIMARY KEY,content TEXT)")
        d.execSQL("CREATE TABLE ai(meeting_id INTEGER PRIMARY KEY,content TEXT)")
        d.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY,v TEXT)")
    }
    override fun onUpgrade(d:android.database.sqlite.SQLiteDatabase,o:Int,n:Int){
        d.execSQL("CREATE TABLE IF NOT EXISTS recordings(id INTEGER PRIMARY KEY AUTOINCREMENT,meeting_id INTEGER,path TEXT,created_at INTEGER)")
        d.execSQL("CREATE TABLE IF NOT EXISTS transcripts(meeting_id INTEGER PRIMARY KEY,content TEXT)")
        d.execSQL("CREATE TABLE IF NOT EXISTS ai(meeting_id INTEGER PRIMARY KEY,content TEXT)")
        d.execSQL("CREATE TABLE IF NOT EXISTS settings(k TEXT PRIMARY KEY,v TEXT)")
    }
    fun addMeeting(t:String,dt:String,h:String,p:String,l:String,ty:String,cat:String,conf:String,a:String):Long{
        val v=android.content.ContentValues().apply{put("title",t);put("date",dt);put("host",h);put("people",p);put("place",l);put("type",ty);put("category",cat);put("confidentiality",conf);put("agenda",a)}
        return writableDatabase.insert("meetings",null,v)
    }
    private fun read(c:android.database.Cursor)=Meeting(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9))
    fun meetings()=readableDatabase.rawQuery("SELECT * FROM meetings ORDER BY id DESC",null).use{c->buildList{while(c.moveToNext())add(read(c))}}
    fun get(id:Long)=readableDatabase.rawQuery("SELECT * FROM meetings WHERE id=?",arrayOf(id.toString())).use{c->if(c.moveToFirst())read(c)else null}
    fun addTopic(id:Long,s:String)=writableDatabase.execSQL("INSERT INTO topics(meeting_id,text) VALUES(?,?)",arrayOf(id,s))
    fun addDecision(id:Long,s:String)=writableDatabase.execSQL("INSERT INTO decisions(meeting_id,text) VALUES(?,?)",arrayOf(id,s))
    fun topics(id:Long)=strings("SELECT text FROM topics WHERE meeting_id=?",id)
    fun decisions(id:Long)=strings("SELECT text FROM decisions WHERE meeting_id=?",id)
    private fun strings(q:String,id:Long)=readableDatabase.rawQuery(q,arrayOf(id.toString())).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    fun addTask(mid:Long,t:String,o:String,d:String,p:String)=writableDatabase.execSQL("INSERT INTO tasks(meeting_id,title,owner,due,priority,status) VALUES(?,?,?,?,?,?)",arrayOf(mid,t,o,d,p,"OPEN"))
    fun tasks(mid:Long)=taskQuery("SELECT id,title,owner,due,priority,status FROM tasks WHERE meeting_id=?",mid)
    fun openTasks()=readableDatabase.rawQuery("SELECT id,title,owner,due,priority,status FROM tasks WHERE status='OPEN' ORDER BY id DESC",null).use{buildTasks(it)}
    private fun taskQuery(q:String,id:Long)=readableDatabase.rawQuery(q,arrayOf(id.toString())).use{buildTasks(it)}
    private fun buildTasks(c:android.database.Cursor)=buildList{while(c.moveToNext())add(Task(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)))}
    fun complete(id:Long)=writableDatabase.execSQL("UPDATE tasks SET status='DONE' WHERE id=?",arrayOf(id))
    fun addRecording(mid:Long,path:String)=writableDatabase.execSQL("INSERT INTO recordings(meeting_id,path,created_at) VALUES(?,?,?)",arrayOf(mid,path,System.currentTimeMillis()))
    fun saveTranscript(id:Long,s:String){
        val enc=CryptoBox.encrypt(s)
        writableDatabase.execSQL("INSERT OR REPLACE INTO transcripts(meeting_id,content) VALUES(?,?)",arrayOf(id,enc))
    }
    fun transcript(id:Long)=readableDatabase.rawQuery("SELECT content FROM transcripts WHERE meeting_id=?",arrayOf(id.toString())).use{c->if(c.moveToFirst())CryptoBox.decrypt(c.getString(0))else""}
    fun search(q:String):List<String>{
        val like="%$q%"
        return readableDatabase.rawQuery("SELECT title,date,category FROM meetings WHERE title LIKE ? OR category LIKE ? OR agenda LIKE ?",arrayOf(like,like,like)).use{c->
            buildList{while(c.moveToNext())add("${c.getString(0)} | ${c.getString(1)} | ${c.getString(2)}")}
        }
    }
    fun saveAi(id:Long,s:String){writableDatabase.execSQL("INSERT OR REPLACE INTO ai(meeting_id,content) VALUES(?,?)",arrayOf(id,CryptoBox.encrypt(s)))}
    fun setSetting(k:String,v:String){writableDatabase.execSQL("INSERT OR REPLACE INTO settings(k,v) VALUES(?,?)",arrayOf(k,CryptoBox.encrypt(v)))}
    fun setting(k:String)=readableDatabase.rawQuery("SELECT v FROM settings WHERE k=?",arrayOf(k)).use{c->if(c.moveToFirst())CryptoBox.decrypt(c.getString(0))else""}

}
