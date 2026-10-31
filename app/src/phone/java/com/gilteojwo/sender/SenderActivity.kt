package com.gilteojwo.sender

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import com.gilteojwo.BuildInfo

/** 폰용 송신 앱의 첫 화면. 지금은 "준비 중" 안내만 보여준다. */
class SenderActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = TextView(this)
        message.text = "길터줘 송신 앱\n준비 중입니다\n\n${BuildInfo.text()}"
        message.textSize = 24f
        message.gravity = Gravity.CENTER
        setContentView(message)
    }
}
