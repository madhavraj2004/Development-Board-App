package com.example.whatapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private Context mContext;
    private List<ChatsModel> mData;
    private ChatsFragmentListener listener;

    public RecyclerViewAdapter(Context mContext, List<ChatsModel> mData, ChatsFragmentListener listener) {
        this.mContext = mContext;
        this.mData = mData != null ? mData : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the chat_list_item layout
        View view = LayoutInflater.from(mContext).inflate(R.layout.chat_list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ChatsModel chat = mData.get(position);
        holder.chatName.setText(chat.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.openChatFragment(chat.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    // Add a single chat safely
    public void addChat(ChatsModel chat) {
        mData.add(chat);
        notifyItemInserted(mData.size() - 1);
    }

    // Get all current chats
    public List<ChatsModel> getAllChats() {
        return new ArrayList<>(mData);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView chatName;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            chatName = itemView.findViewById(R.id.chatName);
        }
    }

    public interface ChatsFragmentListener {
        void openChatFragment(String name);
    }
}
