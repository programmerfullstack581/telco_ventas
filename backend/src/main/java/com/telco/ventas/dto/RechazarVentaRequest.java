package com.telco.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechazarVentaRequest {

    @NotBlank(message = "Motivo de rechazo es obligatorio")
    @Size(min = 5, message = "Motivo debe tener al menos 5 caracteres")
    private String motivoRechazo;
}
