# language: es
Característica: US20 Ciclo completo del viaje contra Fleet real
  Como administrador y conductor de una pyme de transporte
  Quiero que el viaje se asigne, inicie y finalice usando los vehículos y conductores registrados en Fleet
  Para comprobar que Trip y Fleet trabajan juntos sin respuestas simuladas

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que el administrador registró el vehículo "camión" con placa "ABC-123", marca "Volvo", modelo "FH", año 2022 y capacidad "5000"
    Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "10203040", licencia "Q10203040" categoría "A-IIIc" y vencimiento "2028-12-31" vinculado al usuario "luis@andes.pe"
    Y que la asignación consulta a Fleet sin usar el doble de prueba

  Escenario: El conductor inicia y finaliza el viaje asignado con los ids de Fleet
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Cuando el administrador asigna el viaje "viaje" con el vehículo de Fleet "camión" y el conductor de Fleet "Luis Quispe"
    Entonces la respuesta tiene código 200
    Y el viaje queda en estado "ASSIGNED"
    Dado que el conductor "luis@andes.pe" inició sesión
    Cuando el conductor "luis@andes.pe" inicia el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y el viaje queda en estado "IN_ROUTE"
    Cuando el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y el viaje queda en estado "FINISHED"

  Escenario: El conductor finaliza el viaje en ruta aunque la suscripción de la empresa esté restringida
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" con el vehículo de Fleet "camión" y el conductor de Fleet "Luis Quispe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor "luis@andes.pe" inicia el viaje "viaje"
    Y que la suscripción de la empresa con RUC "20123456789" está restringida
    Cuando el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y el viaje queda en estado "FINISHED"

  Escenario: El conductor ve el viaje asignado en su lista
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" con el vehículo de Fleet "camión" y el conductor de Fleet "Luis Quispe"
    Cuando el conductor "luis@andes.pe" consulta sus viajes asignados
    Entonces la respuesta tiene código 200
    Y la lista contiene 1 viajes
    Y la lista incluye el viaje "viaje"

  Escenario: Otro conductor registrado en Fleet recibe 404 al iniciar el viaje
    Dado que el administrador invitó a "Marta Ríos" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que el administrador registró el conductor "Marta Ríos" con documento "DNI" "30405060", licencia "Q30405060" categoría "A-IIIb" y vencimiento "2028-12-31" vinculado al usuario "marta@andes.pe"
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" con el vehículo de Fleet "camión" y el conductor de Fleet "Luis Quispe"
    Cuando el conductor "marta@andes.pe" intenta iniciar el viaje "viaje"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Un conductor sin registro en Fleet recibe 404 al iniciar el viaje
    Dado que el administrador invitó a "Pablo Mendoza" con el correo "pablo@andes.pe" y el rol "DRIVER"
    Y que "pablo@andes.pe" definió la contraseña "Clave-De-Pablo-2026" con su enlace de invitación
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" con el vehículo de Fleet "camión" y el conductor de Fleet "Luis Quispe"
    Cuando el conductor "pablo@andes.pe" intenta iniciar el viaje "viaje"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."
