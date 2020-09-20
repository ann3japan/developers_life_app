package ann.tinkofftask

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

// Data class for Meme entity
data class Meme(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String,
    @SerializedName("gifURL") val gifUrl: String?,
    @SerializedName("previewURL") val previewUrl: String
)

// Main activity
class MainActivity : AppCompatActivity() {
    val url = "https://developerslife.ru/random?json=true"
    val TAG = "MainActivity"
    val gson = GsonBuilder().create()

    // Business logic state
    // currentMeme points to the current meme in the history if the history is not empty
    // If the history is empty, the currentMeme equals -1
    var currentMeme = -1
    val memes = mutableListOf<Meme>()

    lateinit var memeImage: ImageView
    lateinit var backButton: FloatingActionButton
    lateinit var nextButton: FloatingActionButton
    lateinit var memeDescriptionText: TextView

    lateinit var retryButton: Button

    lateinit var memeContainer: LinearLayout
    lateinit var memeFailedContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val queue = Volley.newRequestQueue(this)

        setContentView(R.layout.layout_activity_main)

        memeImage = findViewById(R.id.meme_image)

        backButton = findViewById(R.id.button_back)
        nextButton = findViewById(R.id.button_next)
        memeDescriptionText = findViewById(R.id.meme_description_text)
        retryButton = findViewById(R.id.retry_button)

        memeContainer = findViewById(R.id.meme_container)
        memeFailedContainer = findViewById(R.id.meme_failed_container)

        backButton.setOnClickListener { v: View ->
            currentMeme--
            showMeme(v.context, memes[currentMeme])
            checkBackButtonState()
        }
        nextButton.setOnClickListener { v: View ->
            nextMeme(v.context, queue)
        }

        retryButton.setOnClickListener { v: View ->
            // if the currentMeme == -1 it means that we failed to retrieve the first JSON
            // otherwise it means that we just failed to load the GIF
            if (currentMeme == -1) {
                nextMeme(v.context, queue)
            } else {
                showMeme(v.context, memes[currentMeme])
            }
        }
        nextMeme(this, queue)
    }

    private fun nextMeme(c: Context, queue: RequestQueue) {
        showMemeState()
        if (currentMeme >= 0 && currentMeme < memes.size - 1) {
            currentMeme++
            showMeme(c, memes[currentMeme])
            checkBackButtonState()
            return
        }
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val meme = gson.fromJson<Meme>(response, Meme::class.java)

                currentMeme++
                memes.add(meme)
                checkBackButtonState()

                showMeme(c, meme)
            },
            Response.ErrorListener {
                Toast.makeText(c, "Не удалось загрузить следующий мем.", Toast.LENGTH_SHORT).show()
                if (currentMeme == -1) {
                    showErrorState()
                }
            }
        )
        queue.add(stringRequest)
    }

    private fun showMeme(c: Context, meme: Meme) {
        val requestManager = Glide.with(this)
        val borderRadius = c.resources.getDimensionPixelSize(R.dimen.meme_border_radius)

        // kotlin typing system prevents from code reusal :(
        if (meme.gifUrl != null) {
            requestManager
                .asGif()
                .load(meme.gifUrl)
                .transform(RoundedCorners(borderRadius))
                .listener(object : RequestListener<GifDrawable> {
                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        showMemeState()
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        showErrorState()
                        return false
                    }
                }
                )
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(memeImage)

        } else {
            requestManager
                .load(meme.previewUrl)
                .transform(RoundedCorners(borderRadius))
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        showMemeState()
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        showErrorState()
                        return false
                    }
                }
                )
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(memeImage)
        }

        memeDescriptionText.text = meme.description
    }

    private fun showMemeState() {
        memeContainer.visibility = VISIBLE
        memeFailedContainer.visibility = GONE
    }

    private fun showErrorState() {
        memeContainer.visibility = GONE
        memeFailedContainer.visibility = VISIBLE
    }

    private fun checkBackButtonState() {
        val isEnabled = currentMeme > 0
        backButton.isEnabled = isEnabled
        if (isEnabled) {
            backButton.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.meme_nav_button_enabled, theme))
        } else {
            backButton.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.meme_nav_button_disabled, theme))
        }
    }
}


