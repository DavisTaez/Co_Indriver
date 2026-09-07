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

    public enum EstadoVoz {
        REPOSO,
        ESPERANDO_ODO_INICIO,
        CONFIRMANDO_ODO_INICIO,
        ESPERANDO_VALOR,
        CONFIRMANDO_VALOR,
        EN_CURSO,
        ESPERANDO_ODO_FIN,
        CONFIRMANDO_ODO_FIN
    }

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
    private static long odoInicioTemp = 0;
    private static double valorTemp = 0.0;
    private long odoFinTemp = 0;

    public ManejadorVozCarrera(Context context, CallbackCarreraVoz callback) {
        this.context = context;
        this.callback = callback;
        this.tts = new TextToSpeech(context, this);
    }

    private void mostrarToast(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("es", "EC"));
            ttsListo = true;

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {}
                @Override public void onError(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    if ("ID_PREGUNTA".equals(utteranceId)) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            detenerTTS();
                            abrirMicrofonoGoogle();
                        }, 400);
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
            mostrarToast("Reintentando micrófono (" + estadoActual.name() + ")...");
            abrirMicrofonoGoogle();
        }
    }

    private void abrirMicrofonoGoogle() {
        new Handler(Looper.getMainLooper()).post(() -> {
            destruirSpeechRecognizer();

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                mostrarToast("❌ Servicio de voz no disponible");
                return;
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    mostrarToast("🎙️ ESCUCHANDO... ¡Habla ahora!");
                }

                @Override
                public void onBeginningOfSpeech() {
                    mostrarToast("🗣️ Captando voz...");
                }

                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {
                    mostrarToast("⏳ Procesando lo que dijiste...");
                }

                @Override
                public void onError(int error) {
                    String msj = obtenerMensajeError(error);
                    Log.e(TAG, "Error micrófono: " + msj);
                    mostrarToast("⚠️ " + msj);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String textoEscuchado = matches.get(0);
                        mostrarToast("💬 Escuchado: \"" + textoEscuchado + "\"");
                        procesarRespuesta(textoEscuchado);
                    } else {
                        mostrarToast("⚠️ No se captaron palabras.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        Log.d(TAG, "Parcial: " + matches.get(0));
                    }
                }

                @Override public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-EC");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);
        });
    }

    private void procesarRespuesta(String texto) {
        String textoLimpio = texto.toLowerCase(Locale.ROOT).trim();

        // 1. VALIDACIÓN GLOBAL DE CANCELACIÓN (Aplica en cualquier momento del flujo)
        if (textoLimpio.contains("cancelar") || textoLimpio.contains("me cancelaron") || textoLimpio.contains("cancelada")) {

            // Crear la carrera cancelada usando el odoInicioTemp actual y valor 0.0
            Carrera carreraCancelada = new Carrera(numeroCarreraActual, odoInicioTemp, odoInicioTemp, 0.0, true);

            if (callback != null) {
                callback.onCarreraFinalizada(carreraCancelada);
            }

            // Reiniciar variables temporales y estado del manejador
            numeroCarreraActual++;
            estadoActual = EstadoVoz.REPOSO;
            odoInicioTemp = 0;
            valorTemp = 0.0;
            odoFinTemp = 0;

            hablarSinEsperar("Carrera cancelada.");
            return; // Salimos para no ejecutar el switch
        }

        // 2. FLUJO NORMAL POR SWITCH
        switch (estadoActual) {
            // --- ODÓMETRO INICIAL ---
            case ESPERANDO_ODO_INICIO:
                long odoInicio = extraerNumeroLong(texto);
                if (odoInicio > 0) {
                    odoInicioTemp = odoInicio;
                    estadoActual = EstadoVoz.CONFIRMANDO_ODO_INICIO;
                    hablarYEsperar("Entendí odómetro " + odoInicioTemp + ". ¿Es correcto?", true);
                } else {
                    hablarYEsperar("No entendí el número. ¿Cuál es el odómetro inicial?", true);
                }
                break;

            case CONFIRMANDO_ODO_INICIO:
                if (esAfirmativo(textoLimpio)) {
                    estadoActual = EstadoVoz.ESPERANDO_VALOR;
                    hablarYEsperar("Odómetro guardado. ¿Cuál es el precio de la carrera?", true);
                } else if (esNegativo(textoLimpio)) {
                    odoInicioTemp = 0;
                    estadoActual = EstadoVoz.ESPERANDO_ODO_INICIO;
                    hablarYEsperar("Entendido. Repitamos. ¿Cuál es el odómetro inicial?", true);
                } else {
                    hablarYEsperar("Por favor responde correcto o incorrecto. ¿Es odómetro " + odoInicioTemp + "?", true);
                }
                break;

            // --- PRECIO DE LA CARRERA ---
            case ESPERANDO_VALOR:
                double valor = extraerNumeroDouble(texto);
                if (valor > 0.0) {
                    valorTemp = valor;
                    estadoActual = EstadoVoz.CONFIRMANDO_VALOR;
                    hablarYEsperar("Entendí " + String.format(Locale.US, "%.2f", valorTemp) + " dólares. ¿Es correcto?", true);
                } else {
                    hablarYEsperar("No entendí el precio. ¿Cuál es el precio de la carrera?", true);
                }
                break;

            case CONFIRMANDO_VALOR:
                if (esAfirmativo(textoLimpio)) {
                    estadoActual = EstadoVoz.EN_CURSO;
                    if (callback != null) {
                        callback.onCarreraIniciada(numeroCarreraActual, odoInicioTemp, valorTemp);
                    }
                    hablarSinEsperar("Carrera " + numeroCarreraActual + " iniciada con exito.");
                } else if (esNegativo(textoLimpio)) {
                    valorTemp = 0.0;
                    estadoActual = EstadoVoz.ESPERANDO_VALOR;
                    hablarYEsperar("Entendido. ¿Cuál es el precio de la carrera?", true);
                } else {
                    hablarYEsperar("Por favor responde correcto o incorrecto. ¿Es " + String.format(Locale.US, "%.2f", valorTemp) + " dólares?", true);
                }
                break;

            // --- ODÓMETRO FINAL ---
            case ESPERANDO_ODO_FIN:
                long odoFin = extraerNumeroLong(texto);
                if (odoFin >= odoInicioTemp) {
                    odoFinTemp = odoFin;
                    estadoActual = EstadoVoz.CONFIRMANDO_ODO_FIN;
                    hablarYEsperar("Entendí odómetro final " + odoFinTemp + ". ¿Es correcto?", true);
                } else {
                    hablarYEsperar("El odómetro final debe ser mayor a " + odoInicioTemp + ". ¿Cuál es el odómetro final?", true);
                }
                break;

            case CONFIRMANDO_ODO_FIN:
                if (esAfirmativo(textoLimpio)) {
                    Carrera carreraFinalizada = new Carrera(numeroCarreraActual, odoInicioTemp, odoFinTemp, valorTemp, true);

                    if (callback != null) {
                        callback.onCarreraFinalizada(carreraFinalizada);
                    }

                    hablarSinEsperar("Carrera " + numeroCarreraActual + " finalizada. Recorriste " +
                            carreraFinalizada.getKmRecorridos() + " kilómetros.");

                    numeroCarreraActual++;
                    estadoActual = EstadoVoz.REPOSO;
                    odoInicioTemp = 0;
                    valorTemp = 0.0;
                    odoFinTemp = 0;
                } else if (esNegativo(textoLimpio)) {
                    odoFinTemp = 0;
                    estadoActual = EstadoVoz.ESPERANDO_ODO_FIN;
                    hablarYEsperar("Entendido. ¿Cuál es el odómetro final?", true);
                } else {
                    hablarYEsperar("Por favor responde correcto o incorrecto. ¿Es odómetro final " + odoFinTemp + "?", true);
                }
                break;
        }
    }

    public static void setOdoInicioTemp(long odoInicio) {
        odoInicioTemp = odoInicio;
    }
    public static void setValorTemp(double valor) {
        valorTemp = valor;
    }
    private boolean esAfirmativo(String texto) {
        return texto.contains("correcto") || texto.contains("si") || texto.contains("sí") ||
                texto.contains("afirmativo") || texto.contains("esta bien") || texto.contains("está bien") ||
                texto.contains("simon") || texto.contains("confirmo");
    }

    private boolean esNegativo(String texto) {
        return texto.contains("incorrecto") || texto.contains("no") || texto.contains("negativo") ||
                texto.contains("error") || texto.contains("mal") || texto.contains("equivocado");
    }

    private void hablarYEsperar(String mensaje, boolean abrirMicAlTerminar) {
        if (callback != null) callback.onEstadoCambiado(estadoActual, mensaje);
        if (ttsListo && tts != null) {
            Bundle b = new Bundle();
            b.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, abrirMicAlTerminar ? "ID_PREGUNTA" : "ID_NOTIF");
            tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, b, abrirMicAlTerminar ? "ID_PREGUNTA" : "ID_NOTIF");
        }
    }

    private void hablarSinEsperar(String mensaje) {
        if (callback != null) callback.onEstadoCambiado(estadoActual, mensaje);
        if (ttsListo && tts != null) {
            tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void detenerTTS() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    private void destruirSpeechRecognizer() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    private long extraerNumeroLong(String texto) {
        String soloDigitos = texto.replaceAll("[^0-9]", "");
        if (!soloDigitos.isEmpty()) {
            try { return Long.parseLong(soloDigitos); } catch (Exception ignored) {}
        }
        return convertPalabrasANumero(texto);
    }

    private double extraerNumeroDouble(String texto) {
        String limpio = texto.toLowerCase(Locale.ROOT)
                .replace("con", ".")
                .replace("coma", ".")
                .replace("punto", ".")
                .replace("dólares", "")
                .replace("dolares", "");

        String soloDigitosYPunto = limpio.replaceAll("[^0-9.]", "");
        if (!soloDigitosYPunto.isEmpty()) {
            try { return Double.parseDouble(soloDigitosYPunto); } catch (Exception ignored) {}
        }
        return (double) convertPalabrasANumero(texto);
    }

    private long convertPalabrasANumero(String texto) {
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
                case "cien": case "ciento": temp += 100; break;
                case "mil":
                    total += (temp == 0 ? 1 : temp) * 1000;
                    temp = 0;
                    break;
            }
        }
        return total + temp;
    }

    private String obtenerMensajeError(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Error de Audio (3)";
            case SpeechRecognizer.ERROR_CLIENT: return "Error del Cliente (5)";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Sin Permisos de Micrófono (9)";
            case SpeechRecognizer.ERROR_NETWORK: return "Error de Red (2)";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Tiempo de red agotado (1)";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No se escuchó nada / Silencio (7)";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Tiempo de habla agotado (6)";
            case 8: return "Servicio ocupado (8)";
            default: return "Error (" + errorCode + ")";
        }
    }

    public void destruir() {
        destruirSpeechRecognizer();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}