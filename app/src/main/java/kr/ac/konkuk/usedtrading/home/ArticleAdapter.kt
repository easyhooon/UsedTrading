package kr.ac.konkuk.usedtrading.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kr.ac.konkuk.usedtrading.databinding.ItemArticleBinding
import java.text.SimpleDateFormat
import java.util.*

class ArticleAdapter(val onItemClicked: (ArticleModel) -> Unit): ListAdapter<ArticleModel, ArticleAdapter.ViewHolder>(diffUtil){
    inner class ViewHolder(private val binding: ItemArticleBinding): RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SimpleDateFormat")
        fun bind(articleModel: ArticleModel) {
            val format = SimpleDateFormat("MM월 dd일")
            val date = Date(articleModel.createdAt)
            //createAt으로 현재 시간을 long타입으로 받아왔는데 그것을 Date타입으로 바꾼다음에
            //simpleDateFormat을 통해 포매팅 완성
            binding.titleTextView.text = articleModel.title
            binding.dateTextView.text = format.format(date).toString()
            binding.contentTextView.text = articleModel.content

            //glide 사용
            if(articleModel.imageUrlList.isNotEmpty()){
                Glide.with(binding.thumbnailImageView)
                        // 첫번째 이미지만 보이도록
                    .load(articleModel.imageUrlList.first())
                    .into(binding.thumbnailImageView)
            }
            binding.root.setOnClickListener {
                onItemClicked(articleModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //context 는 parent 에 있다
        return ViewHolder(ItemArticleBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ArticleModel>() {
            override fun areItemsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
                return oldItem.createdAt == newItem.createdAt
            }

            override fun areContentsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

