package com.matt.linphonecall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.matt.linphonecall.databinding.CallVoiceFragmentBinding
import org.linphone.core.Call

class CallVoiceFragment : Fragment() {
    private lateinit var binding: CallVoiceFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = CallVoiceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.number.text = number
        binding.chronometer.start()

        binding.micro.isSelected = (activity as MainActivity).linphoneManager.micEnabled()
        binding.speaker.isSelected = (activity as MainActivity).linphoneManager.speakerEnabled()

        binding.hangUp.setOnClickListener {
            (activity as MainActivity).linphoneManager.terminateCall(currentCall!!)
        }
        binding.micro.setOnClickListener {
            val boolean = !(activity as MainActivity).linphoneManager.micEnabled()
            binding.micro.isSelected = boolean
            (activity as MainActivity).linphoneManager.enableMic(boolean)
        }
        binding.speaker.setOnClickListener {
            val boolean = !(activity as MainActivity).linphoneManager.speakerEnabled()
            binding.micro.isSelected = boolean
            (activity as MainActivity).linphoneManager.enableSpeaker(boolean)
        }
    }

    companion object {
        var number = ""
        var currentCall: Call? = null

        fun newInstance(string: String, call: Call): Fragment {
            number = string
            currentCall = call
            return CallVoiceFragment()
        }
    }

}