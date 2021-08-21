package com.example.android_google_drive_loader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private ArrayList<RecyclerViewItem> itemList;

    public RecyclerViewAdapter(ArrayList<RecyclerViewItem> itemList) {
        this.itemList = itemList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView recyclerViewItemName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerViewItemName = itemView.findViewById(R.id.recyclerViewItem);
        }
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_list_items, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        String recyclerViewItemName = itemList.get(position).getName();
        holder.recyclerViewItemName.setText(recyclerViewItemName);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
