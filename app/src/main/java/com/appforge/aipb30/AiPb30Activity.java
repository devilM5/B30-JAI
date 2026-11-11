package com.appforge.aipb30;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;;

public class AiPb30Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Game logic initialization
        gameScore = 0;
        level = 1;
        isPlaying = false;

        scoreLabel = findViewById(R.id.scoreLabel);
        levelLabel = findViewById(R.id.levelLabel);
        startBtn = findViewById(R.id.startBtn);
        gameArea = findViewById(R.id.gameArea);

        startBtn.setOnClickListener(v -> {
            isPlaying = true;
            gameScore = 0;
            level = 1;
            updateUI();
            startBtn.setVisibility(View.GONE);
            Toast.makeText(this, "Game Started!", Toast.LENGTH_SHORT).show();
        });

        gameArea.setOnTouchListener((v, event) -> {
            if (isPlaying && event.getAction() == MotionEvent.ACTION_DOWN) {
                gameScore += 10;
                if (gameScore % 100 == 0) level++;
                updateUI();
                return true;
            }
            return false;
        });
    }

    private int gameScore;
    private int level;
    private boolean isPlaying;
    private TextView scoreLabel, levelLabel;
    private Button startBtn;
    private View gameArea;

    private void updateUI() {
        scoreLabel.setText("Score: " + gameScore);
        levelLabel.setText("Level: " + level);
    }
}
