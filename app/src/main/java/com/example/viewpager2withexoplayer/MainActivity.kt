package com.example.viewpager2withexoplayer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.viewpager2.widget.ViewPager2
import com.example.viewpager2withexoplayer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: VideoAdapter
    private val videos = ArrayList<Video>()
    private val exoPlayerItems = ArrayList<ExoPlayerItem>()

    private var isAudioMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = VideoAdapter(this, videos, object : VideoAdapter.OnVideoListener {
            override fun onVideoPrepared(exoPlayerItem: ExoPlayerItem) {
                exoPlayerItems.add(exoPlayerItem)
            }

            override fun onVideoClick(position: Int) {
                isAudioMuted = !isAudioMuted

                val index =
                    exoPlayerItems.indexOfFirst { it.position == binding.viewPager2.currentItem }
                if (index != -1) {
                    val player = exoPlayerItems[index].exoPlayer
                    player.volume = if (isAudioMuted) 0f else 1f
                }

                if (isAudioMuted) {
                    binding.ivSpeaker.setImageResource(R.drawable.speaker_muted)
                } else {
                    binding.ivSpeaker.setImageResource(R.drawable.speaker_normal)
                }

                binding.ivSpeaker.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.ivSpeaker.visibility = View.GONE
                }, 1000)
            }
        })

        binding.viewPager2.adapter = adapter

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val previousIndex = exoPlayerItems.indexOfFirst { it.exoPlayer.isPlaying }
                if (previousIndex != -1) {
                    val player = exoPlayerItems[previousIndex].exoPlayer
                    player.pause()
                    player.playWhenReady = false
                }
                val newIndex = exoPlayerItems.indexOfFirst { it.position == position }
                if (newIndex != -1) {
                    val player = exoPlayerItems[newIndex].exoPlayer
                    player.playWhenReady = true
                    player.volume = if (isAudioMuted) 0f else 1f
                    player.play()
                }
            }
        })

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1001)) {
            getAllVideos()
        } else {
            Toast.makeText(this, "Please grant storage permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllVideos() {
        val projection = arrayOf(MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                if (titleIndex > -1 && dataIndex > -1) {
                    val title = cursor.getString(titleIndex)
                    val path = cursor.getString(dataIndex)
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            val uri = Uri.fromFile(file)
                            videos.add(Video(title, uri))
                            adapter.notifyItemInserted(videos.size - 1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            for (g in grantResults) {
                if (g != PermissionChecker.PERMISSION_GRANTED) {
                    return
                }
            }
            getAllVideos()
        }
    }

    override fun onPause() {
        super.onPause()

        val index = exoPlayerItems.indexOfFirst { it.position == binding.viewPager2.currentItem }
        if (index != -1) {
            val player = exoPlayerItems[index].exoPlayer
            player.pause()
            player.playWhenReady = false
        }
    }

    override fun onResume() {
        super.onResume()

        val index = exoPlayerItems.indexOfFirst { it.position == binding.viewPager2.currentItem }
        if (index != -1) {
            val player = exoPlayerItems[index].exoPlayer
            player.playWhenReady = true
            player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exoPlayerItems.isNotEmpty()) {
            for (item in exoPlayerItems) {
                val player = item.exoPlayer
                player.stop()
                player.clearMediaItems()
            }
        }
    }
}