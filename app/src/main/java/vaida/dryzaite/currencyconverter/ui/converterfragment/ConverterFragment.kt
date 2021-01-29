package vaida.dryzaite.currencyconverter.ui.converterfragment

import android.os.Bundle
import android.util.Log
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
    private var timerJob: Job? = null

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
        viewModel.channel.offer(ConverterFragmentViewModel.ConverterEvents.InitBalancesEvent)
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
            viewModel.channel.offer(
                ConverterFragmentViewModel.ConverterEvents.UpdateBalancesEvent(
                binding.etConverterFrom.text.toString(),
                binding.spConverterFromCurrency.selectedItem.toString(),
                binding.spConverterToCurrency.selectedItem.toString(),
                binding.etConverterTo.text.toString()
                )
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
                        binding.etConverterTo.text = requireContext().getString(R.string.balance_item_converted).format(event.result)
                        binding.etConverterTo.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.green
                            )
                        )
                    }
                    is ConverterManager.ExchangeEvent.Failure -> {
                        Snackbar.make(
                            binding.root,
                            requireContext().getString(event.errorResource),
                            Snackbar.LENGTH_LONG
                        ).show()
                        binding.etConverterTo.text = requireContext().getString(R.string.converter_error)
                        binding.etConverterTo.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.red
                            )
                        )
                        timerJob?.cancel()
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
                        Snackbar.make(
                            binding.root,
                            requireContext().getString(event.errorResource),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is ConverterManager.BalanceUpdateEvent.Loading -> {
                        binding.progressBarConverter.isVisible = true
                    }
                    is ConverterManager.BalanceUpdateEvent.Success -> {
                        binding.progressBarConverter.isVisible = false
                        balanceAdapter.balances = event.balances

                        val dialogMessage =
                            event.latestOperation.run {
                                if (event.fee != 0.00) {
                                    requireContext()
                                        .getString(R.string.converter_dialog_message)
                                        .format(
                                            fromAmount,
                                            currencyFrom,
                                            toAmount,
                                            currencyTo,
                                            event.fee,
                                            currencyFrom
                                        )
                                } else {
                                    requireContext()
                                        .getString(R.string.converter_dialog_message_no_fee)
                                        .format(
                                            fromAmount,
                                            currencyFrom,
                                            toAmount,
                                            currencyTo
                                        )
                                }
                            }
                        showAlertDialog(dialogMessage)
                }
                    else -> Unit
                }
            }
        }
    }

    private fun initConvert() {
        Log.i("MSG", "initconvert called")

        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.IO).launch {
            tickFlow(5000L).collect {
                viewModel.channel.offer(
                    ConverterFragmentViewModel.ConverterEvents.ConvertEvent(
                        binding.etConverterFrom.text.toString(),
                        binding.spConverterFromCurrency.selectedItem.toString(),
                        binding.spConverterToCurrency.selectedItem.toString()
                    )
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
                    viewModel.channel.offer(
                        ConverterFragmentViewModel.ConverterEvents.UpdateInputQueryEvent(
                            it.toString()
                        )
                    )
                }
            }
        }
        binding.spConverterFromCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = resources.getStringArray(R.array.currency_codes)[position]
                viewModel.channel.offer(
                    ConverterFragmentViewModel.ConverterEvents.UpdateFromCurrencyEvent(selected))
            }
        }
        binding.spConverterToCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = resources.getStringArray(R.array.currency_codes)[position]
                viewModel.channel.offer(
                    ConverterFragmentViewModel.ConverterEvents.UpdateToCurrencyEvent(selected)
                )
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