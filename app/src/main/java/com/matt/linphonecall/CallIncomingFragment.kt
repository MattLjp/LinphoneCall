package com.matt.linphonecall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.matt.linphonecall.databinding.CallIncomingFragmentBinding
import org.linphone.core.Call


class CallIncomingFragment : Fragment() {
    private lateinit var binding: CallIncomingFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = CallIncomingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.number.text = number
        if (isVideo) {
            binding.type.text = "视频通话"
        } else {
            binding.type.text = "语音通话"
        }

        binding.answer.setOnClickListener {
            (activity as MainActivity).linphoneManager.answerCall(currentCall!!)
        }
        binding.hangUp.setOnClickListener {
            (activity as MainActivity).linphoneManager.declineCall(currentCall!!)
        }

    }


    companion object {
        var isVideo = false
        var number = ""
        var currentCall: Call? = null

        fun newInstance(string: String, boolean: Boolean, call: Call): Fragment {
            number = string
            isVideo = boolean
            currentCall = call
            return CallIncomingFragment()
        }
    }

}