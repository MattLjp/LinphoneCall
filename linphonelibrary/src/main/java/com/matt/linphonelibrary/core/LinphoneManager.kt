package com.matt.linphonelibrary.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.TextureView
import com.matt.linphonelibrary.R
import com.matt.linphonelibrary.callback.PhoneCallback
import com.matt.linphonelibrary.callback.RegistrationCallback
import com.matt.linphonelibrary.utils.AudioRouteUtils
import com.matt.linphonelibrary.utils.LinphoneUtils
import com.matt.linphonelibrary.utils.VideoZoomHelper
import org.linphone.core.*
import java.io.File
import java.util.*

/**
 * @ Author : 廖健鹏
 * @ Time : 2021/6/23
 * @ e-mail : 329524627@qq.com
 * @ Description :
 */
class LinphoneManager private constructor(private val context: Context) {
    private val TAG = javaClass.simpleName

    private var core: Core
    private var corePreferences: CorePreferences
    private var coreIsStart = false
    var registrationCallback: RegistrationCallback? = null
    var phoneCallback: PhoneCallback? = null


    init {
        //日志收集
        Factory.instance().setLogCollectionPath(context.filesDir.absolutePath)
        Factory.instance().enableLogCollection(LogCollectionState.Enabled)

        corePreferences = CorePreferences(context)
        corePreferences.copyAssetsFromPackage()
        val config = Factory.instance().createConfigWithFactory(
            corePreferences.configPath,
            corePreferences.factoryConfigPath
        )
        corePreferences.config = config

        val appName = context.getString(R.string.app_name)
        Factory.instance().setDebugMode(corePreferences.debugLogs, appName)

        core = Factory.instance().createCoreWithConfig(config, context)
    }

    private var previousCallState = Call.State.Idle

    private val coreListener = object : CoreListenerStub() {
        override fun onGlobalStateChanged(core: Core, state: GlobalState?, message: String) {
            if (state === GlobalState.On) {
            }
        }

        //登录状态回调
        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            when (state) {
                RegistrationState.None -> registrationCallback?.registrationNone()
                RegistrationState.Progress -> registrationCallback?.registrationProgress()
                RegistrationState.Ok -> registrationCallback?.registrationOk()
                RegistrationState.Cleared -> registrationCallback?.registrationCleared()
                RegistrationState.Failed -> registrationCallback?.registrationFailed()
            }
        }

        //电话状态回调
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Log.i(TAG, "[Context] Call state changed [$state]")

            when (state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                    if (gsmCallActive) {
                        Log.w(
                            TAG,
                            "[Context] Refusing the call with reason busy because a GSM call is active"
                        )
                        call.decline(Reason.Busy)
                        return
                    }

                    phoneCallback?.incomingCall(call)
                    gsmCallActive = true

                    //自动接听
                    if (corePreferences.autoAnswerEnabled) {
                        val autoAnswerDelay = corePreferences.autoAnswerDelay
                        if (autoAnswerDelay == 0) {
                            Log.w(TAG, "[Context] Auto answering call immediately")
                            answerCall(call)
                        } else {
                            Log.i(
                                TAG,
                                "[Context] Scheduling auto answering in $autoAnswerDelay milliseconds"
                            )
                            val mainThreadHandler = Handler(Looper.getMainLooper())
                            mainThreadHandler.postDelayed({
                                Log.w(TAG, "[Context] Auto answering call")
                                answerCall(call)
                            }, autoAnswerDelay.toLong())
                        }
                    }
                }

                Call.State.OutgoingInit -> {
                    phoneCallback?.outgoingInit(call)
                    gsmCallActive = true
                }

                Call.State.OutgoingProgress -> {
                    if (core.callsNb == 1 && corePreferences.routeAudioToBluetoothIfAvailable) {
                        AudioRouteUtils.routeAudioToBluetooth(core, call)
                    }
                }

                Call.State.Connected -> phoneCallback?.callConnected(call)

