package com.linx64.propickersample;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.linx64.propicker.pro_image_picker.ProPicker;

public class JavaMainActivityExample extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        findViewById(R.id.btnChooser).setOnClickListener(v -> ProPicker.with(this).start((integer, intent) -> {

            ImageView iv = findViewById(R.id.iv);
            iv.setImageURI(ProPicker.getPickerData(intent).getUri());
            return null;
        }));

    }

}