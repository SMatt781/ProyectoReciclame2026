package com.example.proyectoreciclame.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoRecuperacion(String destino, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destino);
        message.setSubject("Recíclame - Código de recuperación");
        message.setText(
                "Hola.\n\n" +
                        "Tu código de recuperación de contraseña es: " + codigo + "\n\n" +
                        "Este código vence en 10 minutos.\n" +
                        "Si no solicitaste este cambio, ignora este mensaje."
        );
        mailSender.send(message);
    }
}