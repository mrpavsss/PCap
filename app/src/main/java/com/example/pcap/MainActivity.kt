package com.example.pcap

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.pcap.fragments.ComposeFragment
import com.example.pcap.fragments.FeedFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.parse.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.example.pcap.fragments.ProfileFragment


class MainActivity : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        supportActionBar?.setCustomView(R.layout.actionbar_title);

        val fragmentManager: FragmentManager = supportFragmentManager

        bottomNav = findViewById(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener {
                item ->

            var fragmentToShow: Fragment? = null
            when (item.itemId) {

                R.id.action_home -> {
                    fragmentToShow = FeedFragment()
                    item.icon = getDrawable(R.drawable.instagram_home_filled_24)
                }
                R.id.action_compose -> {
                    fragmentToShow = ComposeFragment()
                    item.icon = getDrawable(R.drawable.instagram_new_post_filled_24)
                }
                R.id.action_profile -> {
                    fragmentToShow = ProfileFragment()
                    item.icon = getDrawable(R.drawable.instagram_user_filled_24)
                }
            }

            if (fragmentToShow != null) {
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragmentToShow).commit()
            }
            true
        }

        bottomNav.selectedItemId = R.id.action_home

    }

    companion object {
        const val TAG = "MainActivity"
    }
}