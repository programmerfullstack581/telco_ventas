package com.telco.ventas.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVentaRequest {

    @NotBlank(message = "DNI cliente es obligatorio")
    @Pattern(regexp = "^(\\d{8}|\\d{11})$", message = "DNI debe tener 8 dígitos o RUC 11 dígitos")
    private String dniCliente;

    @NotBlank(message = "Nombre cliente es obligatorio")
    private String nombreCliente;

    @NotBlank(message = "Teléfono cliente es obligatorio")
    @Pattern(regexp = "^\\d{9}$", message = "Teléfono debe tener 9 dígitos")
    private String telefonoCliente;

    @NotBlank(message = "Dirección cliente es obligatorio")
    private String direccionCliente;

    private String planActual;

    @NotBlank(message = "Plan nuevo es obligatorio")
    private String planNuevo;

    @NotBlank(message = "Código llamada es obligatorio")
    private String codigoLlamada;

    @NotBlank(message = "Producto es obligatorio")
    private String producto;

    @NotNull(message = "Monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monto debe ser mayor a 0")
    private BigDecimal monto;

    private Long clienteId;

    private Long planId;

    private Long distritoId;

    @Email(message = "Email inválido")
    private String email;
}
