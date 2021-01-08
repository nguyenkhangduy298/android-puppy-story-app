package com.example.admin.puppyapp;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

public class DogImageViewHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;

    public DogImageViewHolder(View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.dog_imageView);
    }
}