                Call.State.StreamsRunning -> {
                    // Do not automatically route audio to bluetooth after first call
                    if (core.callsNb == 1) {
                        // Only try to route bluetooth / headphone / headset when the call is in StreamsRunning for the first time
                        if (previousCallState == Call.State.Connected) {
                            Log.i(
                                TAG,
                                "[Context] First call going into StreamsRunning state for the first time, trying to route audio to headset or bluetooth if available"
                            )
                            if (AudioRouteUtils.isHeadsetAudioRouteAvailable(core)) {
                                AudioRouteUtils.routeAudioToHeadset(core, call)
                            } else if (corePreferences.routeAudioToBluetoothIfAvailable && AudioRouteUtils.isBluetoothAudioRouteAvailable(
                                    core
                                )
                            ) {
                                AudioRouteUtils.routeAudioToBluetooth(core, call)
                            }
                        }
                    }

                    if (corePreferences.routeAudioToSpeakerWhenVideoIsEnabled && call.currentParams.videoEnabled()) {
                        // Do not turn speaker on when video is enabled if headset or bluetooth is used
                        if (!AudioRouteUtils.isHeadsetAudioRouteAvailable(core) &&
                            !AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed(core, call)
                        ) {
                            Log.i(
                                TAG,
                                "[Context] Video enabled and no wired headset not bluetooth in use, routing audio to speaker"
                            )
                            AudioRouteUtils.routeAudioToSpeaker(core, call)
                        }
                    }
                }
                Call.State.End, Call.State.Released, Call.State.Error -> {
                    if (core.callsNb == 0) {
                        when (state) {
                            Call.State.End -> phoneCallback?.callEnd(call)

                            Call.State.Released -> phoneCallback?.callReleased(call)

                            Call.State.Error -> {
                                val id = when (call.errorInfo.reason) {
                                    Reason.Busy -> R.string.call_error_user_busy
                                    Reason.IOError -> R.string.call_error_io_error
                                    Reason.NotAcceptable -> R.string.call_error_incompatible_media_params
                                    Reason.NotFound -> R.string.call_error_user_not_found
                                    Reason.Forbidden -> R.string.call_error_forbidden
                                    else -> R.string.call_error_unknown
                                }
                                phoneCallback?.error(context.getString(id))
                            }
                        }
                        gsmCallActive = false
                    }
                }
            }
            previousCallState = state
        }
    }

    /**
     * 启动linphone
     */
    fun start() {
        if (!coreIsStart) {
            coreIsStart = true
            Log.i(TAG, "[Context] Starting")
            core.addListener(coreListener)
            core.start()

            initLinphone()

            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            Log.i(TAG, "[Context] Registering phone state listener")
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    /**
     * 停止linphone
     */
    fun stop() {
        coreIsStart = false
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        Log.i(TAG, "[Context] Unregistering phone state listener")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        core.removeListener(coreListener)
        core.stop()
    }


    /**
     * 注册到服务器
     *
     * @param username     账号名
     * @param password      密码
     * @param domain     IP地址：端口号
     */
    fun createProxyConfig(
        username: String,
        password: String,
        domain: String,
        type: TransportType? = TransportType.Udp
    ) {
        core.clearProxyConfig()

        val accountCreator = core.createAccountCreator(corePreferences.xmlRpcServerUrl)
        accountCreator.language = Locale.getDefault().language
        accountCreator.reset()

        accountCreator.username = username
        accountCreator.password = password
        accountCreator.domain = domain
        accountCreator.displayName = username
        accountCreator.transport = type

        accountCreator.createProxyConfig()
    }


    /**
     * 取消注册
     */
    fun removeInvalidProxyConfig() {
        core.clearProxyConfig()

    }


    /**
     * 拨打电话
     * @param to String
     * @param isVideoCall Boolean
     */
    fun startCall(to: String, isVideoCall: Boolean) {
        try {
            val addressToCall = core.interpretUrl(to)
            addressToCall?.displayName = to
            val params = core.createCallParams(null)
            //启用通话录音
//            params?.recordFile = LinphoneUtils.getRecordingFilePathForAddress(context, addressToCall!!)
            //启动低宽带模式
            if (LinphoneUtils.checkIfNetworkHasLowBandwidth(context)) {
                Log.w(TAG, "[Context] Enabling low bandwidth mode!")
                params?.enableLowBandwidth(true)
            }
            if (isVideoCall) {
                params?.enableVideo(true)
                core.enableVideoCapture(true)
                core.enableVideoDisplay(true)
            } else {
                params?.enableVideo(false)
            }
            if (params != null) {
                core.inviteAddressWithParams(addressToCall!!, params)
            } else {
                core.inviteAddress(addressToCall!!)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    /**
     * 接听来电
     *
     */
    fun answerCall(call: Call) {
        Log.i(TAG, "[Context] Answering call $call")
        val params = core.createCallParams(call)
        //启用通话录音
//        params?.recordFile = LinphoneUtils.getRecordingFilePathForAddress(context, call.remoteAddress)
        if (LinphoneUtils.checkIfNetworkHasLowBandwidth(context)) {
            Log.w(TAG, "[Context] Enabling low bandwidth mode!")
            params?.enableLowBandwidth(true)
        }
        params?.enableVideo(isVideoCall(call))
        call.acceptWithParams(params)
    }

    /**
     * 谢绝电话
     * @param call Call
     */
    fun declineCall(call: Call) {
        val voiceMailUri = corePreferences.voiceMailUri
        if (voiceMailUri != null && corePreferences.redirectDeclinedCallToVoiceMail) {
            val voiceMailAddress = core.interpretUrl(voiceMailUri)
            if (voiceMailAddress != null) {
                Log.i(TAG, "[Context] Redirecting call $call to voice mail URI: $voiceMailUri")
                call.redirectTo(voiceMailAddress)
            }
        } else {
            Log.i(TAG, "[Context] Declining call $call")
            call.decline(Reason.Declined)
        }
    }

    /**
     * 挂断电话
     */
    fun terminateCall(call: Call) {
        Log.i(TAG, "[Context] Terminating call $call")
        call.terminate()
    }

    fun micEnabled() = core.micEnabled()

    fun speakerEnabled() = core.outputAudioDevice?.type == AudioDevice.Type.Speaker

    /**
     * 启动麦克风
     * @param micEnabled Boolean
     */
    fun enableMic(micEnabled: Boolean) {
        core.enableMic(micEnabled)
    }

    /**
     * 扬声器或听筒
     * @param SpeakerEnabled Boolean
     */
    fun enableSpeaker(SpeakerEnabled: Boolean) {
        if (SpeakerEnabled) {
            AudioRouteUtils.routeAudioToEarpiece(core)
        } else {
            AudioRouteUtils.routeAudioToSpeaker(core)
        }
    }


    /**
     * 是否是视频电话
     * @return Boolean
     */
    fun isVideoCall(call: Call): Boolean {
        val remoteParams = call.remoteParams
        return remoteParams != null && remoteParams.videoEnabled()
    }


    /**
     * 设置视频界面
     * @param videoRendering TextureView 对方界面
     * @param videoPreview CaptureTextureView 自己界面
     */
    fun setVideoWindowId(videoRendering: TextureView, videoPreview: TextureView) {
        core.nativeVideoWindowId = videoRendering
        core.nativePreviewWindowId = videoPreview
    }

    /**
     * 设置视频电话可缩放
     * @param context Context
     * @param videoRendering TextureView
     */
    fun setVideoZoom(context: Context, videoRendering: TextureView) {
        VideoZoomHelper(context, videoRendering, core)
    }

    fun switchCamera() {
        val currentDevice = core.videoDevice
        Log.i(TAG, "[Context] Current camera device is $currentDevice")

        for (camera in core.videoDevicesList) {
            if (camera != currentDevice && camera != "StaticImage: Static picture") {
                Log.i(TAG, "[Context] New camera device will be $camera")
                core.videoDevice = camera
                break
            }
        }

        val conference = core.conference
        if (conference == null || !conference.isIn) {
            val call = core.currentCall
            if (call == null) {
                Log.w(TAG, "[Context] Switching camera while not in call")
                return
            }
            call.update(null)
        }
    }


    //初始化一些操作
    private fun initLinphone() {

        configureCore()

        initUserCertificates()
    }


    private fun configureCore() {
        // 来电铃声
        core.isNativeRingingEnabled = true
        // 来电振动
        core.isVibrationOnIncomingCallEnabled = true
        core.enableEchoCancellation(true) //回声消除
        core.enableAdaptiveRateControl(true) //自适应码率控制
    }

    private var gsmCallActive = false
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            gsmCallActive = when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.i(TAG, "[Context] Phone state is off hook")
                    true
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.i(TAG, "[Context] Phone state is ringing")
                    true
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.i(TAG, "[Context] Phone state is idle")
                    false
                }
                else -> {
                    Log.i(TAG, "[Context] Phone state is unexpected: $state")
                    false
                }
            }
        }
    }


    //设置存放用户x509证书的目录路径
    private fun initUserCertificates() {
        val userCertsPath = corePreferences!!.userCertificatesPath
        val f = File(userCertsPath)
        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.e(TAG, "[Context] $userCertsPath can't be created.")
            }
        }
        core.userCertificatesPath = userCertsPath
    }


    companion object {

        // For Singleton instantiation
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: LinphoneManager? = null
        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: LinphoneManager(context).also { instance = it }
            }

    }

}