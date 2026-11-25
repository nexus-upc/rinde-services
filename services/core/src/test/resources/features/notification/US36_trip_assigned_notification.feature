# language: es
Característica: US36 Notificar al conductor la asignación de un viaje
  Como conductor
  Quiero recibir un aviso cuando me asignen un viaje
  Para enterarme oportunamente sin revisar la aplicación de forma permanente

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"

  Escenario: Registrar dispositivo y enviar aviso push al asignar un viaje
    Dado que el conductor "luis@andes.pe" registró su dispositivo "fcm-token-luis-01" en la plataforma "ANDROID"
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications entrega el aviso push al token "fcm-token-luis-01" del conductor "luis@andes.pe" con el código del viaje, el destino "Arequipa" y la fecha de salida "HOY+5"

  Escenario: Ignorar evento repetido sin duplicar el envío push
    Dado que el conductor "luis@andes.pe" registró su dispositivo "fcm-token-luis-02" en la plataforma "ANDROID"
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Trujillo" con carga "Alimentos" y salida "HOY+3"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y llega nuevamente el mismo evento de asignación del viaje
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications registra un único envío push para el evento

  Escenario: Conservar la asignación y programar reintento cuando falla el proveedor push
    Dado que el conductor "luis@andes.pe" registró su dispositivo "fcm-token-luis-03" en la plataforma "ANDROID"
    Y que el proveedor push falla temporalmente
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Cusco" con carga "Equipos" y salida "HOY+4"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications prepara un aviso pendiente para el conductor "luis@andes.pe" con el código del viaje, el destino "Cusco" y la fecha de salida "HOY+4"

  Escenario: Omitir sin reintentos cuando el conductor no tiene dispositivos registrados
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Piura" con carga "Insumos" y salida "HOY+2"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications omite el aviso sin programar reintentos

  Escenario: Dar de baja un dispositivo de forma idempotente antes de asignar viaje
    Dado que el conductor "luis@andes.pe" registró su dispositivo "fcm-token-luis-05" en la plataforma "ANDROID"
    Cuando el conductor "luis@andes.pe" elimina su dispositivo "fcm-token-luis-05"
    Entonces la respuesta tiene código 204
    Cuando el conductor "luis@andes.pe" elimina su dispositivo "fcm-token-luis-05"
    Entonces la respuesta tiene código 204
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Ica" con carga "Envases" y salida "HOY+6"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications omite el aviso sin programar reintentos
