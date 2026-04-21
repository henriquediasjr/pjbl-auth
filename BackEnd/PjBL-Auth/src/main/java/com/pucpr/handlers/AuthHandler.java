package com.pucpr.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Classe responsável por gerenciar as requisições de Autenticação.
 */
public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/auth/login
     * Valida credenciais e retorna um JWT.
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            JsonNode body = mapper.readTree(exchange.getRequestBody());
            String email = body.get("email").asText();
            // aceita "senha" (padrão interno) ou "password" (frontend)
            String senha = body.has("senha") ? body.get("senha").asText()
                                             : body.get("password").asText();

            Optional<Usuario> usuarioOpt = repository.findByEmail(email);

            // Mensagem genérica: não revelar se é e-mail ou senha que está errado
            if (!usuarioOpt.isPresent() || !BCrypt.checkpw(senha, usuarioOpt.get().getSenhaHash())) {
                sendResponse(exchange, 401, "{\"erro\": \"E-mail ou senha inválidos.\"}");
                return;
            }

            String token = jwtService.generateToken(usuarioOpt.get());
            sendResponse(exchange, 200, "{\"token\": \"" + token + "\"}");

        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"erro\": \"Requisição inválida.\"}");
        }
    }

    /**
     * POST /api/auth/register
     * Cadastra novo usuário com senha hasheada via BCrypt.
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            JsonNode body = mapper.readTree(exchange.getRequestBody());
            // aceita "nome" (padrão interno) ou "name" (frontend)
            String nome  = body.has("nome") ? body.get("nome").asText()
                                            : body.get("name").asText();
            String email = body.get("email").asText();
            // aceita "senha" (padrão interno) ou "password" (frontend)
            String senha = body.has("senha") ? body.get("senha").asText()
                                             : body.get("password").asText();
            String role  = body.has("role") ? body.get("role").asText() : "PACIENTE";

            // Nunca armazenar senha em texto puro
            String senhaHash = BCrypt.hashpw(senha, BCrypt.gensalt(12));

            Usuario novoUsuario = new Usuario(nome, email, senhaHash, role);
            repository.save(novoUsuario);

            sendResponse(exchange, 201, "{\"mensagem\": \"Usuário cadastrado com sucesso.\"}");

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"erro\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"erro\": \"Requisição inválida.\"}");
        }
    }

    /**
     * Qualquer método em /api/protected
     * Exige header: Authorization: Bearer TOKEN
     */
    public void handleProtected(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "{\"erro\": \"Token não fornecido.\"}");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            sendResponse(exchange, 401, "{\"erro\": \"Token inválido ou expirado.\"}");
            return;
        }

        String email = jwtService.extractEmail(token);
        sendResponse(exchange, 200, "{\"mensagem\": \"Acesso autorizado.\", \"email\": \"" + email + "\"}");
    }

    /**
     * POST /api/auth/logout
     * JWT é stateless — logout é feito no cliente apagando o token.
     * Este endpoint existe para compatibilidade com o frontend.
     */
    public void handleLogout(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendResponse(exchange, 200, "{\"mensagem\": \"Logout realizado.\"}");
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}