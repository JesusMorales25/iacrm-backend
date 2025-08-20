package com.chatgpt.backend.service;

import com.chatgpt.backend.enums.CategoriaContacto;
import com.chatgpt.backend.persistence.Conversacion;
import com.chatgpt.backend.persistence.DatosContacto;
import com.chatgpt.backend.repository.ConversacionRepository;
import com.chatgpt.backend.repository.DatosContactoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
    public class FunctionExecutorService {

    private static final Logger log = LoggerFactory.getLogger(FunctionExecutorService.class);

        @Value("${openai.api.key}")
        private String openaiApiKey;

        @Value("${make.webhookUrl}")
        private String WEBHOOKURL_MAKE;

        @Value("${make.webhookUrlGuardarContacto}")
        private String WEBHOOKURL_GUARDAR_CONTACTO;

        @Autowired
        private DatosContactoRepository datosContactoRepository;

        @Autowired
        private ConversacionRepository conversacionRepository;

        public void procesarToolCalls(Map<String, Object> runStatus, String threadId, String runId, String numero, String mensaje, HttpHeaders headers) throws Exception {
            Map<String, Object> requiredAction = (Map<String, Object>) runStatus.get("required_action");
            if (requiredAction == null) {
                log.info("No hay required_action para procesar.");
                return;
            }

            Map<String, Object> submitToolOutputs = (Map<String, Object>) requiredAction.get("submit_tool_outputs");
            if (submitToolOutputs == null || submitToolOutputs.get("tool_calls") == null) {
                log.info("No hay tool_calls para procesar.");
                return;
            }

            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) submitToolOutputs.get("tool_calls");
            List<Map<String, Object>> outputs = new java.util.ArrayList<>();

            for (Map<String, Object> toolCall : toolCalls) {
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName = (String) function.get("name");

                String resultado;

                try {
                    switch (functionName) {
                        case "guardar_datos_contacto":
                            procesarGuardarDatosContacto(function, numero, mensaje, threadId);
                            asignarCategoria(threadId, CategoriaContacto.REGISTRO);
                            resultado = "Datos de contacto guardados correctamente";
                            break;

                        case "agendar_cita":
                            procesarAgendarCitaViaWebhook(function, numero);
                            resultado = "Cita agendada correctamente en Google Calendar";
                            break;

                        case "solicitar_agente":
                            asignarCategoria(threadId, CategoriaContacto.PROSPECTO);
                            resultado = "Usuario marcado como PROSPECTO (solicitó hablar con agente)";
                            break;


                        default:
                            resultado = "Función no soportada: " + functionName;
                            break;
                    }
                } catch (Exception e) {
                    resultado = "Error al ejecutar función: " + functionName + ". Detalles: " + e.getMessage();
                    e.printStackTrace();
                }

                Map<String, Object> toolOutput = new HashMap<>();
                toolOutput.put("tool_call_id", toolCall.get("id").toString());
                toolOutput.put("output", resultado);
                outputs.add(toolOutput);
            }

            if (outputs.isEmpty()) {
                Optional<DatosContacto> optionalContacto = datosContactoRepository.findByThreadId(threadId);

                DatosContacto contacto = optionalContacto.orElseGet(() -> {
                    DatosContacto nuevo = DatosContacto.builder()
                            .numeroUsuario(numero)
                            .threadId(threadId)
                            .categoria(CategoriaContacto.CURIOSO)
                            .build();
                    datosContactoRepository.save(nuevo);
                    log.info("Se creó nuevo contacto con categoría CURIOSO: {}", numero);
                    return nuevo;
                });

            }



            enviarRespuestasToolCalls(outputs, threadId, runId);
        }

        /*private void procesarGuardarDatosContacto(Map<String, Object> function, String numero, String mensaje, String threadId) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> args = mapper.readValue((String) function.get("arguments"), Map.class);

            String nombre = args.get("nombre");
            String correo = args.get("correo");
            String telefono = args.get("telefono");

            DatosContacto contacto = DatosContacto.builder()
                    .nombre(nombre)
                    .correo(correo)
                    .telefono(telefono)
                    .numeroUsuario(numero)
                    .threadId(threadId) // ✅ Asociamos el threadId a este contacto
                    .categoria("PROSPECTO")
                    .build();


            datosContactoRepository.save(contacto);
            llamarWebhookGuardarContacto(WEBHOOKURL_GUARDAR_CONTACTO, nombre, correo, telefono, numero);
        }*/

    private void procesarGuardarDatosContacto(Map<String, Object> function, String numero, String mensaje, String threadId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> args = mapper.readValue((String) function.get("arguments"), Map.class);

        String nombre = args.get("nombre");
        String correo = args.get("correo");
        String telefono = args.get("telefono");

        DatosContacto contacto = datosContactoRepository.findByThreadId(threadId)
                .orElseGet(() -> DatosContacto.builder()
                        .threadId(threadId)
                        .numeroUsuario(numero)
                        .categoria(CategoriaContacto.CURIOSO) // estado inicial
                        .build()
                );

        boolean actualizo = false;

        // Función para actualizar si el campo está vacío
        actualizo |= actualizarSiVacio(contacto::getNombre, contacto::setNombre, nombre);
        actualizo |= actualizarSiVacio(contacto::getCorreo, contacto::setCorreo, correo);
        actualizo |= actualizarSiVacio(contacto::getTelefono, contacto::setTelefono, telefono);

        // Lógica de categorías
        if (mensaje.toLowerCase().contains("quiero hablar con un asesor") ||
                mensaje.toLowerCase().contains("contactar asesor")) {
            if (contacto.getCategoria() != CategoriaContacto.PROSPECTO) {
                contacto.setCategoria(CategoriaContacto.PROSPECTO);
                actualizo = true;
                log.info("Contacto {} actualizado a PROSPECTO", numero);
            }
        } else if (contacto.getNombre() != null && contacto.getCorreo() != null && contacto.getTelefono() != null) {
            if (contacto.getCategoria() != CategoriaContacto.REGISTRO) {
                contacto.setCategoria(CategoriaContacto.REGISTRO);
                actualizo = true;
                log.info("Contacto {} actualizado a REGISTRO", numero);
            }
        } else if (contacto.getCategoria() == null) {
            contacto.setCategoria(CategoriaContacto.CURIOSO);
            actualizo = true;
        }

        // Guardar solo si hubo cambios
        if (actualizo || contacto.getId() == null) {
            datosContactoRepository.save(contacto);
            llamarWebhookGuardarContacto(WEBHOOKURL_GUARDAR_CONTACTO,
                    contacto.getNombre(), contacto.getCorreo(), contacto.getTelefono(), numero);
        }
    }

    /**
     * Actualiza un campo si está vacío y el nuevo valor es válido.
     */
    private boolean actualizarSiVacio(Supplier<String> getter, Consumer<String> setter, String nuevoValor) {
        if (nuevoValor != null && !nuevoValor.isBlank() &&
                (getter.get() == null || getter.get().isBlank())) {
            setter.accept(nuevoValor);
            return true;
        }
        return false;
    }



    private void enviarRespuestasToolCalls(List<Map<String, Object>> outputs, String threadId, String runId) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("tool_outputs", outputs);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(openaiApiKey);
                headers.add("OpenAI-Beta", "assistants=v2");

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                String url = "https://api.openai.com/v1/threads/" + threadId + "/runs/" + runId + "/submit_tool_outputs";
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                log.info("Respuestas enviadas a OpenAI correctamente.");

            } catch (Exception e) {
                System.err.println("Error al enviar respuestas de tool_calls: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private String interpretarFechaNatural(String fechaTexto) {
            fechaTexto = fechaTexto.toLowerCase().trim();
            fechaTexto = fechaTexto.replaceAll("\\b(el|la|los|las|un|una|unos|unas)\\b", "").trim();

            LocalDate hoy = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Map<String, java.time.DayOfWeek> diasSemana = Map.of(
                    "lunes", java.time.DayOfWeek.MONDAY,
                    "martes", java.time.DayOfWeek.TUESDAY,
                    "miércoles", java.time.DayOfWeek.WEDNESDAY,
                    "miercoles", java.time.DayOfWeek.WEDNESDAY,
                    "jueves", java.time.DayOfWeek.THURSDAY,
                    "viernes", java.time.DayOfWeek.FRIDAY,
                    "sábado", java.time.DayOfWeek.SATURDAY,
                    "sabado", java.time.DayOfWeek.SATURDAY,
                    "domingo", java.time.DayOfWeek.SUNDAY
            );

            if (fechaTexto.contains("hoy")) {
                return hoy.format(formatter);
            } else if (fechaTexto.contains("mañana")) {
                return hoy.plusDays(1).format(formatter);
            } else if (fechaTexto.contains("pasado mañana")) {
                return hoy.plusDays(2).format(formatter);
            } else {
                for (Map.Entry<String, java.time.DayOfWeek> entry : diasSemana.entrySet()) {
                    if (fechaTexto.contains(entry.getKey())) {
                        java.time.DayOfWeek diaDeseado = entry.getValue();
                        int diasParaAgregar = (7 + diaDeseado.getValue() - hoy.getDayOfWeek().getValue()) % 7;
                        if (diasParaAgregar == 0) {
                            diasParaAgregar = 7;
                        }
                        LocalDate proximoDia = hoy.plusDays(diasParaAgregar);
                        return proximoDia.format(formatter);
                    }
                }

                try {
                    String[] partes = fechaTexto.replace("de", "").trim().split(" ");
                    int dia = Integer.parseInt(partes[0]);
                    String mesTexto = partes[1];

                    Map<String, Integer> meses = Map.ofEntries(
                            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3),
                            Map.entry("abril", 4), Map.entry("mayo", 5), Map.entry("junio", 6),
                            Map.entry("julio", 7), Map.entry("agosto", 8), Map.entry("septiembre", 9),
                            Map.entry("octubre", 10), Map.entry("noviembre", 11), Map.entry("diciembre", 12)
                    );

                    Integer mes = meses.get(mesTexto);
                    if (mes != null) {
                        int anio = hoy.getYear();
                        LocalDate fecha = LocalDate.of(anio, mes, dia);
                        if (fecha.isBefore(hoy)) {
                            fecha = fecha.plusYears(1);
                        }
                        return fecha.format(formatter);
                    }
                } catch (Exception e) {
                    System.err.println("No se pudo interpretar la fecha: " + fechaTexto);
                }
            }
            return hoy.format(formatter);
        }


        private void llamarWebhookCitaAgendada(String url, String nombre, String correo, String telefono, String fecha, String hora, String detalleServicio) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                Map<String, String> payload = new HashMap<>();
                payload.put("nombre", nombre);
                payload.put("correo", correo);
                payload.put("telefono", telefono);
                payload.put("fecha", fecha);
                payload.put("hora", hora);
                payload.put("detalleServicio",detalleServicio);


                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                log.info("Hook ejecutado con éxito. Respuesta: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error al ejecutar hook externo: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void procesarAgendarCitaViaWebhook(Map<String, Object> function, String numero) throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> args = mapper.readValue((String) function.get("arguments"), Map.class);

            String nombre = args.get("nombre");
            String correo = args.get("correo");
            String telefono = args.get("telefono");
            String fechaTexto = args.get("fecha");
            String hora = args.get("hora");
            String fecha = interpretarFechaNatural(fechaTexto);
            String detalleServicio = args.get("detalleServicio");

            String webhookUrl = WEBHOOKURL_MAKE;
            llamarWebhookCitaAgendada(webhookUrl, nombre, correo, telefono, fecha, hora, detalleServicio);
        }
        private void llamarWebhookGuardarContacto(String url, String nombre, String correo, String telefono, String numeroUsuario) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                Map<String, String> payload = new HashMap<>();
                payload.put("nombre", nombre);
                payload.put("correo", correo);
                payload.put("telefono", telefono);
                payload.put("numeroUsuario", numeroUsuario);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                log.info("Webhook de guardar contacto ejecutado. Respuesta: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error al ejecutar webhook de contacto: " + e.getMessage());
                e.printStackTrace();
            }
        }

    public void asignarCategoria(String threadId, CategoriaContacto nuevaCategoria) {
        Optional<DatosContacto> optionalContacto = datosContactoRepository.findByThreadId(threadId);

        if (optionalContacto.isPresent()) {
            DatosContacto contacto = optionalContacto.get();
            if (!nuevaCategoria.equals(contacto.getCategoria())) {
                contacto.setCategoria(nuevaCategoria);
                datosContactoRepository.save(contacto);
                log.info("Categoría actualizada a '{}' para el contacto: {}", nuevaCategoria, contacto.getNumeroUsuario());
            } else {
                log.info("La categoría ya era '{}', no se actualizó", nuevaCategoria);
            }
        } else {
            log.warn("No se encontró contacto para el thread_id: {}", threadId);
        }
    }




}