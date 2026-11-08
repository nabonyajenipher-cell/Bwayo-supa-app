package com.bwayo.superapp

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val blue = Color.rgb(13, 71, 161)
    private val posts = mutableListOf("Welcome to Bwayo Super App! Share your moments here.")
    private lateinit var feed: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcome()
    }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(245, 248, 255))
        setPadding(24, 28, 24, 24)
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 26f
        setTextColor(blue)
        gravity = Gravity.CENTER
        setPadding(0, 16, 0, 20)
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { action() }
    }

    private fun showWelcome() {
        val layout = base()
        layout.gravity = Gravity.CENTER
        layout.addView(heading("BWAYO SUPER APP"))
        layout.addView(TextView(this).apply {
            text = "Connect • Share • Enjoy"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        })
        layout.addView(button("Get started") { showRegister() })
        setContentView(layout)
    }

    private fun showRegister() {
        val layout = base()
        layout.addView(heading("Create your account"))
        val name = EditText(this).apply { hint = "Your name" }
        val email = EditText(this).apply { hint = "Email address" }
        layout.addView(name)
        layout.addView(email)
        layout.addView(button("Continue") {
            if (name.text.isBlank() || email.text.isBlank()) {
                Toast.makeText(this, "Enter your name and email", Toast.LENGTH_SHORT).show()
            } else showHome(name.text.toString())
        })
        layout.addView(button("Back") { showWelcome() })
        setContentView(layout)
    }

    private fun showHome(user: String) {
        val layout = base()
        layout.addView(heading("Hello, $user"))
        layout.addView(TextView(this).apply {
            text = "Your community feed"
            textSize = 18f
            setTextColor(Color.DKGRAY)
        })
        val input = EditText(this).apply { hint = "Write a post..." }
        layout.addView(input)
        layout.addView(button("Publish post") {
            if (input.text.isNotBlank()) {
                posts.add(0, "$user: ${input.text}")
                input.text.clear()
                refreshPosts()
            }
        })
        layout.addView(button("Admin area") {
            Toast.makeText(this, "Admin tools need secure account setup and backend connection.", Toast.LENGTH_LONG).show()
        })
        feed = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(ScrollView(this).apply { addView(feed) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
        refreshPosts()
    }

    private fun refreshPosts() {
        if (!::feed.isInitialized) return
        feed.removeAllViews()
        posts.forEach { post ->
            feed.addView(TextView(this).apply {
                text = post
                textSize = 17f
                setTextColor(Color.DKGRAY)
                setBackgroundColor(Color.WHITE)
                setPadding(16, 18, 16, 18)
            })
            feed.addView(Space(this).apply { minimumHeight = 10 })
        }
    }
}
