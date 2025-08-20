package com.chatgpt.backend.controller.chat;

import com.chatgpt.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> payload) {
        String mensaje = payload.get("mensaje");
        String numero = payload.get("numero");
        String respuesta = chatService.obtenerRespuestaOpenAI(mensaje, numero);
        System.out.println("Respuesta: " + respuesta);
        return Map.of("respuesta", respuesta);
    }
}

