package com.gilteojwo.sdktest

import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.car.Car
import android.os.Bundle
import android.widget.TextView
import com.gilteojwo.BuildInfo

/** 차량용 시험 화면 (빌드 확인용 임시 화면) */
class SdkTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val car: Car? = Car.createCar(this)
        val vehicle = Vehicle(this)
        val message = TextView(this)
        message.text = "길터줘 시험 화면 준비 중\n${BuildInfo.text()}\nCar 연결: ${car != null}, Vehicle: ${vehicle.javaClass.simpleName}"
        message.textSize = 24f
        setContentView(message)
        car?.disconnect()
    }
}
