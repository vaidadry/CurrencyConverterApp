package vaida.dryzaite.currencyconverter.ui.converterfragment

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.data.db.UserBalance
import vaida.dryzaite.currencyconverter.databinding.ItemBalanceBinding

class BalancesAdapter : RecyclerView.Adapter<BalancesAdapter.BalancesViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<UserBalance>() {
        override fun areItemsTheSame(oldItem: UserBalance, newItem: UserBalance): Boolean {
            return oldItem.currency == newItem.currency
        }

        override fun areContentsTheSame(oldItem: UserBalance, newItem: UserBalance): Boolean {
            return oldItem.currency == newItem.currency &&
                    oldItem.amount == newItem.amount
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var balances: List<UserBalance>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalancesViewHolder {
        val itemBinding = ItemBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BalancesViewHolder(itemBinding, parent.context)
    }

    override fun onBindViewHolder(holder: BalancesViewHolder, position: Int) {
        val balance = balances[position]
        holder.itemView.setOnClickListener {
            onItemClickListener?.let {
                it(balance)
            }
        }
        holder.bind(balance)
    }

    override fun getItemCount(): Int {
        return balances.size
    }

    class BalancesViewHolder(private val binding: ItemBalanceBinding, val context: Context) : RecyclerView.ViewHolder(binding.root) {
        fun bind(balance: UserBalance) {
            binding.tvBalance.text = context
                .getString(R.string.balance_item_placeholder)
                .format(balance.amount, balance.currency)
        }
    }

    private var onItemClickListener: ((UserBalance) -> Unit)? = null

    fun setItemClickedListener(listener: (UserBalance) -> Unit) {
        onItemClickListener = listener
    }
}