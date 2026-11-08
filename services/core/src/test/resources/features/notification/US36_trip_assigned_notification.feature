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

  Escenario: Recibir TripAssigned y preparar el aviso para el conductor
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Entonces el viaje queda asignado al conductor "luis@andes.pe"
    Y Notifications prepara un aviso pendiente para el conductor "luis@andes.pe" con el código del viaje, el destino "Arequipa" y la fecha de salida "HOY+5"

  # El evento actual no contiene token de dispositivo y el proyecto no configura un proveedor push.
  # Este escenario verifica recepción y preparación del aviso; no acredita entrega a un teléfono.
