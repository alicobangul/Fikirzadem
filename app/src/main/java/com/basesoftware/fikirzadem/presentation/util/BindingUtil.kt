package com.basesoftware.fikirzadem.presentation.util

import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.toSpannable
import androidx.databinding.BindingAdapter
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.databinding.BottomDialogPostMoreBinding
import com.basesoftware.fikirzadem.util.ExtensionUtil.downloadFromDrawable
import com.basesoftware.fikirzadem.util.ExtensionUtil.downloadFromUrl
import com.basesoftware.fikirzadem.util.ExtensionUtil.getDrawableWithCode
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.basesoftware.fikirzadem.util.WorkUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BindingUtil {

    @BindingAdapter("android:postCategory")
    @JvmStatic
    fun postCategory(view: TextView, categoryId : Int?) {

        categoryId?.let {

            // Post kategorisi hatalı ise "-" değil ise kategoriyi yazdır

            val categoryTextList = view.context.resources.getStringArray(R.array.arrayCategoryText)

            view.text = when((categoryTextList.size - 1) < it) {
                true -> "-"
                else -> categoryTextList[it]
            }

            // Post kategorisi hatalı ise kırmızı, değil ise kategori rengini göster

            val categoryTextColor: IntArray = view.context.resources.getIntArray(R.array.arrayCategoryColor)

            view.setTextColor(
                when((categoryTextColor.size - 1) < it) {
                    true -> Color.parseColor("#FF0000")
                    else -> categoryTextColor[it]
                }
            )

        }

    }

    @BindingAdapter("android:postText")
    @JvmStatic
    fun postText(view: TextView, postContent : String?) {

        postContent?.let { content ->

            /**
             * Eğer içerik 200 karakter veya az ise kendisi gösteriliyor
             * Eğer 200 karakter üzeri ise;
             * İçeriğin ilk 200 karakteri alınıyor ... ekleniyor (devamı niteliğinde)
             * İki alt satıra geçip "Devam et >>" ekleniyor.
             */
            val outputText = when(content.length <= 200) {
                true -> content
                else -> content.substring(0,200) + "...\n\n" + view.context.getString(R.string.read_more)
            }

            view.movementMethod = LinkMovementMethod.getInstance() // Tıklanabilir içerik oluşturuldu
            view.setText(outputText, TextView.BufferType.SPANNABLE) // Textview'a spannable bildirildi

            view.setSpannableFactory(object : Spannable.Factory() {
                override fun newSpannable(source: CharSequence?): Spannable {

                    val span = outputText.toSpannable()

                    if(content.length > 200) {

                        // Tıklanabilir kısmın rengi değiştiriliyor ve altı çizili durumu değiştiriliyor
                        span.setSpan(object : ClickableSpan() {
                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.color = Color.parseColor("#8899A6")
                                ds.isUnderlineText = false
                            }

                            override fun onClick(widget: View) {
                                // Yazıya tıklandığında ekranın altından içeriğin devamının görüntüleneceği bottomSheet açılıyor
                                BottomSheetDialog(view.context, R.style.BottomSheetDialogTheme).apply {
                                    val binding = BottomDialogPostMoreBinding
                                        .inflate(LayoutInflater.from(view.context.applicationContext), null, false)
                                    binding.content = content
                                    setContentView(binding.root)
                                    show()
                                }

                            } },
                            205,
                            (205 + view.context.getString(R.string.read_more).length - 1),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        /**
                         * >> işaretinden sonra &#160; ile xml'de görünmez boşluk yaratıldı
                         * Spannable görünmez boşluk öncesindeki > işaretine kadar yapıldı (length - 1)
                         * Aksi durumda text'in son satırının tamamı tıklanabilir oluyor. Bu durum engellendi.
                         */
                    }

                    return span
                }

            }) // Spannable Faktörü eklendi


        }

    }

    @BindingAdapter("android:postRating")
    @JvmStatic
    fun postRating(view: RatingBar, rating : String?) {

        rating?.let { view.rating = it.toFloat() }

    }

    @BindingAdapter("android:postCommentIcoCalculate")
    @JvmStatic
    fun postCommentIcoCalculate(view: ImageView, rating : String?) {

        /**
         * if(it.toFloat() % 1f == 0f) it.toFloat().toInt() else (it.toFloat() + 0.5f).toInt()
         * Eğer yorum 3.5 2.5 vb ise bir 0.5f ekleyip tam yıldıza oranla
         */

        rating?.let {

            (view.parent as ConstraintLayout).setConstraintSet(
                ConstraintSet().apply {
                    clone(view.parent as ConstraintLayout)
                    setHorizontalBias(view.id,
                        when(if(it.toFloat() % 1f == 0f) it.toFloat().toInt() else (it.toFloat() + 0.5f).toInt()) {
                            0 -> 0.100f // 1 yıldız
                            1 -> 0.100f // 1 yıldız
                            2 -> 0.320f // 2 yıldız
                            3 -> 0.540f // 3 yıldız
                            4 -> 0.755f // 4 yıldız
                            else -> 0.980f // 5 yıldız
                        }
                    )
                }
            )

        }

    }

    @BindingAdapter("android:calculateDate")
    @JvmStatic
    fun calculateDate(view: TextView, time: Long?) {

        time?.let {

            CoroutineScope(Dispatchers.Main).launch {

                view.text = WorkUtil.calculateDate(view.context, it, "Binding")

            }

        }

    }

    @BindingAdapter("android:datetime")
    @JvmStatic
    fun datetime(view: TextView, time: Timestamp?) {

        val dateFormat = "dd / MM / yyyy  -  HH : mm : ss"

        view.text = when(time == null) {
            true -> "- / - / -    - : - : -"
            else -> time.let { SimpleDateFormat(dateFormat, Locale.forLanguageTag("tr")).format(Date(it.seconds*1000)) }
        }

    }

    @BindingAdapter("android:marqueeSettings")
    @JvmStatic
    fun marqueeSettings(view: TextView, text : String?) {

        text?.let {

            view.setTextIsSelectable(it.length >= 10)
            view.setHorizontallyScrolling(it.length >= 10)
            view.ellipsize = TextUtils.TruncateAt.MARQUEE
            view.marqueeRepeatLimit = -1

        }

    }

    @BindingAdapter("android:ratingSettings")
    @JvmStatic
    fun ratingSettings(view: RatingBar, value : Boolean) {

        if(value) view.setOnRatingBarChangeListener { _, rating, _ -> if(rating == 0f) view.rating = 1f }

    }

    @BindingAdapter("android:drawableWithCode")
    @JvmStatic
    fun drawableWithCode(view: ImageView, drawableCode: Int?) {

        drawableCode?.let { view.getDrawableWithCode(it) }

    }

    @BindingAdapter("android:download")
    @JvmStatic
    fun download(view: ImageView, url: String?) {

        view.downloadFromUrl(url ?: "default")

    }

    @BindingAdapter("android:downloadPpWithUserId")
    @JvmStatic
    fun downloadPpWithUserId(view: ImageView, userId: String?) {

        userId?.let {

            CoroutineScope(Dispatchers.IO).launch {
                WorkUtil
                    .firestore()
                    .collection("Users")
                    .document(it)
                    .get(Source.CACHE)
                    .addOnCompleteListener { task ->
                        CoroutineScope(Dispatchers.Main).launch {
                            when(task.isSuccessful) {
                                true -> view.downloadFromUrl(task.result.toUserModel().userProfilePicture)
                                else -> view.downloadFromDrawable(AppCompatResources.getDrawable(view.context, R.drawable.login_user_ico)!!)
                            }
                        }
                    }
            }

        }

    }

    @BindingAdapter("android:emailVerify")
    @JvmStatic
    fun emailVerify(view: ImageView, value: Boolean?) {

        value?.let {

            view.setImageResource(if(it) R.drawable.okey else R.drawable.fail)

        }

    }

    @BindingAdapter("android:socialSettings")
    @JvmStatic
    fun socialSettings(view: View, socialLink: String?) {

        view.visibility = when(socialLink == null) {
            true -> View.GONE
            else -> when(socialLink.equals("-",true)) {
                true -> View.GONE
                else -> View.VISIBLE
            }
        }

    }

    @BindingAdapter("android:spaceFilter")
    @JvmStatic
    fun spaceFilter(view: EditText, filter: Boolean) {

        // Edittext görünümlerinde ilk karakter boşluk yazma engellendi

        view.setRawInputType(InputType.TYPE_CLASS_TEXT) // Giriş yazı tipi belirtildi

        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty() && filter) {
                    if(s.toString().length == 1 && s.toString().matches(Regex(" "))){ s?.clear() }
                }
            }

        })

    }

    @BindingAdapter("android:textMenuFilter")
    @JvmStatic
    fun textMenuFilter(view: EditText, value : Boolean) {

        // Edittext görünümlerinde kopyala yapıştır menüsü kapatıldı

        view.setRawInputType(InputType.TYPE_CLASS_TEXT) // Giriş yazı tipi belirtildi

        view.customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean { return !value }
            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean { return !value }
            override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean { return !value }
            override fun onDestroyActionMode(p0: ActionMode?) {}

        }

    }

    @BindingAdapter("android:menuFilter")
    @JvmStatic
    fun menuFilter(view: TextView, value : Boolean) {

        // Edittext görünümlerinde kopyala yapıştır menüsü kapatıldı

        view.setRawInputType(InputType.TYPE_CLASS_TEXT) // Giriş yazı tipi belirtildi

        view.customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean { return !value }
            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean { return !value }
            override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean { return !value }
            override fun onDestroyActionMode(p0: ActionMode?) {}

        }

    }

    @BindingAdapter(value = ["android:animation" , "android:animationStart"], requireAll = false)
    @JvmStatic
    fun splashScreenAnimation(view: View, animation : Animation, animationStart : Boolean?) {

        animationStart?.let { if (it) view.startAnimation(animation) }

    }

}