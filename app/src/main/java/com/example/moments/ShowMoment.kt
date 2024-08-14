package com.example.moments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import com.bumptech.glide.Glide
import com.example.moments.databinding.ShowMomentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ShowMoment(private val moment: Moment, private val index: Int) : DialogFragment() {

    private var _binding: ShowMomentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mainActivity = activity as MainActivity
        val inflater = mainActivity.layoutInflater
        _binding = ShowMomentBinding.inflate(inflater)

        val builder = AlertDialog.Builder(mainActivity)
            .setView(binding.root)

        binding.txtTitle.text = moment.title
        binding.txtContents.text = moment.contents

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.txtDate.text = dateFormat.format(moment.date)
        binding.txtDate.visibility = View.VISIBLE
        Log.d("ShowMoment", "Displaying moment with address: ${moment.address}")


        moment.address?.let { address ->
            binding.txtAddress.text = address
            binding.txtAddress.visibility = View.VISIBLE
            Log.d("ShowMoment", "Address set and visible")
        } ?: run {
            binding.txtAddress.visibility = View.GONE
            Log.d("ShowMoment", "Address not available, view hidden")
        }


        if (moment.photoPath != null) {
            Glide.with(this)
                .load(moment.photoPath)
                .into(binding.imageView)
            binding.imageView.visibility = View.VISIBLE
        } else {
            binding.imageView.visibility = View.GONE
        }

        binding.btnOK.setOnClickListener {
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            mainActivity.deleteMoment(index)
            Toast.makeText(mainActivity, resources.getString(R.string.moment_deleted), Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}