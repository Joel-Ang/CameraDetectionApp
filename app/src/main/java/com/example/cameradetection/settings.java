package com.example.cameradetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.net.URI;

public class settings extends AppCompatActivity {
    SeekBar sensitivity_bar;
    TextView sensitivity_value;
    EditText emailText;

    Button updateEmailButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sensitivity_bar = findViewById(R.id.sensitivity_bar);

        sensitivity_value = findViewById(R.id.sensitivity_value);

        emailText = findViewById(R.id.editTextEmailAddress);

        updateEmailButton = findViewById(R.id.update_button);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int sens = sharedPreferences.getInt("sensitivity_value", 50); // Default value is 50 if the key doesn't exist

        emailText.setText(sharedPreferences.getString("toEmail", ""));
        sensitivity_bar.setProgress(sens);
        sensitivity_value.setText(String.valueOf(sensitivity_bar.getProgress()));


        sensitivity_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sensitivity_bar, int progress, boolean fromUser) {
                // Called when the progress level has changed.
                sensitivity_value.setText(String.valueOf(progress));

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("sensitivity_value", progress);
                editor.apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Called when the user starts dragging the thumb.
                sensitivity_value.setText(String.valueOf(sensitivity_bar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Called when the user stops dragging the thumb.
                int currentValue = seekBar.getProgress(); // Get the final value
                // Use currentValue as needed
            }
        });

        updateEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getText().toString().trim();
                // Check email validity or is empty
                if(email.isEmpty() || !email.matches("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")){
                    Toast.makeText(settings.this, "Invalid Email", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("toEmail", email);
                editor.apply();
            }
        });


    }

}