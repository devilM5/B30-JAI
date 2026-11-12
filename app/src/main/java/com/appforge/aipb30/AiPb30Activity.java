package com.appforge.aipb30;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Locale;

public class AiPb30Activity extends Activity {
    private static final int VOICE_REQUEST = 101;
    private static final int MIC_PERMISSION = 102;

    private TextView chatText;
    private EditText messageInput;
    private TextToSpeech tts;
    private GenerativeModelFutures geminiModel;
    private SharedPreferences memory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-3.8-flash");
        geminiModel = GenerativeModelFutures.from(ai);

        setContentView(R.layout.activity_main);

        chatText = findViewById(R.id.chatText);
        messageInput = findViewById(R.id.messageInput);
        Button sendBtn = findViewById(R.id.sendBtn);
        Button voiceBtn = findViewById(R.id.voiceBtn);
        Button historyBtn = findViewById(R.id.historyBtn);

        memory = getSharedPreferences("PB30_MEMORY", MODE_PRIVATE);
        chatText.setText(memory.getString("chat", "नमस्ते! मैं PB-30 JAI हूँ।"));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
            }
        });

        sendBtn.setOnClickListener(v -> sendMessage());
        voiceBtn.setOnClickListener(v -> startVoice());
        historyBtn.setOnClickListener(v ->
            Toast.makeText(this, "पुरानी बातचीत स्क्रीन पर सेव है।", Toast.LENGTH_SHORT).show()
        );
    }

    private void startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION
            );
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "अपनी बात बोलिए");
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Voice Recognition उपलब्ध नहीं है।",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoice();
        } else if (requestCode == MIC_PERMISSION) {
            Toast.makeText(this, "माइक्रोफोन की अनुमति जरूरी है।",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                messageInput.setText(results.get(0));
                sendMessage();
            }
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;

        addChat("तुम: " + message);
        messageInput.setText("");

        new AlertDialog.Builder(this)
                .setTitle("Google Gemini को सवाल भेजें?")
                .setMessage("क्या तुम इस सवाल को Google Gemini को भेजने की अनुमति देते हो? केवल यही सवाल भेजा जाएगा। पुरानी बातचीत, Memory या फाइलें नहीं भेजी जाएँगी।")
                .setPositiveButton("हाँ, भेजो", (dialog, which) -> callGemini(message))
                .setNegativeButton("नहीं", (dialog, which) -> {
                    String reply = "ठीक है। तुम्हारा सवाल Google को नहीं भेजा गया।";
                    addChat("PB-30 JAI: " + reply);
                    if (tts != null) {
                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "PB30_REPLY");
                    }
                })
                .show();
    }

    private void callGemini(String message) {
        addChat("PB-30 JAI: Gemini से जवाब ला रहा हूँ...");

        Content prompt = new Content.Builder()
                .addText(message)
                .build();

        try {
            Futures.addCallback(
                    geminiModel.generateContent(prompt),
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            runOnUiThread(() -> {
                                String reply = result.getText();
                                if (reply == null || reply.trim().isEmpty()) {
                                    reply = "Gemini से खाली जवाब मिला।";
                                }
                                addChat("PB-30 JAI: " + reply);
                                if (tts != null) {
                                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "PB30_REPLY");
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            runOnUiThread(() -> {
                                String reply = "Gemini से जवाब नहीं मिला: " + error.getMessage();
                                addChat("PB-30 JAI: " + reply);
                            });
                        }
                    },
                    MoreExecutors.directExecutor()
            );
        } catch (Exception error) {
            addChat("PB-30 JAI: Gemini शुरू नहीं हो पाया: " + error.getMessage());
        }
    }

    private void addChat(String line) {
        String old = chatText.getText().toString();
        String updated = old + "\n\n" + line;
        chatText.setText(updated);
        memory.edit().putString("chat", updated).apply();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
