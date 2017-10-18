package xyz.klinker.messenger.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import android.widget.Toast

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.billing.ProductAvailable
import xyz.klinker.messenger.shared.util.billing.ProductType

class AccountPurchaseActivity : AppCompatActivity() {

    private var isInitial = true
    private var revealedPurchaseOptions = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsHelper.accountTutorialStarted(this)

        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_account_purchase)
        setUpInitialLayout()

        if (Settings.mainColorSet.color == Color.WHITE) {
            findViewById<View>(R.id.initial_layout).setBackgroundColor(ColorSet.TEAL(this).color)
            findViewById<View>(R.id.purchase_layout).setBackgroundColor(ColorSet.TEAL(this).color)
            (findViewById<View>(R.id.try_it) as TextView)
                    .setTextColor(ColorStateList.valueOf(ColorSet.TEAL(this).color))
        } else {
            findViewById<View>(R.id.initial_layout).setBackgroundColor(Settings.mainColorSet.color)
            findViewById<View>(R.id.purchase_layout).setBackgroundColor(Settings.mainColorSet.color)
            (findViewById<View>(R.id.try_it) as TextView)
                    .setTextColor(ColorStateList.valueOf(Settings.mainColorSet.color))
        }

        Handler().postDelayed({ this.circularRevealIn() }, 100)
    }

    private fun setUpInitialLayout() {
        findViewById<View>(R.id.try_it).setOnClickListener { tryIt() }

        val startTime: Long = 500
        quickViewReveal(findViewById(R.id.icon_watch), startTime)
        quickViewReveal(findViewById(R.id.icon_tablet), startTime + 75)
        quickViewReveal(findViewById(R.id.icon_computer), startTime + 150)
        quickViewReveal(findViewById(R.id.icon_phone), startTime + 225)
        quickViewReveal(findViewById(R.id.icon_notify), startTime + 300)
    }

    private fun tryIt() {
        slidePurchaseOptionsIn()
        AnalyticsHelper.accountTutorialFinished(this)

        // set up purchasing views here
        val monthly = findViewById<View>(R.id.monthly)
        val threeMonth = findViewById<View>(R.id.three_month)
        val yearly = findViewById<View>(R.id.yearly)
        val lifetime = findViewById<View>(R.id.lifetime)

        if (!revealedPurchaseOptions) {
            val startTime: Long = 300
            quickViewReveal(yearly, startTime)
            quickViewReveal(threeMonth, startTime + 75)
            quickViewReveal(monthly, startTime + 150)
            quickViewReveal(lifetime, startTime + 225)

            revealedPurchaseOptions = true
        }

        monthly.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createMonthly()) }
        threeMonth.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createThreeMonth()) }
        yearly.setOnClickListener { warnOfPlayStoreSubscriptionProcess(ProductAvailable.createYearly()) }
        lifetime.setOnClickListener { finishWithPurchaseResult(ProductAvailable.createLifetime()) }
    }

    private fun finishWithPurchaseResult(product: ProductAvailable) {
        val result = Intent()
        result.putExtra(PRODUCT_ID_EXTRA, product.productId)
        setResult(Activity.RESULT_OK, result)

        if (product.type == ProductType.SUBSCRIPTION) {
            Toast.makeText(this, R.string.subscription_toast, Toast.LENGTH_LONG).show()
        }

        AnalyticsHelper.accountSelectedPurchase(this)
        finish()
    }

    private fun warnOfPlayStoreSubscriptionProcess(product: ProductAvailable) {
        AlertDialog.Builder(this, R.style.SubscriptionPicker)
                .setMessage(R.string.play_store_subscription_warning)
                .setPositiveButton(R.string.ok) { _, _ -> finishWithPurchaseResult(product) }
                .show()
    }

    private fun circularRevealIn() {
        val view = findViewById<View>(R.id.initial_layout)
        view.visibility = View.VISIBLE

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val cx = view.width / 2
                val cy = view.height / 2
                val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius).start()
            } else {
                view.alpha = 0f
                view.animate().alpha(1f).start()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    private fun circularRevealOut() {
        val view = findVisibleHolder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx = view.width / 2
            val cy = view.height / 2
            val initialRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0f)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view.visibility = View.INVISIBLE
                    close()
                }
            })

            anim.start()
        } else {
            view.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    close()
                }
            }).start()
        }
    }

    private fun quickViewReveal(view: View, delay: Long) {
        view.translationX = (-1 * DensityUtil.toDp(this, 16)).toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
                .translationX(0f)
                .alpha(1f)
                .setStartDelay(delay)
                .start()
    }

    private fun slidePurchaseOptionsIn() {
        slideIn(findViewById(R.id.purchase_layout))
    }

    private fun slideIn(view: View) {
        isInitial = false
        val initial = findViewById<View>(R.id.initial_layout)

        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationX = view.width.toFloat()
        view.animate()
                .alpha(1f)
                .translationX(0f)
                .setListener(null)
                .start()

        initial.animate()
                .alpha(0f)
                .translationX((-1 * initial.width).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        initial.visibility = View.INVISIBLE
                        initial.translationX = 0f
                    }
                }).start()
    }

    private fun slideOut() {
        isInitial = true
        val visible = findVisibleHolder()
        val initial = findViewById<View>(R.id.initial_layout)

        visible.animate()
                .alpha(0f)
                .translationX(visible.width.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visible.visibility = View.INVISIBLE
                        visible.translationX = 0f
                    }
                }).start()

        initial.visibility = View.VISIBLE
        initial.alpha = 0f
        initial.translationX = (-1 * initial.width).toFloat()
        initial.animate()
                .alpha(1f)
                .translationX(0f)
                .setListener(null)
                .start()
    }

    private fun findVisibleHolder(): View {
        val initial = findViewById<View>(R.id.initial_layout)
        val purchase = findViewById<View>(R.id.purchase_layout)

        return if (initial.visibility != View.INVISIBLE) {
            initial
        } else {
            purchase
        }
    }

    override fun onBackPressed() {
        if (isInitial) {
            circularRevealOut()
        } else {
            slideOut()
        }
    }

    private fun close() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        val PRODUCT_ID_EXTRA = "product_id"
    }
}