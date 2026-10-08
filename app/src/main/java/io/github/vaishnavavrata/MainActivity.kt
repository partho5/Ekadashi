package io.github.vaishnavavrata

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : Activity() {

    private companion object {
        const val REQUEST_NOTIF_PERMISSION = 101
        const val SWIPE_THRESHOLD = 80
        const val SWIPE_VELOCITY_THRESHOLD = 100
        const val NOTIF_ASK_DELAY_MS = 3000L
        const val STATE_BG_INDEX = "bgIndex"
    }

    private var selectedCountry: Country = Country.OTHER
    private var selectedLanguage: String = "en"

    private var isShowingSetup: Boolean = false

    private var allVratas: List<Vrata> = emptyList()
    private var currentIndex: Int = -1
    private var gestureDetector: GestureDetector? = null
    private var bgIndex: Int = -1

    override fun attachBaseContext(newBase: Context) {
        val lang = Config.getLanguage(newBase)
        super.attachBaseContext(L10n.attachBaseContext(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One random background per app launch; kept across recreate() (e.g. language change).
        bgIndex = savedInstanceState?.getInt(STATE_BG_INDEX, -1) ?: -1
        if (bgIndex < 0) {
            val arr = resources.obtainTypedArray(R.array.backgrounds)
            val count = arr.length()
            arr.recycle()
            bgIndex = if (count > 0) Random.nextInt(count) else 0
        }

        if (!Config.isSetupDone(this)) {
            showSetupView()
        } else {
            showHomeView()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_BG_INDEX, bgIndex)
    }

    override fun onRestart() {
        super.onRestart()
        // Returning from the background: jump back to the next immediate vrata.
        if (!isShowingSetup) {
            loadVrataDataAndRender(keepCurrentSelection = false)
        }
    }

    private fun applyBackground() {
        val bg = findViewById<ImageView>(R.id.bg_image) ?: return
        val arr = resources.obtainTypedArray(R.array.backgrounds)
        val resId = if (arr.length() > 0) arr.getResourceId(bgIndex.coerceIn(0, arr.length() - 1), 0) else 0
        arr.recycle()
        if (resId != 0) bg.setImageResource(resId)
    }

    override fun onResume() {
        super.onResume()

        if (isShowingSetup) {
            updateNotifRow()
        } else {
            checkNotificationPermissionBanner()

            // Background network fetch on resume
            Thread {
                val updated = Repo.refresh(this)
                if (updated) {
                    Scheduler.scheduleAll(this)
                    runOnUiThread {
                        if (!isShowingSetup) {
                            loadVrataDataAndRender(keepCurrentSelection = true)
                        }
                    }
                }
            }.start()
        }
    }

    // --- SETUP VIEW ---

    private fun showSetupView() {
        isShowingSetup = true
        setContentView(R.layout.setup)
        applyBackground()

        selectedCountry = Config.getCountry(this)
        selectedLanguage = Config.getLanguage(this)

        setupEventListeners()
        updateSetupUI()
    }

    private fun setupEventListeners() {
        findViewById<View>(R.id.card_bd)?.setOnClickListener {
            selectedCountry = Country.BD
            updateSetupUI()
        }

        findViewById<View>(R.id.card_in)?.setOnClickListener {
            selectedCountry = Country.IN
            updateSetupUI()
        }

        findViewById<View>(R.id.card_other)?.setOnClickListener {
            selectedCountry = Country.OTHER
            updateSetupUI()
        }

        findViewById<View>(R.id.pill_en)?.setOnClickListener {
            selectedLanguage = "en"
            updateSetupUI()
        }

        findViewById<View>(R.id.pill_bn)?.setOnClickListener {
            selectedLanguage = "bn"
            updateSetupUI()
        }

        findViewById<View>(R.id.pill_hi)?.setOnClickListener {
            selectedLanguage = "hi"
            updateSetupUI()
        }

        findViewById<Button>(R.id.btn_continue)?.setOnClickListener {
            onContinueClicked()
        }

        findViewById<View>(R.id.row_notif)?.setOnClickListener {
            requestNotificationPermissionOrOpenSettings()
        }
    }

    private fun updateSetupUI() {
        val cardBd = findViewById<LinearLayout>(R.id.card_bd)
        val cardIn = findViewById<LinearLayout>(R.id.card_in)
        val cardOther = findViewById<LinearLayout>(R.id.card_other)
        val tvTzAuto = findViewById<TextView>(R.id.tv_tz_auto)

        cardBd?.setBackgroundResource(if (selectedCountry == Country.BD) R.drawable.card_selected else R.drawable.card_bg)
        cardIn?.setBackgroundResource(if (selectedCountry == Country.IN) R.drawable.card_selected else R.drawable.card_bg)
        cardOther?.setBackgroundResource(if (selectedCountry == Country.OTHER) R.drawable.card_selected else R.drawable.card_bg)

        if (selectedCountry == Country.OTHER) {
            val sysZone = try { ZoneId.systemDefault().id } catch (e: Exception) { "UTC" }
            tvTzAuto?.text = getString(R.string.tz_auto_fmt, sysZone)
            tvTzAuto?.visibility = View.VISIBLE
        } else {
            tvTzAuto?.visibility = View.GONE
        }

        val pillEn = findViewById<TextView>(R.id.pill_en)
        val pillBn = findViewById<TextView>(R.id.pill_bn)
        val pillHi = findViewById<TextView>(R.id.pill_hi)

        fun stylePill(pill: TextView?, isSelected: Boolean) {
            if (isSelected) {
                pill?.setBackgroundResource(R.drawable.pill_selected)
                pill?.setTextColor(getColor(R.color.pill_selected_text))
            } else {
                pill?.setBackgroundResource(R.drawable.pill_bg)
                pill?.setTextColor(getColor(R.color.pill_text))
            }
        }

        stylePill(pillEn, selectedLanguage == "en")
        stylePill(pillBn, selectedLanguage == "bn")
        stylePill(pillHi, selectedLanguage == "hi")

        val previewContext = L10n.attachBaseContext(this, selectedLanguage)
        findViewById<TextView>(R.id.setup_title)?.text = previewContext.getString(R.string.app_name)
        findViewById<TextView>(R.id.setup_subtitle)?.text = previewContext.getString(R.string.app_subtitle)
        findViewById<TextView>(R.id.setup_country_title)?.text = previewContext.getString(R.string.country_title)
        findViewById<TextView>(R.id.tv_bd)?.text = previewContext.getString(R.string.country_bd)
        findViewById<TextView>(R.id.tv_in)?.text = previewContext.getString(R.string.country_in)
        findViewById<TextView>(R.id.tv_other)?.text = previewContext.getString(R.string.country_other)
        findViewById<TextView>(R.id.setup_lang_title)?.text = previewContext.getString(R.string.language_title)
        findViewById<TextView>(R.id.pill_en)?.text = previewContext.getString(R.string.lang_en)
        findViewById<TextView>(R.id.pill_bn)?.text = previewContext.getString(R.string.lang_bn)
        findViewById<TextView>(R.id.pill_hi)?.text = previewContext.getString(R.string.lang_hi)
        findViewById<TextView>(R.id.tv_row_notif)?.text = previewContext.getString(R.string.notif_row_text)
        findViewById<TextView>(R.id.tv_footer_note)?.text = previewContext.getString(R.string.footer_note)
        updateNotifRow()
        findViewById<Button>(R.id.btn_continue)?.text = if (Config.isSetupDone(this)) {
            previewContext.getString(R.string.btn_save)
        } else {
            previewContext.getString(R.string.btn_continue)
        }
    }

    private fun onContinueClicked() {
        val oldLang = Config.getLanguage(this)
        Config.saveConfig(this, selectedCountry, selectedLanguage)

        Scheduler.scheduleAll(this)
        Scheduler.scheduleRefreshJob(this)

        if (oldLang != selectedLanguage) {
            recreate()
        } else {
            showHomeView()
        }
    }

    // --- HOME VIEW ---

    private fun showHomeView() {
        isShowingSetup = false
        setContentView(R.layout.home)
        applyBackground()

        findViewById<ImageView>(R.id.btn_settings)?.setOnClickListener {
            showSetupView()
        }

        findViewById<View>(R.id.banner_notif)?.setOnClickListener {
            requestNotificationPermissionOrOpenSettings()
        }

        findViewById<View>(R.id.btn_prev)?.setOnClickListener {
            showPrevVrata()
        }

        findViewById<View>(R.id.btn_next)?.setOnClickListener {
            showNextVrata()
        }

        scheduleFirstNotificationAsk()
        setupSwipeDetector()
        checkNotificationPermissionBanner()
        loadVrataDataAndRender(keepCurrentSelection = false)
    }

    private fun setupSwipeDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            showPrevVrata()
                        } else {
                            showNextVrata()
                        }
                        return true
                    }
                }
                return false
            }
        })

        val touchListener = View.OnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event) ?: false
        }

        findViewById<View>(R.id.home_root)?.setOnTouchListener(touchListener)
        findViewById<View>(R.id.hero_card)?.setOnTouchListener(touchListener)
    }

    private fun showPrevVrata() {
        if (currentIndex > 0) {
            currentIndex--
            renderCurrentVrata()
        }
    }

    private fun showNextVrata() {
        if (currentIndex < allVratas.size - 1) {
            currentIndex++
            renderCurrentVrata()
        }
    }

    private fun isNotifPermissionMissing(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** Home banner: shown only after the permission has been asked once and is still not granted. */
    private fun checkNotificationPermissionBanner() {
        val banner = findViewById<View>(R.id.banner_notif) ?: return
        banner.visibility =
            if (isNotifPermissionMissing() && Config.isNotifAsked(this)) View.VISIBLE else View.GONE
    }

    /** Settings row: shown only when notifications are denied. */
    private fun updateNotifRow() {
        val row = findViewById<View>(R.id.row_notif) ?: return
        row.visibility =
            if (isNotifPermissionMissing() && Config.isSetupDone(this)) View.VISIBLE else View.GONE
    }

    /** First launch: ask for the notification permission 3 seconds after the home screen appears. */
    private fun scheduleFirstNotificationAsk() {
        if (!isNotifPermissionMissing() || Config.isNotifAsked(this)) return
        findViewById<View>(R.id.home_root)?.postDelayed({
            if (!isFinishing && !isShowingSetup && isNotifPermissionMissing() && !Config.isNotifAsked(this)) {
                Config.setNotifAsked(this)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIF_PERMISSION)
            }
        }, NOTIF_ASK_DELAY_MS)
    }

    /** Prompts for the permission; once Android stops showing the dialog, opens the app's notification settings. */
    private fun requestNotificationPermissionOrOpenSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val firstTime = !Config.isNotifAsked(this)
        Config.setNotifAsked(this)
        if (firstTime || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIF_PERMISSION)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun loadVrataDataAndRender(keepCurrentSelection: Boolean) {
        val targetZone = Config.getTimezone(this)
        val today = LocalDate.now(targetZone)

        val vratas = Repo.load(this)
        allVratas = vratas.sortedBy { it.date }

        if (allVratas.isEmpty()) {
            currentIndex = -1
            renderCurrentVrata()
            return
        }

        if (!keepCurrentSelection || currentIndex < 0 || currentIndex >= allVratas.size) {
            // Find nearest upcoming/today vrata
            val firstUpcomingIdx = allVratas.indexOfFirst { it.date >= today }
            currentIndex = if (firstUpcomingIdx != -1) {
                firstUpcomingIdx
            } else {
                allVratas.size - 1
            }
        }

        renderCurrentVrata()
    }

    private fun renderCurrentVrata() {
        val heroCard = findViewById<View>(R.id.hero_card)
        val btnPrev = findViewById<View>(R.id.btn_prev)
        val btnNext = findViewById<View>(R.id.btn_next)

        if (allVratas.isEmpty() || currentIndex < 0 || currentIndex >= allVratas.size) {
            heroCard?.visibility = View.GONE
            btnPrev?.isEnabled = false
            btnNext?.isEnabled = false
            return
        }

        heroCard?.visibility = View.VISIBLE

        val targetZone = Config.getTimezone(this)
        val today = LocalDate.now(targetZone)
        val locale = L10n.getLocale(Config.getLanguage(this))
        val vrata = allVratas[currentIndex]

        val heroBadge = findViewById<TextView>(R.id.hero_badge)
        val tvCardCounter = findViewById<TextView>(R.id.tv_card_counter)
        val heroTag = findViewById<TextView>(R.id.hero_relative_tag)
        val heroName = findViewById<TextView>(R.id.hero_vrata_name)
        val heroDate = findViewById<TextView>(R.id.hero_vrata_date)
        val heroParanaText = findViewById<TextView>(R.id.hero_parana_text)
        val heroParanaContainer = findViewById<View>(R.id.hero_parana_container)
        val heroParanaWrapper = findViewById<View>(R.id.hero_parana_wrapper)
        val heroNote = findViewById<TextView>(R.id.hero_note)

        val daysDiff = ChronoUnit.DAYS.between(today, vrata.date)

        heroBadge?.text = when {
            daysDiff > 0L -> getString(R.string.hero_label_next)
            daysDiff == 0L -> getString(R.string.hero_label_today)
            else -> getString(R.string.hero_label_past)
        }

        tvCardCounter?.text = getString(R.string.card_counter_fmt, currentIndex + 1, allVratas.size)

        heroTag?.text = when {
            daysDiff > 1L -> getString(R.string.hero_in_days, daysDiff.toInt())
            daysDiff == 1L -> getString(R.string.hero_tomorrow)
            daysDiff == 0L -> getString(R.string.hero_today)
            daysDiff == -1L -> getString(R.string.hero_yesterday)
            else -> getString(R.string.hero_days_ago, abs(daysDiff.toInt()))
        }

        heroName?.text = L10n.getVrataName(this, vrata.type)
        heroDate?.text = L10n.formatDate(vrata.date, locale, "EEEE, d MMMM yyyy")
        val paranaText = L10n.formatParanaWindow(this, vrata.parana, targetZone, locale)
        heroParanaText?.text = paranaText
        heroParanaWrapper?.visibility = if (paranaText == null) View.GONE else View.VISIBLE
        heroNote?.text = vrata.note
        heroNote?.visibility = if (vrata.note.isNullOrBlank()) View.GONE else View.VISIBLE

        val hasPrev = currentIndex > 0
        val hasNext = currentIndex < allVratas.size - 1

        btnPrev?.isEnabled = hasPrev
        btnPrev?.alpha = if (hasPrev) 1.0f else 0.4f

        btnNext?.isEnabled = hasNext
        btnNext?.alpha = if (hasNext) 1.0f else 0.4f
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIF_PERMISSION) {
            checkNotificationPermissionBanner()
            updateNotifRow()
        }
    }
}
