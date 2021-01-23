package vaida.dryzaite.currencyconverter.ui.converterfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.databinding.FragmentConverterBinding
import vaida.dryzaite.currencyconverter.ui.BaseFragment
import vaida.dryzaite.currencyconverter.util.NavigationSettings


@AndroidEntryPoint
class ConverterFragment : BaseFragment<ConverterFragmentViewModel, FragmentConverterBinding>() {
    override val navigationSettings: NavigationSettings? by lazy {
        NavigationSettings(requireContext().getString(R.string.app_name))
    }
    override val layoutId : Int = R.layout.fragment_converter

    override fun getViewModelClass(): Class<ConverterFragmentViewModel> {
        return ConverterFragmentViewModel::class.java
    }
    override fun setupUI() {
        Toast.makeText(requireContext(), "kdasndkja", Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConverterConvert.setOnClickListener {
            viewModel.convert(
                binding.etConverterFrom.text.toString(),
                binding.spConverterFromCurrency.selectedItem.toString(),
                binding.spConverterToCurrency.selectedItem.toString()
            )
        }

        lifecycleScope.launchWhenStarted {
            viewModel.conversion.collect { event ->
                when (event) {
                    is ConverterFragmentViewModel.CurrencyEvent.Success -> {
                        binding.progressBarConverter.isVisible = false
                        binding.etConverterTo.text = event.resultText
                    }
                    is ConverterFragmentViewModel.CurrencyEvent.Failure -> {
                        binding.progressBarConverter.isVisible = false
                        binding.etConverterTo.text = event.errorText
                        binding.etConverterTo.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.red
                            )
                        )
                    }
                    is ConverterFragmentViewModel.CurrencyEvent.Loading -> {
                        binding.progressBarConverter.isVisible = true
                    }
                    else -> Unit
                }
            }
        }

    }


}