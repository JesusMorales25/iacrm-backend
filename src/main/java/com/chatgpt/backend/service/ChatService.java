package com.chatgpt.backend.service;

import com.chatgpt.backend.enums.CategoriaContacto;
import com.chatgpt.backend.persistence.Conversacion;
import com.chatgpt.backend.persistence.DatosContacto;
import com.chatgpt.backend.repository.ConversacionRepository;
import com.chatgpt.backend.repository.DatosContactoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class ChatService {

    @Autowired
    ConversacionRepository conversacionRepository;

    @Autowired
    private DatosContactoRepository datosContactoRepository;

    @Autowired
    private FunctionExecutorService functionExecutorService;

    @Autowired
    private MessageLimitService messageLimitService;

    @Value("${openai.api.key}")
    private String API_KEY;

    @Value("${openai.assistant.id}")
    private String ASSISTANT_ID;
    
    @Value("${openai.model:gpt-4}")
    private String OPENAI_MODEL;
    
    @Value("${openai.max.tokens:2000}")
    private int OPENAI_MAX_TOKENS;
    
    @Value("${openai.temperature:0.7}")
    private double OPENAI_TEMPERATURE;

    @Value("${business.max.messages.per.user:100}")
    private int MAX_MESSAGES_PER_USER;
    
    @Value("${business.message.timeout:30}")
    private int MESSAGE_TIMEOUT_SECONDS;
    
    @Value("${business.default.user.category:CURIOSO}")
    private String DEFAULT_USER_CATEGORY;

    @Value("${business.thread.cleanup.interval:3600}")
    private int THREAD_CLEANUP_INTERVAL_SECONDS;

    @Value("https://api.openai.com/v1/threads")
    private String THREADS_URL;

    @Value("https://api.openai.com/v1/threads/{thread_id}/runs")
    private String RUN_URL_TEMPLATE;

    @Value("https://api.openai.com/v1/threads/{thread_id}/runs/{run_id}")
    private String RETRIEVE_RUN_URL_TEMPLATE;

    @Value("https://api.openai.com/v1/threads/{thread_id}/runs")
    private String LIST_RUNS_URL_TEMPLATE;

    @Value("https://api.openai.com/v1/threads/{thread_id}/messages")
    private String MESSAGES_URL_TEMPLATE;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> userThreadMap = new ConcurrentHashMap<>();
    
    // Cola de mensajes pendientes por usuario para mantener el orden
    private final Map<String, ConcurrentLinkedQueue<PendingMessage>> userMessageQueues = new ConcurrentHashMap<>();
    
    // Semáforos para controlar un hilo de procesamiento por usuario
    private final Map<String, Semaphore> userSemaphores = new ConcurrentHashMap<>();
    
    // Clase interna para manejar mensajes pendientes con timestamp
    private static class PendingMessage {
        final String mensaje;
        final long timestamp;
        final String id;
        
        PendingMessage(String mensaje) {
            this.mensaje = mensaje;
            this.timestamp = System.currentTimeMillis();
            this.id = System.currentTimeMillis() + "_" + mensaje.hashCode();
        }
    }

    public String obtenerRespuestaOpenAI(String mensaje, String numero) {
        // 1. VALIDACIÓN INTELIGENTE DE LÍMITES
        MessageLimitService.ValidationResult validation = messageLimitService.validateMessage(numero);
        
        if (!validation.isAllowed()) {
            // Si es bloqueo silencioso, simplemente ignorar el mensaje
            if ("SILENT_LIMIT_REACHED".equals(validation.getCode())) {
                return null; // No responder nada (transparente para el usuario)
            }
            // Si tiene mensaje, enviarlo (para otros tipos de planes)
            return validation.getMessage();
        }
        
        // Obtener o crear el semáforo para este usuario
        Semaphore userSemaphore = userSemaphores.computeIfAbsent(numero, k -> new Semaphore(1));
        
        try {
            // Intentar adquirir el semáforo de inmediato
            if (userSemaphore.tryAcquire()) {
                try {
                    // Verificar si hay mensajes en cola primero
                    ConcurrentLinkedQueue<PendingMessage> messageQueue = userMessageQueues.get(numero);
                    if (messageQueue != null && !messageQueue.isEmpty()) {
                        // Agregar el mensaje actual a la cola y procesar toda la cola
                        messageQueue.offer(new PendingMessage(mensaje));
                        return procesarColaCompleta(numero);
                    } else {
                        // Procesar inmediatamente si no hay cola
                        String respuesta = procesarMensajeInmediato(mensaje, numero);
                        
                        // Agregar warning si existe
                        if (validation.hasWarning()) {
                            respuesta = validation.getWarning() + "\n\n" + respuesta;
                        }
                        
                        return respuesta;
                    }
                } finally {
                    userSemaphore.release();
                }
            } else {
                // Si hay un mensaje siendo procesado, agregar a la cola
                ConcurrentLinkedQueue<PendingMessage> messageQueue = userMessageQueues.computeIfAbsent(numero, k -> new ConcurrentLinkedQueue<>());
                PendingMessage pendingMessage = new PendingMessage(mensaje);
                messageQueue.offer(pendingMessage);
                
                // Esperar a que sea nuestro turno y procesar
                return esperarYProcesarEnCola(numero, pendingMessage.id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error al procesar tu mensaje.";
        }
    }

    private String esperarYProcesarEnCola(String numero, String messageId) throws InterruptedException {
        Semaphore userSemaphore = userSemaphores.get(numero);
        
        // Esperar hasta 30 segundos para obtener el semáforo
        if (userSemaphore.tryAcquire(30, java.util.concurrent.TimeUnit.SECONDS)) {
            try {
                // Verificar si nuestro mensaje aún está en la cola (no fue procesado)
                ConcurrentLinkedQueue<PendingMessage> messageQueue = userMessageQueues.get(numero);
                if (messageQueue != null) {
                    boolean messageStillPending = messageQueue.stream()
                            .anyMatch(pm -> pm.id.equals(messageId));
                    
                    if (messageStillPending) {
                        return procesarColaCompleta(numero);
                    } else {
                        // El mensaje ya fue procesado como parte de un contexto anterior
                        return "Tu mensaje ya fue procesado en el contexto anterior.";
                    }
                }
                return "No hay mensajes para procesar.";
            } finally {
                userSemaphore.release();
            }
        } else {
            return "Lo siento, el sistema está ocupado. Por favor intenta de nuevo.";
        }
    }

    private String procesarColaCompleta(String numero) {
        ConcurrentLinkedQueue<PendingMessage> messageQueue = userMessageQueues.get(numero);
        if (messageQueue == null || messageQueue.isEmpty()) {
            return "No hay mensajes para procesar.";
        }

        // Recopilar todos los mensajes de la cola para procesarlos como contexto
        StringBuilder contextoCompleto = new StringBuilder();
        PendingMessage mensaje;
        int mensajesProcesados = 0;
        
        // Limitar a máximo 5 mensajes para evitar contextos demasiado largos
        while ((mensaje = messageQueue.poll()) != null && mensajesProcesados < 5) {
            if (contextoCompleto.length() > 0) {
                contextoCompleto.append("\n");
            }
            contextoCompleto.append(mensaje.mensaje);
            mensajesProcesados++;
        }

        if (contextoCompleto.length() > 0) {
            String respuesta = procesarMensajeInmediato(contextoCompleto.toString(), numero);
            
            // Limpiar cola si está vacía después del procesamiento
            if (messageQueue.isEmpty()) {
                userMessageQueues.remove(numero);
            }
            
            return respuesta;
        }
        
        return "No se encontraron mensajes válidos para procesar.";
    }

    private String procesarMensajeInmediato(String mensaje, String numero) {
        try {
            HttpHeaders headers = createHeaders();
            String threadId = userThreadMap.computeIfAbsent(numero, key -> {
                Map<String, Object> thread = post(THREADS_URL, headers, new HashMap<>());
                return (String) thread.get("id");
            });

            // Verifica si ya existe el contacto con ese número
            if (!datosContactoRepository.existsByNumeroUsuario(numero)) {
                // Si no existe, crea el contacto con categoría configurable
                datosContactoRepository.save(DatosContacto.builder()
                        .numeroUsuario(numero)
                        .threadId(threadId)
                        .categoria(obtenerCategoriaDefecto())
                        .build()
                );
            }

            // Verificar si hay un run activo antes de continuar (solo para seguridad adicional)
            if (hasActiveRun(threadId, headers)) {
                // Esperar un poco y reintentar una vez
                Thread.sleep(2000);
                if (hasActiveRun(threadId, headers)) {
                    return "El sistema está procesando. Por favor espera un momento...";
                }
            }

            // Preparar el mensaje con contexto mejorado si contiene múltiples partes
            String mensajeProcesado = mensaje;
            if (mensaje.contains("\n")) {
                mensajeProcesado = "Contexto de conversación:\n" + mensaje;
            }

            Map<String, Object> messageBody = Map.of(
                    "role", "user",
                    "content", mensajeProcesado
            );
            post("https://api.openai.com/v1/threads/" + threadId + "/messages", headers, messageBody);

            Map<String, Object> runBody = Map.of("assistant_id", ASSISTANT_ID);
            Map<String, Object> run = post(RUN_URL_TEMPLATE.replace("{thread_id}", threadId), headers, runBody);
            String runId = (String) run.get("id");

            boolean completed = false;
            int maxRetries = 60; // Reducido a 30 segundos para mayor fluidez
            int retryCount = 0;
            
            while (!completed && retryCount < maxRetries) {
                Thread.sleep(500);
                retryCount++;
                
                Map<String, Object> runStatus = get(RETRIEVE_RUN_URL_TEMPLATE
                        .replace("{thread_id}", threadId)
                        .replace("{run_id}", runId), headers);

                String status = (String) runStatus.get("status");

                if ("requires_action".equals(status)) {
                    functionExecutorService.procesarToolCalls(runStatus, threadId, runId, numero, mensajeProcesado, headers);
                } else if ("completed".equals(status)) {
                    completed = true;
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Run falló: " + runStatus.get("last_error"));
                } else if ("cancelled".equals(status)) {
                    throw new RuntimeException("Run fue cancelado");
                } else if ("expired".equals(status)) {
                    throw new RuntimeException("Run expiró");
                }
            }
            
            if (!completed) {
                throw new RuntimeException("Timeout: El procesamiento tardó demasiado tiempo");
            }

            Map<String, Object> messages = get(MESSAGES_URL_TEMPLATE.replace("{thread_id}", threadId), headers);
            List<Map<String, Object>> data = (List<Map<String, Object>>) messages.get("data");

            for (Map<String, Object> msg : data) {
                if ("assistant".equals(msg.get("role"))) {
                    Map<String, Object> messageContent = ((List<Map<String, Object>>) msg.get("content")).get(0);
                    String text = (String) ((Map<String, Object>) messageContent.get("text")).get("value");

                    // Guardar la conversación con el mensaje original (no el procesado)
                    Conversacion conversacion = Conversacion.builder()
                            .numero(numero)
                            .threadId(threadId)
                            .mensajeUsuario(mensaje.length() > 500 ? mensaje.substring(0, 500) + "..." : mensaje)
                            .respuestaBot(text)
                            .fechaHora(LocalDateTime.now())
                            .build();

                    conversacionRepository.save(conversacion);
                    return text;
                }
            }
            return "No se obtuvo respuesta del asistente.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error al procesar tu mensaje: " + e.getMessage();
        }
    }

    /**
     * Verifica si hay un run activo (in_progress, queued, requires_action) en el thread
     * para evitar race conditions con la OpenAI Assistant API
     */
    private boolean hasActiveRun(String threadId, HttpHeaders headers) {
        try {
            Map<String, Object> runsResponse = get(LIST_RUNS_URL_TEMPLATE.replace("{thread_id}", threadId), headers);
            List<Map<String, Object>> runs = (List<Map<String, Object>>) runsResponse.get("data");
            
            if (runs != null && !runs.isEmpty()) {
                for (Map<String, Object> run : runs) {
                    String status = (String) run.get("status");
                    // Verificar si hay runs en estados activos
                    if ("in_progress".equals(status) || 
                        "queued".equals(status) || 
                        "requires_action".equals(status)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            // En caso de error, asumimos que no hay runs activos para no bloquear
            System.err.println("Error verificando runs activos: " + e.getMessage());
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);
        headers.add("OpenAI-Beta", "assistants=v2");
        return headers;
    }

    private Map<String, Object> post(String url, HttpHeaders headers, Object body) {
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(url, request, Map.class);
    }

    private Map<String, Object> get(String url, HttpHeaders headers) {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, request, Map.class).getBody();
    }

    /**
     * Valida si el usuario ha alcanzado el límite máximo de mensajes
     */
    private boolean validarLimiteMensajes(String numero) {
        try {
            // Contar mensajes del usuario en la base de datos
            long totalMensajes = conversacionRepository.countByNumero(numero);
            return totalMensajes < MAX_MESSAGES_PER_USER;
        } catch (Exception e) {
            // En caso de error, permitir el mensaje (fail-safe)
            return true;
        }
    }

    /**
     * Usa la categoría por defecto configurada en lugar de hardcodeada
     */
    private CategoriaContacto obtenerCategoriaDefecto() {
        try {
            return CategoriaContacto.valueOf(DEFAULT_USER_CATEGORY);
        } catch (IllegalArgumentException e) {
            // Si la categoría configurada no es válida, usar CURIOSO por defecto
            return CategoriaContacto.CURIOSO;
        }
    }
}