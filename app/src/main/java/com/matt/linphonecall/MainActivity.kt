package com.matt.linphonecall

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.matt.linphonecall.databinding.ActivityMainBinding
import com.matt.linphonecall.databinding.CallIncomingFragmentBinding
import com.matt.linphonelibrary.callback.PhoneCallback
import com.matt.linphonelibrary.callback.RegistrationCallback
import com.matt.linphonelibrary.core.CoreService
import com.matt.linphonelibrary.core.LinphoneManager
import org.linphone.core.Call
import org.linphone.core.TransportType

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var linphoneManager: LinphoneManager
    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
            //通过的权限
            val grantedList = it.filterValues { it }.mapNotNull { it.key }

            //未通过的权限
            val deniedList = (it - grantedList).map { it.key }

            //拒绝并且点了“不再询问”权限
            val alwaysDeniedList = deniedList.filterNot {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

            if (deniedList.isNotEmpty()) {
                finish()
            } else {
                linphoneManager = LinphoneManager.getInstance(this)
                linphoneManager.start()
                linphoneManager.registrationCallback = object : RegistrationCallback() {
                    override fun registrationOk() {
                        binding.tips.text = "登录成功"
                    }

                    override fun registrationCleared() {
                        binding.tips.text = "未登录"
                    }

                    override fun registrationFailed() {
                        binding.tips.text = "登录失败"
                    }

                    override fun registrationNone() {
                        binding.tips.text = "未登录"
                    }
                }
                linphoneManager.phoneCallback = object : PhoneCallback() {
                    override fun incomingCall(call: Call?) {
                        showCall(
                            1,
                            call!!,
                            call.remoteAddress.displayName ?: "",
                            linphoneManager.isVideoCall(call)
                        )
                    }

                    override fun outgoingInit(call: Call?) {
                        showCall(
                            2,
                            call!!,
                            call.remoteAddress.displayName!!,
                            isVideo
                        )
                    }

                    override fun callConnected(call: Call?) {
                        if (linphoneManager.isVideoCall(call!!)) {
                            showCall(4, call, call.remoteAddress.displayName ?: "")
                        } else {
                            showCall(3, call, call.remoteAddress.displayName ?: "")
                        }

                    }

                    override fun callReleased(call: Call?) {
                        showCall(0, call!!)
                    }

                    override fun callEnd(call: Call?) {

                    }

                    override fun error(string: String?) {
                        Toast.makeText(this@MainActivity, string, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        binding.login.setOnClickListener {
            val account = binding.account.text.toString()
            val password = binding.password.text.toString()
            val ip = binding.ip.text.toString()
            val port = binding.port.text.toString()
            val type = when (binding.radioGroup.checkedRadioButtonId) {
                R.id.udp -> TransportType.Udp
                R.id.tcp -> TransportType.Tcp
                else -> TransportType.Tls
            }
            if (account.isEmpty() || password.isEmpty() || ip.isEmpty() || port.isEmpty()) return@setOnClickListener
            linphoneManager.createProxyConfig(account, password, "$ip:$port", type)

        }
        binding.logout.setOnClickListener {
            linphoneManager.removeInvalidProxyConfig()
        }
        binding.voice.setOnClickListener {
            isVideo = false
            val number = binding.number.text.toString()
            linphoneManager.startCall(number, false)
        }
        binding.video.setOnClickListener {
            isVideo = true
            val number = binding.number.text.toString()
            linphoneManager.startCall(number, true)
        }

    }

    private var lastFragment: Fragment? = null
    private fun showCall(type: Int, call: Call, number: String = "", isVideo: Boolean = false) {
        when (type) {
            0 -> {
                binding.frameLayout.visibility = View.INVISIBLE
                supportFragmentManager.beginTransaction()
                    .remove(lastFragment!!)
                    .commit()
            }
            1 -> {
                binding.frameLayout.visibility = View.VISIBLE
                lastFragment = CallIncomingFragment.newInstance(number, isVideo, call)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, lastFragment!!)
                    .commit()
            }
            2 -> {
                binding.frameLayout.visibility = View.VISIBLE
                lastFragment = CallOutgoingFragment.newInstance(number, isVideo, call)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, lastFragment!!)
                    .commit()
            }
            3 -> {
                binding.frameLayout.visibility = View.VISIBLE
                lastFragment = CallVoiceFragment.newInstance(number, call)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, lastFragment!!)
                    .commit()
            }
            4 -> {
                binding.frameLayout.visibility = View.VISIBLE
                lastFragment = CallVideoFragment.newInstance(number, call)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, lastFragment!!)
                    .commit()
            }
        }
    }
}