package com.matt.linphonecall

import android.app.Activity
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.matt.linphonecall.databinding.CallOutgoingFragmentBinding
import org.linphone.core.Call

class CallOutgoingFragment : Fragment(), SurfaceHolder.Callback {
    private lateinit var binding: CallOutgoingFragmentBinding
    private var surfaceHolder: SurfaceHolder? = null
    private var camera: Camera? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = CallOutgoingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.number.text = number
        if (isVideo) {
            binding.surfaceView.visibility = View.VISIBLE
            surfaceHolder = binding.surfaceView.holder
            surfaceHolder?.setFormat(PixelFormat.TRANSPARENT)
            surfaceHolder?.setKeepScreenOn(true)
            surfaceHolder?.addCallback(this)
        } else {
            binding.surfaceView.visibility = View.INVISIBLE
        }


        binding.hangUp.setOnClickListener {
            (activity as MainActivity).linphoneManager.terminateCall(currentCall!!)
        }
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val cameraInfo = CameraInfo()
            val cameraCount = Camera.getNumberOfCameras()
            for (camIdx in 0 until cameraCount) {
                Camera.getCameraInfo(camIdx, cameraInfo)
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    camera = Camera.open(camIdx)
                    setCameraDisplayOrientation(
                        requireActivity(),
                        camIdx,
                        camera!!
                    )
                }
            }
            if (camera == null) {
                camera = Camera.open()
                setCameraDisplayOrientation(
                    requireActivity(),
                    0,
                    camera!!
                )
            }
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            if (null != camera) {
                camera?.release()
                camera = null
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (null != camera) {
            camera?.stopPreview()
            camera?.release()
            camera = null
        }
    }

    override fun onPause() {
        if (camera != null) {
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
            camera?.release()
            camera = null
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: Camera) {
        val info = CameraInfo()
        //获取摄像头信息
        Camera.getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        //获取摄像头当前的角度
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            //前置摄像头
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {
            // back-facing  后置摄像头
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    companion object {
        var isVideo = false
        var number = ""
        var currentCall: Call? = null

        fun newInstance(string: String, boolean: Boolean, call: Call): Fragment {
            number = string
            isVideo = boolean
            currentCall = call
            return CallOutgoingFragment()
        }
    }

}