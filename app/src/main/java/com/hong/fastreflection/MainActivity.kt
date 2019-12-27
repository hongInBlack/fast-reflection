package com.hong.fastreflection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hong.fastreflection.annotation.Greet

@Greet("MainActivity")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}