package vaida.dryzaite.currencyconverter.ui.converterfragment

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.databinding.FragmentConverterBinding
import vaida.dryzaite.currencyconverter.ui.BaseFragment
import vaida.dryzaite.currencyconverter.util.ConverterManager
import vaida.dryzaite.currencyconverter.util.NavigationSettings
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ConverterFragment @Inject constructor() : BaseFragment<ConverterFragmentViewModel, FragmentConverterBinding>() {
    private val balanceAdapter = BalancesAdapter()
    private var searchJob: Job? = null

    override val navigationSettings: NavigationSettings? by lazy {
        NavigationSettings(requireContext().getString(R.string.app_name))
    }
    override val layoutId: Int = R.layout.fragment_converter

    override fun getViewModelClass(): Class<ConverterFragmentViewModel> {
        return ConverterFragmentViewModel::class.java
    }

    override fun setupUI() {
        binding.spConverterFromCurrency.setSelection(
            resources.getStringArray(R.array.currency_codes).indexOf(
                requireContext().getString(R.string.defaultCurrencySelected))
        )
        setupInputListener()
        observeBalances()
        viewModel.getInitBalances()
        setupRecyclerView()

        viewModel.amountInput.observe(viewLifecycleOwner, Observer {
            initConvert()
        })
        viewModel.currencyFromInput.observe(viewLifecycleOwner, Observer {
            initConvert()
        })
        viewModel.currencyToInput.observe(viewLifecycleOwner, Observer {
            initConvert()
        })

        binding.btnConverterConvert.setOnClickListener {
            viewModel.updateBalances(
                requireContext(),
                binding.etConverterFrom.text.toString(),
                binding.spConverterFromCurrency.selectedItem.toString(),
                binding.spConverterToCurrency.selectedItem.toString(),
                binding.etConverterTo.text.toString()
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            viewModel.conversion.collect { event ->
                when (event) {
                    is ConverterManager.ExchangeEvent.Success -> {
                        binding.btnConverterConvert.isEnabled = true
                        binding.etConverterTo.text = event.resultText
                        binding.etConverterTo.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.green
                            )
                        )
                    }
                    is ConverterManager.ExchangeEvent.Failure -> {
                        Snackbar.make(binding.root, event.errorText, Snackbar.LENGTH_LONG).show()
                        binding.etConverterTo.text = requireContext().getString(R.string.converter_error)
                        binding.etConverterTo.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.red
                            )
                        )
                    }
                    is ConverterManager.ExchangeEvent.Loading -> {
                        binding.btnConverterConvert.isEnabled = false
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.balanceUpdate.collect { event ->
                when (event) {
                    is ConverterManager.BalanceUpdateEvent.Failure -> {
                        binding.progressBarConverter.isVisible = false
                        Snackbar.make(binding.root, event.errorText, Snackbar.LENGTH_LONG).show()
                    }
                    is ConverterManager.BalanceUpdateEvent.Loading -> {
                        binding.progressBarConverter.isVisible = true
                    }
                    is ConverterManager.BalanceUpdateEvent.Success -> {
                        binding.progressBarConverter.isVisible = false
                        balanceAdapter.balances = event.balances
                        showAlertDialog(event.dialog)
                }
                    else -> Unit
                }
            }
        }
    }

    private fun initConvert() {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            tickFlow(5000L).collect {
                viewModel.convert(
                    requireContext(),
                    binding.etConverterFrom.text.toString(),
                    binding.spConverterFromCurrency.selectedItem.toString(),
                    binding.spConverterToCurrency.selectedItem.toString()
                )
            }
        }
    }

    private fun tickFlow(millis: Long) = callbackFlow {
        val timer = Timer()
        var time = 0
        timer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    try { offer(time) } catch (e: Exception) {}
                    time += 1
                }
            },
            0,
            millis)
        awaitClose {
            timer.cancel()
        }
    }

    private fun observeBalances() {
        viewModel.balances.observe(viewLifecycleOwner, Observer {
            balanceAdapter.apply {
                this.balances = it ?: arrayListOf()
                this.setItemClickedListener {
                    binding.spConverterFromCurrency.setSelection(
                        resources.getStringArray(R.array.currency_codes).indexOf(
                            it.currency)
                    )
                }
            }
        })
    }

    private fun setupRecyclerView() {
        binding.rvConverterBalances.apply {
            adapter = balanceAdapter
        }
    }

    private fun setupInputListener() {
        binding.etConverterFrom.doOnTextChanged { text, _, _, _ ->
            text?.let {
                if (it.isNotEmpty()) {
                    viewModel.updateInputQuery(it.toString())
                }
            }
        }
        binding.spConverterFromCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = resources.getStringArray(R.array.currency_codes)[position]
                viewModel.updateCurrencyFromQuery(selected)
            }
        }
        binding.spConverterToCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = resources.getStringArray(R.array.currency_codes)[position]
                viewModel.updateCurrencyFromQuery(selected)
            }
        }
    }

    private fun showAlertDialog(message: String) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        alertDialog.setTitle(requireContext().getString(R.string.converter_dialog_title))
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(
            requireContext().getString(R.string.converter_dialog_button)) { _, _ -> }
        val alert: AlertDialog = alertDialog.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
}