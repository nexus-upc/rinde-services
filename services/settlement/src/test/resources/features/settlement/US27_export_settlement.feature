# language: es
Característica: US27 Exportar la liquidación como CSV
  Como administrador u operador de la empresa
  Quiero descargar la liquidación en formato CSV
  Para archivarla o compartirla con contabilidad

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Y que existe la liquidación "liq" del viaje "viaje"

  Escenario: Exportar liquidación abierta
    Cuando el administrador exporta la liquidación "liq"
    Entonces la respuesta tiene código 200
    Y la respuesta CSV contiene "settlementId"
    Y la respuesta CSV contiene "status,OPEN"

  Escenario: Exportar liquidación inexistente devuelve 404
    Cuando el administrador exporta una liquidación con id inexistente
    Entonces la respuesta tiene código 404

  Escenario: Exportar requiere autenticación
    Cuando una persona sin token exporta la liquidación "liq"
    Entonces la respuesta tiene código 401
