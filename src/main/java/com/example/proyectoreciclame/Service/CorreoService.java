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

    public void enviarConfirmacionRegistro(String destino, String nombres, String rolSolicitado) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destino);
        message.setSubject("Recíclame - Registro recibido");
        message.setText(
                "Hola " + obtenerNombreSeguro(nombres) + ".\n\n" +
                        "Hemos recibido tu solicitud de registro en Recíclame como " + rolSolicitado + ".\n\n" +
                        "Tu cuenta se encuentra pendiente de revisión por un administrador. " +
                        "Te enviaremos un correo cuando tu solicitud sea aprobada o denegada.\n\n" +
                        "Gracias por registrarte.\n\n" +
                        "Equipo Recíclame"
        );
        mailSender.send(message);
    }

    public void enviarRegistroAprobado(String destino, String nombres) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destino);
        message.setSubject("Recíclame - Solicitud aprobada");
        message.setText(
                "Hola " + obtenerNombreSeguro(nombres) + ".\n\n" +
                        "Tu solicitud de registro en Recíclame ha sido aprobada.\n\n" +
                        "Ya puedes iniciar sesión con el correo y contraseña que registraste.\n\n" +
                        "Ingresa a la plataforma desde:\n" +
                        "http://localhost:8080/login\n\n" +
                        "Equipo Recíclame"
        );
        mailSender.send(message);
    }

    public void enviarRegistroDenegado(String destino, String nombres, String motivo) {
        String motivoFinal = (motivo != null && !motivo.isBlank())
                ? motivo
                : "No se especificó un motivo.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destino);
        message.setSubject("Recíclame - Solicitud denegada");
        message.setText(
                "Hola " + obtenerNombreSeguro(nombres) + ".\n\n" +
                        "Tu solicitud de registro en Recíclame ha sido denegada.\n\n" +
                        "Motivo:\n" +
                        motivoFinal + "\n\n" +
                        "Si consideras que se trata de un error, puedes comunicarte con el equipo de soporte.\n\n" +
                        "Equipo Recíclame"
        );
        mailSender.send(message);
    }

    private String obtenerNombreSeguro(String nombres) {
        return (nombres != null && !nombres.isBlank()) ? nombres : "usuario";
    }
}