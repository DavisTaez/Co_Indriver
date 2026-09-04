package com.example.controlindriver;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

public class FloatingWidgetService extends Service implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsPreparado = false;

    private SharedPreferences sharedPreferences;
    private int numeroCarrera = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("ControlDriverPrefs", MODE_PRIVATE);
        numeroCarrera = sharedPreferences.getInt("numero_carrera", 1);

        // Inicializar Sintetizador de Voz con Listener de finalización
        tts = new TextToSpeech(this, this);

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.addView(floatingView, params);
        }

        ImageView imgBola = floatingView.findViewById(R.id.imgBolaFlotante);

        imgBola.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private static final int CLICK_THRESHOLD = 10;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        int diffX = Math.abs((int) (event.getRawX() - initialTouchX));
                        int diffY = Math.abs((int) (event.getRawY() - initialTouchY));
                        if (diffX < CLICK_THRESHOLD && diffY < CLICK_THRESHOLD) {
                            solicitarComandoVoz();
                        }
                        return true;
                }
                return false;
            }
        });

        inicializarSpeechRecognizer();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("es", "ES"));
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsPreparado = true;

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if ("ID_ESCUCHA".equals(utteranceId)) {
                            new Handler(Looper.getMainLooper()).post(() -> abrirMicrofono());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {}
                });
            }
        }
    }

    private void hablar(String texto) {
        if (ttsPreparado && tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_LONG).show()
        );
    }

    private void solicitarComandoVoz() {
        if (ttsPreparado) {
            Bundle b = new Bundle();
            b.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ID_ESCUCHA");
            tts.speak("Escuchando", TextToSpeech.QUEUE_FLUSH, b, "ID_ESCUCHA");
        } else {
            abrirMicrofono();
        }
    }

    private void inicializarSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    hablar("No logré escucharte.");
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String frase = matches.get(0).toLowerCase(Locale.ROOT);
                        procesarComandoVoz(frase);
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void abrirMicrofono() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000);
            speechRecognizer.startListening(intent);
        }
    }

    private void procesarComandoVoz(String texto) {
        numeroCarrera = sharedPreferences.getInt("numero_carrera", 1);
        long numeroExtraido = extraerNumero(texto);

        boolean esInicio = texto.contains("inicio") || texto.contains("iniciar") || texto.contains("empezar");
        boolean esFin = texto.contains("termino") || texto.contains("terminar") || texto.contains("fin") || texto.contains("final");

        if (esInicio && numeroExtraido > 0) {
            float odoInicio = (float) numeroExtraido;
            sharedPreferences.edit()
                    .putFloat("odo_inicio_carrera", odoInicio)
                    .putBoolean("en_carrera", true)
                    .putString("ultimo_odometro", String.valueOf(numeroExtraido))
                    .apply();

            hablar("Registrando carrera " + numeroCarrera + " con " + numeroExtraido);

        } else if (esFin && numeroExtraido > 0) {
            boolean enCarrera = sharedPreferences.getBoolean("en_carrera", false);
            float odoInicioVal = sharedPreferences.getFloat("odo_inicio_carrera", 0f);

            if (enCarrera && numeroExtraido >= odoInicioVal) {
                long kmRecorridos = numeroExtraido - (long) odoInicioVal;

                sharedPreferences.edit()
                        .putBoolean("en_carrera", false)
                        .putString("ultimo_odometro", String.valueOf(numeroExtraido))
                        .putFloat("km_ultimo_viaje", (float) kmRecorridos)
                        .apply();

                // Enviar Broadcast a MainActivity
                Intent intentUpdate = new Intent("ACCION_ACTUALIZAR_CARRERAS");
                intentUpdate.putExtra("numero_carrera", numeroCarrera);
                intentUpdate.putExtra("odo_inicio", (long) odoInicioVal);
                intentUpdate.putExtra("odo_fin", numeroExtraido);
                intentUpdate.putExtra("valor", 0.0);
                sendBroadcast(intentUpdate);

                hablar("Final de viaje registrado para la carrera " + numeroCarrera +
                        ". Recorriste " + kmRecorridos + " kilómetros.");

                numeroCarrera++;
                sharedPreferences.edit().putInt("numero_carrera", numeroCarrera).apply();
            } else {
                hablar("Carrera finalizada con odómetro " + numeroExtraido);
            }

        } else if (numeroExtraido > 0) {
            sharedPreferences.edit().putString("ultimo_odometro", String.valueOf(numeroExtraido)).apply();
            hablar("Odómetro registrado con " + numeroExtraido);
        } else {
            hablar("No logré entender el odómetro.");
        }
    }

    private long extraerNumero(String texto) {
        String soloDigitos = texto.replaceAll("[^0-9]", "");
        if (!soloDigitos.isEmpty()) {
            try { return Long.parseLong(soloDigitos); } catch (Exception ignored) {}
        }

        long total = 0, temp = 0;
        String[] palabras = texto.split("\\s+");

        for (String p : palabras) {
            switch (p) {
                case "un": case "uno": temp += 1; break;
                case "dos": temp += 2; break;
                case "tres": temp += 3; break;
                case "cuatro": temp += 4; break;
                case "cinco": temp += 5; break;
                case "seis": temp += 6; break;
                case "siete": temp += 7; break;
                case "ocho": temp += 8; break;
                case "nueve": temp += 9; break;
                case "diez": temp += 10; break;
                case "veinte": temp += 20; break;
                case "treinta": temp += 30; break;
                case "cuarenta": temp += 40; break;
                case "cincuenta": temp += 50; break;
                case "sesenta": temp += 60; break;
                case "setenta": temp += 70; break;
                case "ochenta": temp += 80; break;
                case "noventa": temp += 90; break;
                case "cien": case "ciento": temp += 100; break;
                case "doscientos": temp += 200; break;
                case "trescientos": temp += 300; break;
                case "cuatrocientos": temp += 400; break;
                case "quinientos": temp += 500; break;
                case "seiscientos": temp += 600; break;
                case "setecientos": temp += 700; break;
                case "ochocientos": temp += 800; break;
                case "novecientos": temp += 900; break;
                case "mil":
                    total += (temp == 0 ? 1 : temp) * 1000;
                    temp = 0;
                    break;
            }
        }
        return total + temp;
    }

    @Override
    public void onDestroy() {
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}