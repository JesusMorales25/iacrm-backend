package com.chatgpt.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Validador de variables de entorno críticas
 * Evita que la aplicación inicie con configuración insegura
 */
@Component
public class EnvironmentValidator {

    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${jwt.secret:}")
    private String jwtSecret;
    
    @Value("${bot.api.key:}")
    private String botApiKey;
    
    @Value("${spring.datasource.username:}")
    private String dbUser;
    
    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validateEnvironment() {
        StringBuilder missingVars = new StringBuilder();
        
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            missingVars.append("OPENAI_API_KEY, ");
        }
        
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            missingVars.append("JWT_SECRET, ");
        }
        
        if (botApiKey == null || botApiKey.trim().isEmpty()) {
            missingVars.append("BOT_API_KEY, ");
        }
        
        if (dbUser == null || dbUser.trim().isEmpty()) {
            missingVars.append("DB_USER, ");
        }
        
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            missingVars.append("DB_PASSWORD, ");
        }
        
        if (missingVars.length() > 0) {
            String missing = missingVars.toString();
            missing = missing.substring(0, missing.length() - 2); // Remover última coma
            
            String errorMessage = String.format(
                "🚨 CONFIGURACIÓN INSEGURA DETECTADA 🚨\n" +
                "Las siguientes variables de entorno son OBLIGATORIAS: %s\n" +
                "Por seguridad, la aplicación NO puede iniciar sin estas variables.\n" +
                "Configure las variables de entorno antes de ejecutar la aplicación.",
                missing
            );
            
            System.err.println(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        System.out.println("✅ Todas las variables de entorno críticas están configuradas correctamente.");
    }
}