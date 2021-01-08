package com.example.admin.puppyapp;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class BottomSheetToAddImage extends BottomSheetDialogFragment {
    private ActionListener mActionListener;
    private ArrayList<Button> buttons = new ArrayList<>();
    public static ArrayList<Integer> buttonIds = new ArrayList<>();
    private ImageView pickedImage;

    public BottomSheetToAddImage() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_layout, container, false);
        pickedImage = v.findViewById(R.id.image_picked);

        Button chooseImage = v.findViewById(R.id.choose_image);
        buttons.add(0, chooseImage);
        buttonIds.add(0, R.id.choose_image);

        Button uploadImage = v.findViewById(R.id.upload_image);
        buttons.add(1, uploadImage);
        buttonIds.add(1, R.id.upload_image);

        for(int i = 0; i < 2; i++){
            final int finalI = i;
            buttons.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mActionListener != null){
                        mActionListener.onButtonClick(buttonIds.get(finalI));
                    }
                }
            });
        }

        return v;
    }

    public void getPickedImage(Uri fileUri){
        Picasso.get().load(fileUri).into(pickedImage);
    }

    public void setActionListener(ActionListener actionListener) {
        this.mActionListener = actionListener;
    }

    interface ActionListener {
        void onButtonClick(int id);
    }
}
