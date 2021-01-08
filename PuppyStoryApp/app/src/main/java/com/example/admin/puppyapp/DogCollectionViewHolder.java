package com.example.admin.puppyapp;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mikhaellopez.circularimageview.CircularImageView;

public class DogCollectionViewHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView breed;
    public TextView description;
    public CircularImageView profileImage;

    public DogCollectionViewHolder(View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.dog_name);
        breed = itemView.findViewById(R.id.dog_breed);
        description = itemView.findViewById(R.id.description);
        profileImage = itemView.findViewById(R.id.header_image);
    }
}
