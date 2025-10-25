package com.chatgpt.backend.service;

import com.chatgpt.backend.repository.ConversacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para validaciones inteligentes de límites de mensajes
 * Implementa límites por tiempo en lugar de límites absolutos
 */
@Service
public class MessageLimitService {

    @Autowired
    private ConversacionRepository conversacionRepository;

    // Configuración de límites inteligentes
    @Value("${business.max.messages.per.hour:20}")
    private int MAX_MESSAGES_PER_HOUR;
    
    @Value("${business.max.messages.per.day:100}")
    private int MAX_MESSAGES_PER_DAY;
    
    @Value("${business.max.messages.per.minute:5}")
    private int MAX_MESSAGES_PER_MINUTE;
    
    @Value("${business.upgrade.threshold:80}")
    private int UPGRADE_THRESHOLD_PERCENT;
    
    @Value("${business.plan.type:FREE}")
    private String PLAN_TYPE;
    
    @Value("${business.silent.limits:false}")
    private boolean SILENT_LIMITS;

    // Cache para conteo rápido de mensajes por minuto
    private final ConcurrentHashMap<String, MessageCounter> userMessageCounters = new ConcurrentHashMap<>();

    /**
     * Valida si el usuario puede enviar un mensaje
     * @param numero Número del usuario
     * @return Resultado de la validación
     */
    public ValidationResult validateMessage(String numero) {
        // 1. Validar límite por minuto (anti-spam)
        if (!validatePerMinuteLimit(numero)) {
            if (SILENT_LIMITS) {
                // Log para monitoreo pero no notificar al usuario
                logLimitReached(numero, "RATE_LIMIT_EXCEEDED", "minuto");
                return ValidationResult.silentBlock();
            }
            return ValidationResult.blocked(
                "Demasiados mensajes muy rápido. Espera un momento antes de continuar.",
                "RATE_LIMIT_EXCEEDED"
            );
        }

        // 2. Validar límite por hora
        long messagesLastHour = countMessagesInLastHours(numero, 1);
        if (messagesLastHour >= MAX_MESSAGES_PER_HOUR) {
            if (SILENT_LIMITS) {
                logLimitReached(numero, "HOURLY_LIMIT_EXCEEDED", "hora");
                return ValidationResult.silentBlock();
            }
            return ValidationResult.blocked(
                "Has alcanzado el límite de " + MAX_MESSAGES_PER_HOUR + " mensajes por hora. Podrás continuar en unos minutos.",
                "HOURLY_LIMIT_EXCEEDED"
            );
        }

        // 3. Validar límite diario
        long messagesToday = countMessagesInLastHours(numero, 24);
        if (messagesToday >= MAX_MESSAGES_PER_DAY) {
            if (SILENT_LIMITS) {
                logLimitReached(numero, "DAILY_LIMIT_EXCEEDED", "día");
                return ValidationResult.silentBlock();
            }
            return getDailyLimitResponse(messagesToday);
        }

        // 4. Verificar si debe mostrar mensaje de upgrade (solo si no es silencioso)
        if (!SILENT_LIMITS && shouldShowUpgradeMessage(messagesToday)) {
            return ValidationResult.allowed().withWarning(
                "Has usado " + messagesToday + " de " + MAX_MESSAGES_PER_DAY + " mensajes diarios. " +
                "¿Necesitas más? Contáctanos para un plan premium."
            );
        }

        return ValidationResult.allowed();
    }

    /**
     * Valida límite por minuto (anti-spam)
     */
    private boolean validatePerMinuteLimit(String numero) {
        MessageCounter counter = userMessageCounters.computeIfAbsent(numero, k -> new MessageCounter());
        return counter.canSendMessage(MAX_MESSAGES_PER_MINUTE);
    }

    /**
     * Cuenta mensajes en las últimas X horas
     */
    private long countMessagesInLastHours(String numero, int hours) {
        // Implementar consulta eficiente a BD
        // Para ahora, usar método existente como fallback
        return conversacionRepository.countByNumero(numero);
    }

    /**
     * Respuesta personalizada para límite diario según plan
     */
    private ValidationResult getDailyLimitResponse(long messagesToday) {
        if ("FREE".equals(PLAN_TYPE)) {
            return ValidationResult.blocked(
                "¡Límite alcanzado! Has usado todos tus " + MAX_MESSAGES_PER_DAY + " mensajes gratuitos de hoy. " +
                "¿Necesitas más? Contáctanos para acceso ilimitado.",
                "DAILY_LIMIT_FREE_PLAN"
            );
        } else if ("PREMIUM".equals(PLAN_TYPE)) {
            return ValidationResult.blocked(
                "Has alcanzado tu límite diario de " + MAX_MESSAGES_PER_DAY + " mensajes. " +
                "El límite se restablecerá mañana a las 00:00.",
                "DAILY_LIMIT_PREMIUM_PLAN"
            );
        } else {
            // Enterprise - no debería llegar aquí
            return ValidationResult.allowed();
        }
    }

    /**
     * Determina si mostrar mensaje de upgrade
     */
    private boolean shouldShowUpgradeMessage(long messagesToday) {
        if (!"FREE".equals(PLAN_TYPE)) return false;
        
        double usagePercent = (double) messagesToday / MAX_MESSAGES_PER_DAY * 100;
        return usagePercent >= UPGRADE_THRESHOLD_PERCENT;
    }

    /**
     * Log cuando se alcanza un límite (para monitoreo)
     */
    private void logLimitReached(String numero, String limitType, String periodo) {
        // Log para el equipo de monitoreo/alertas
        System.out.println("LIMIT_REACHED: Usuario " + numero + " alcanzó límite " + limitType + " en periodo " + periodo);
        // Aquí podrías enviar a un sistema de monitoreo como Datadog, New Relic, etc.
    }

    /**
     * Clase para contar mensajes por minuto
     */
    private static class MessageCounter {
        private volatile long lastMinute = 0;
        private volatile int count = 0;

        synchronized boolean canSendMessage(int maxPerMinute) {
            long currentMinute = System.currentTimeMillis() / 60000; // Minutos desde epoch
            
            if (currentMinute != lastMinute) {
                // Nuevo minuto, resetear contador
                lastMinute = currentMinute;
                count = 1;
                return true;
            } else {
                // Mismo minuto, verificar límite
                if (count < maxPerMinute) {
                    count++;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Resultado de validación de mensaje
     */
    public static class ValidationResult {
        private final boolean allowed;
        private final String message;
        private final String code;
        private String warning;

        private ValidationResult(boolean allowed, String message, String code) {
            this.allowed = allowed;
            this.message = message;
            this.code = code;
        }

        public static ValidationResult allowed() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult blocked(String message, String code) {
            return new ValidationResult(false, message, code);
        }

        public static ValidationResult silentBlock() {
            // Bloquea pero sin mensaje al usuario (para WhatsApp empresarial)
            return new ValidationResult(false, null, "SILENT_LIMIT_REACHED");
        }

        public ValidationResult withWarning(String warning) {
            this.warning = warning;
            return this;
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public String getCode() { return code; }
        public String getWarning() { return warning; }
        public boolean hasWarning() { return warning != null; }
    }
}