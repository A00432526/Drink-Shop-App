package com.hoc.drinkshop

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.drink_item_layout.view.*
import kotlinx.coroutines.experimental.timeunit.TimeUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@Suppress("unused")
inline val Any?.unit
    get() = Unit

private typealias DrinkAdapterSubjectItemType = Pair<Int, Drink>

class DrinkAdapter(
    private val onCLickListener: (Drink) -> Unit,
    private val userPhone: String
) : ListAdapter<Drink, DrinkAdapter.ViewHolder>(diffCallback) {
    private val subject = PublishSubject.create<DrinkAdapterSubjectItemType>()
    val clickObservable: Observable<DrinkAdapterSubjectItemType>
        get() = subject.throttleFirst(
            400,
            TimeUnit.MILLISECONDS
        )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent inflate R.layout.drink_item_layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val textDrinkName = itemView.textDrinkName!!
        private val imageDrink = itemView.imageDrink!!
        private val imageAddToCart = itemView.imageAddToCart!!
        private val buttonFav = itemView.buttonFav!!
        private val imageFav = itemView.imageFav!!
        private val textNumberOfStars = itemView.textNumberOfStars!!
        private val textDrinkPrice = itemView.textDrinkPrice!!
        private val cardView = itemView.cardView

        init {
            imageAddToCart.setOnClickListener(this)
            cardView.setOnClickListener(this)
            buttonFav.setOnClickListener(this)
        }

        override fun onClick(v: View) = when (v.id) {
            R.id.buttonFav -> adapterPosition(::getItem)?.let(subject::onNext).unit
            else -> adapterPosition { onCLickListener(getItem(it)) }.unit
        }

        fun bind(drink: Drink) {
            textDrinkName.text = drink.name
            textDrinkPrice.text =
                itemView.context.getString(R.string.price, decimalFormatPrice.format(drink.price))
            textNumberOfStars.text = decimalFormatStarCount.format(drink.starCount.toLong())
            Picasso.get()
                .load(drink.imageUrl)
                .fit()
                .error(R.drawable.ic_image_black_24dp)
                .placeholder(R.drawable.ic_image_black_24dp)
                .into(imageDrink)

            val isFavorite = userPhone in drink.stars
            when {
                isFavorite -> R.drawable.ic_favorite_black_24dp
                else -> R.drawable.ic_favorite_border_black_24dp
            }.let(imageFav::setImageResource)

//            buttonFav.clicks()
//                    .takeUntil(itemView.detaches())
//                    .throttleFirst(400, TimeUnit.MILLISECONDS)
//                    .map { adapterPosition to drink }
//                    .subscribe(subject)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        subject.onComplete()
    }

    companion object {
        @JvmField
        val diffCallback: DiffUtil.ItemCallback<Drink> = object : DiffUtil.ItemCallback<Drink>() {
            override fun areItemsTheSame(oldItem: Drink, newItem: Drink) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Drink, newItem: Drink) = oldItem == newItem
        }
        @JvmField
        val decimalFormatPrice = DecimalFormat.getInstance()!!
        @JvmField
        val decimalFormatStarCount =
            DecimalFormat("###,###", DecimalFormatSymbols().apply { groupingSeparator = ' ' })
    }
}