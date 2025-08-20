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

@Service
public class ChatService {

    @Autowired
    ConversacionRepository conversacionRepository;

    @Autowired
    private DatosContactoRepository datosContactoRepository;

    @Autowired
    private FunctionExecutorService functionExecutorService;

    @Value("${openai.api.key}")
    private String API_KEY;

    @Value("${openai.assistant.id}")
    private String ASSISTANT_ID;

    @Value("https://api.openai.com/v1/threads")
    private String THREADS_URL;

    @Value("https://api.openai.com/v1/threads/{thread_id}/runs")
    private String RUN_URL_TEMPLATE;

    @Value("https://api.openai.com/v1/threads/{thread_id}/runs/{run_id}")
    private String RETRIEVE_RUN_URL_TEMPLATE;

    @Value("https://api.openai.com/v1/threads/{thread_id}/messages")
    private String MESSAGES_URL_TEMPLATE;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> userThreadMap = new ConcurrentHashMap<>();

    public String obtenerRespuestaOpenAI(String mensaje, String numero) {
        try {
            HttpHeaders headers = createHeaders();
            String threadId = userThreadMap.computeIfAbsent(numero, key -> {
                Map<String, Object> thread = post(THREADS_URL, headers, new HashMap<>());
                return (String) thread.get("id");
            });

            // Verifica si ya existe el contacto con ese número
            if (!datosContactoRepository.existsByNumeroUsuario(numero)) {
                // Si no existe, crea el contacto como CURIOSO con threadId
                datosContactoRepository.save(DatosContacto.builder()
                        .numeroUsuario(numero)
                        .threadId(threadId)
                        .categoria(CategoriaContacto.CURIOSO)
                        .build()
                );
            }


            Map<String, Object> messageBody = Map.of(
                    "role", "user",
                    "content", mensaje
            );
            post("https://api.openai.com/v1/threads/" + threadId + "/messages", headers, messageBody);

            Map<String, Object> runBody = Map.of("assistant_id", ASSISTANT_ID);
            Map<String, Object> run = post(RUN_URL_TEMPLATE.replace("{thread_id}", threadId), headers, runBody);
            String runId = (String) run.get("id");

            boolean completed = false;
            while (!completed) {
                Thread.sleep(500);
                Map<String, Object> runStatus = get(RETRIEVE_RUN_URL_TEMPLATE
                        .replace("{thread_id}", threadId)
                        .replace("{run_id}", runId), headers);

                String status = (String) runStatus.get("status");

                if ("requires_action".equals(status)) {
                    functionExecutorService.procesarToolCalls(runStatus, threadId, runId, numero, mensaje, headers);
                } else if ("completed".equals(status)) {
                    completed = true;
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Run falló");
                }
            }

            Map<String, Object> messages = get(MESSAGES_URL_TEMPLATE.replace("{thread_id}", threadId), headers);
            List<Map<String, Object>> data = (List<Map<String, Object>>) messages.get("data");

            for (Map<String, Object> msg : data) {
                if ("assistant".equals(msg.get("role"))) {
                    Map<String, Object> messageContent = ((List<Map<String, Object>>) msg.get("content")).get(0);
                    String text = (String) ((Map<String, Object>) messageContent.get("text")).get("value");

                    Conversacion conversacion = Conversacion.builder()
                            .numero(numero)
                            .threadId(threadId)
                            .mensajeUsuario(mensaje)
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
            return "Ocurrió un error al procesar tu mensaje.";
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
}