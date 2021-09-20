package com.example.android_google_drive_loader;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_google_drive_loader.Enums.DriveType;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private ArrayList<RecyclerViewItem> itemList;
    private Context context;

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
        context = parent.getContext();
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_list_items, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        RecyclerViewItem item = itemList.get(position);
        holder.recyclerViewItemName.setText(item.getName());
        if (item.getType() == DriveType.FOLDER) {
            holder.recyclerViewItemName.setGravity(Gravity.CENTER);
            holder.recyclerViewItemName.setTypeface(null, Typeface.BOLD);
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.main_text_color, typedValue, true);
            holder.recyclerViewItemName.setTextColor(typedValue.data);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
