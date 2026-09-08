package com.example.controlindriver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class ManejadorVozCarrera implements TextToSpeech.OnInitListener {
    private static final String TAG = "CARRERA_VOZ";
    public enum EstadoVoz { REPOSO, ESPERANDO_ODO_INICIO, CONFIRMANDO_ODO_INICIO, ESPERANDO_VALOR, CONFIRMANDO_VALOR, EN_CURSO, ESPERANDO_ODO_FIN, CONFIRMANDO_ODO_FIN }
    public interface CallbackCarreraVoz {
        void onCarreraIniciada(int numero, long odoInicio, double valor);
        void onCarreraFinalizada(Carrera carrera);
        void onEstadoCambiado(EstadoVoz estadoActual, String mensaje);
    }

    private final Context context;
    private final CallbackCarreraVoz callback;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsListo = false;
    private EstadoVoz estadoActual = EstadoVoz.REPOSO;
    private int numeroCarreraActual = 1;
    private long odoInicioTemp = 0;
    private double valorTemp = 0.0;
    private long odoFinTemp = 0;

    public ManejadorVozCarrera(Context context, CallbackCarreraVoz callback) {
        this.context = context;
        this.callback = callback;
        this.tts = new TextToSpeech(context, this);
    }

    public void setNumeroCarreraActual(int numero) { this.numeroCarreraActual = numero; }

    public void actualizarDatosEnCurso(long nuevoOdoInicio, double nuevoValor) {
        if (estadoActual == EstadoVoz.EN_CURSO) {
            this.odoInicioTemp = nuevoOdoInicio;
            this.valorTemp = nuevoValor;
        }
    }

    private void mostrarToast(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show());
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("es", "EC"));
            ttsListo = true;
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {}
                @Override public void onError(String utteranceId) {}
                @Override public void onDone(String utteranceId) {
                    if ("ID_PREGUNTA".equals(utteranceId)) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> { detenerTTS(); abrirMicrofonoGoogle(); }, 400);
                    }
                }
            });
        }
    }

    public void presionarBotonCarrera() {
        detenerTTS();
        if (estadoActual == EstadoVoz.REPOSO) {
            estadoActual = EstadoVoz.ESPERANDO_ODO_INICIO;
            hablarYEsperar("Iniciando carrera " + numeroCarreraActual + ". ¿Cuál es el odómetro inicial?", true);
        } else if (estadoActual == EstadoVoz.EN_CURSO) {
            estadoActual = EstadoVoz.ESPERANDO_ODO_FIN;
            hablarYEsperar("Finalizando carrera " + numeroCarreraActual + ". ¿Cuál es el odómetro final?", true);
        } else {
            abrirMicrofonoGoogle();
        }
    }

    private void abrirMicrofonoGoogle() {
        new Handler(Looper.getMainLooper()).post(() -> {
            destruirSpeechRecognizer();
            if (!SpeechRecognizer.isRecognitionAvailable(context)) { mostrarToast("Servicio de voz no disponible"); return; }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { mostrarToast("🎙 ESCUCHANDO..."); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { mostrarToast("⏳ Procesando..."); }
                @Override public void onError(int error) { mostrarToast("⚠ " + obtenerMensajeError(error)); }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches!= null &&!matches.isEmpty()) { procesarRespuesta(matches.get(0)); }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-EC");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizer.startListening(intent);
        });
    }

    private void procesarRespuesta(String texto) {
        String textoLimpio = texto.toLowerCase(Locale.ROOT).trim();
        if (textoLimpio.contains("cancelar") || textoLimpio.contains("me cancelaron")) {
            Carrera carreraCancelada = new Carrera(numeroCarreraActual, odoInicioTemp, odoInicioTemp, 0.0, true);
            if (callback!= null) callback.onCarreraFinalizada(carreraCancelada);
            numeroCarreraActual++; estadoActual = EstadoVoz.REPOSO; odoInicioTemp = 0; valorTemp = 0.0; odoFinTemp = 0;
            hablarSinEsperar("Carrera cancelada."); return;
        }
        switch (estadoActual) {
            case ESPERANDO_ODO_INICIO:
                long odoInicio = extraerNumeroLong(texto);
                if (odoInicio > 0) { odoInicioTemp = odoInicio; estadoActual = EstadoVoz.CONFIRMANDO_ODO_INICIO; hablarYEsperar("Entendí odómetro " + odoInicioTemp + ". ¿Es correcto?", true); }
                else { hablarYEsperar("No entendí. ¿Cuál es el odómetro inicial?", true); } break;
            case CONFIRMANDO_ODO_INICIO:
                if (esAfirmativo(textoLimpio)) { estadoActual = EstadoVoz.ESPERANDO_VALOR; hablarYEsperar("Guardado. ¿Cuál es el precio?", true); }
                else if (esNegativo(textoLimpio)) { odoInicioTemp = 0; estadoActual = EstadoVoz.ESPERANDO_ODO_INICIO; hablarYEsperar("Repitamos. ¿Odómetro inicial?", true); }
                else { hablarYEsperar("Responde correcto o incorrecto. ¿Odómetro " + odoInicioTemp + "?", true); } break;
            case ESPERANDO_VALOR:
                double valor = extraerNumeroDouble(texto);
                if (valor > 0) { valorTemp = valor; estadoActual = EstadoVoz.CONFIRMANDO_VALOR; hablarYEsperar("Entendí " + String.format(Locale.US, "%.2f", valorTemp) + " dólares. ¿Correcto?", true); }
                else { hablarYEsperar("No entendí el precio. ¿Cuál es?", true); } break;
            case CONFIRMANDO_VALOR:
                if (esAfirmativo(textoLimpio)) { estadoActual = EstadoVoz.EN_CURSO; if (callback!= null) callback.onCarreraIniciada(numeroCarreraActual, odoInicioTemp, valorTemp); hablarSinEsperar("Carrera " + numeroCarreraActual + " iniciada."); }
                else if (esNegativo(textoLimpio)) { valorTemp = 0.0; estadoActual = EstadoVoz.ESPERANDO_VALOR; hablarYEsperar("¿Cuál es el precio?", true); }
                else { hablarYEsperar("¿Es " + String.format(Locale.US, "%.2f", valorTemp) + " dólares?", true); } break;
            case ESPERANDO_ODO_FIN:
                long odoFin = extraerNumeroLong(texto);
                if (odoFin >= odoInicioTemp) { odoFinTemp = odoFin; estadoActual = EstadoVoz.CONFIRMANDO_ODO_FIN; hablarYEsperar("Odómetro final " + odoFinTemp + ". ¿Correcto?", true); }
                else { hablarYEsperar("Debe ser mayor a " + odoInicioTemp + ". ¿Final?", true); } break;
            case CONFIRMANDO_ODO_FIN:
                if (esAfirmativo(textoLimpio)) {
                    Carrera carreraFinalizada = new Carrera(numeroCarreraActual, odoInicioTemp, odoFinTemp, valorTemp, false);
                    if (callback!= null) callback.onCarreraFinalizada(carreraFinalizada);
                    hablarSinEsperar("Carrera finalizada. Recorriste " + carreraFinalizada.getKmRecorridos() + " km.");
                    numeroCarreraActual++; estadoActual = EstadoVoz.REPOSO; odoInicioTemp = 0; valorTemp = 0.0; odoFinTemp = 0;
                } else if (esNegativo(textoLimpio)) { odoFinTemp = 0; estadoActual = EstadoVoz.ESPERANDO_ODO_FIN; hablarYEsperar("¿Odómetro final?", true); }
                else { hablarYEsperar("¿Es odómetro final " + odoFinTemp + "?", true); } break;
        }
    }

    private boolean esAfirmativo(String texto) { return texto.contains("correcto") || texto.contains("si") || texto.contains("sí") || texto.contains("afirmativo"); }
    private boolean esNegativo(String texto) { return texto.contains("incorrecto") || texto.contains("no") || texto.contains("negativo"); }
    private void hablarYEsperar(String mensaje, boolean abrirMic) {
        if (callback!= null) callback.onEstadoCambiado(estadoActual, mensaje);
        if (ttsListo && tts!= null) { Bundle b = new Bundle(); b.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, abrirMic? "ID_PREGUNTA" : "ID_NOTIF"); tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, b, abrirMic? "ID_PREGUNTA" : "ID_NOTIF"); }
    }
    private void hablarSinEsperar(String mensaje) {
        if (callback!= null) callback.onEstadoCambiado(estadoActual, mensaje);
        if (ttsListo && tts!= null) tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, null);
    }
    private void detenerTTS() { if (tts!= null && tts.isSpeaking()) tts.stop(); }
    private void destruirSpeechRecognizer() { if (speechRecognizer!= null) { try { speechRecognizer.destroy(); } catch (Exception ignored) {} speechRecognizer = null; } }
    private long extraerNumeroLong(String texto) { String soloDigitos = texto.replaceAll("[^0-9]", ""); if (!soloDigitos.isEmpty()) { try { return Long.parseLong(soloDigitos); } catch (Exception ignored) {} } return 0; }
    private double extraerNumeroDouble(String texto) { String limpio = texto.toLowerCase().replace("con", ".").replace("coma", ".").replace("dólares", "").replace("dolares", "").replaceAll("[^0-9.]", ""); if (!limpio.isEmpty()) { try { return Double.parseDouble(limpio); } catch (Exception ignored) {} } return 0; }
    private String obtenerMensajeError(int errorCode) {
        switch (errorCode) {
            case 1: return "Tiempo de red agotado"; case 2: return "Error de Red"; case 7: return "No se escuchó nada"; default: return "Error " + errorCode;
        }
    }
    public void destruir() { destruirSpeechRecognizer(); if (tts!= null) { tts.stop(); tts.shutdown(); } }
}